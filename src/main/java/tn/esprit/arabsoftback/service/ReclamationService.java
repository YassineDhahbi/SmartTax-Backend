package tn.esprit.arabsoftback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.dto.MessageDto;
import tn.esprit.arabsoftback.dto.ReclamationAgentStatsDto;
import tn.esprit.arabsoftback.dto.ReclamationDto;
import tn.esprit.arabsoftback.entity.Message;
import tn.esprit.arabsoftback.entity.Role;
import tn.esprit.arabsoftback.entity.Reclamation;
import tn.esprit.arabsoftback.exception.ReclamationException;
import tn.esprit.arabsoftback.mapper.ReclamationMapper;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.repository.IUtilisateurRepository;
import tn.esprit.arabsoftback.repository.MessageRepository;
import tn.esprit.arabsoftback.repository.ReclamationRepository;

import jakarta.persistence.criteria.Predicate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReclamationService {

    private static final Set<String> RECLAMATION_LIST_SORT_FIELDS = Set.of(
            "dateCreation",
            "dateSoumission",
            "sujet",
            "nomUser",
            "emailUser",
            "reference",
            "categorie",
            "type",
            "urgence",
            "statut",
            "etatReclamation"
    );

    private final ReclamationRepository reclamationRepository;
    private final MessageRepository messageRepository;
    private final IUtilisateurRepository utilisateurRepository;
    private final NotificationProducerService notificationProducerService;
    private final ReclamationMapper reclamationMapper;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Configuration pour les fichiers
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "application/pdf", 
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final String UPLOAD_DIR = "uploads/reclamations/";
    
    /**
     * Crée une nouvelle réclamation
     */
    public ReclamationDto.CreateReclamationResponse createReclamation(
            ReclamationDto.CreateReclamationDto dto, 
            String emailUser,
            MultipartFile[] files) {
        return createReclamation(dto, emailUser, files, null, null);
    }

    public ReclamationDto.CreateReclamationResponse createReclamation(
            ReclamationDto.CreateReclamationDto dto,
            String authenticatedEmail,
            MultipartFile[] files,
            String statutValue,
            String emailOverride) {
        
        // Générer une référence unique
        String reference = generateReference();
        
        // Créer l'entité
        Reclamation reclamation = reclamationMapper.toEntity(dto);
        reclamation.setReference(reference);
        String normalizedOverrideEmail = normalizeText(emailOverride);
        String normalizedAuthenticatedEmail = normalizeText(authenticatedEmail);
        String finalEmail = normalizedOverrideEmail != null ? normalizedOverrideEmail : normalizedAuthenticatedEmail;
        if (finalEmail == null) {
            finalEmail = "test@example.com";
            log.warn("Email utilisateur indisponible pendant création réclamation, fallback utilisé.");
        }
        reclamation.setEmailUser(finalEmail);
        reclamation.setNomUser(normalizeText(reclamation.getNomUser()));
        reclamation.setTelephoneUser(normalizeText(reclamation.getTelephoneUser()));
        enrichContribuableFromAccountIfMissing(reclamation);

        // Si la soumission est demandée, enregistrer la date de soumission.
        if (statutValue != null && "SOUMIS".equalsIgnoreCase(statutValue)) {
            reclamation.setStatut(Reclamation.StatutReclamation.SOUMIS);
            reclamation.setDateSoumission(LocalDateTime.now());
            reclamation.setEtatReclamation(Reclamation.EtatReclamation.EN_COURS);
        }
        
        // Traiter les fichiers
        if (files != null && files.length > 0) {
            List<ReclamationDto.PieceJointeDto> piecesJointes = processFiles(files, reference);
            try {
                reclamation.setPiecesJointes(objectMapper.writeValueAsString(piecesJointes));
            } catch (JsonProcessingException e) {
                log.error("Erreur lors de la sérialisation des pièces jointes", e);
            }
        }
        
        // Sauvegarder
        Reclamation saved = reclamationRepository.save(reclamation);
        log.info("Nouvelle réclamation créée: {} par {}", reference, finalEmail);
        if (saved.getStatut() == Reclamation.StatutReclamation.SOUMIS) {
            sendReclamationSoumiseKafkaNotification(saved);
        }
        
        return ReclamationDto.CreateReclamationResponse.builder()
                .id(saved.getId())
                .reference(saved.getReference())
                .message("Réclamation créée avec succès")
                .statut(reclamationMapper.mapStatutToDto(saved.getStatut()))
                .build();
    }

    private String normalizeText(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim();
        if (normalized.isEmpty()
                || "undefined".equalsIgnoreCase(normalized)
                || "null".equalsIgnoreCase(normalized)
                || "anonymoususer".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    /**
     * Complète nom (prénom + nom) et téléphone depuis le compte {@link Utilisateur} si manquants.
     *
     * @return true si une valeur a été renseignée (pour persister au besoin)
     */
    private boolean enrichContribuableFromAccountIfMissing(Reclamation reclamation) {
        String email = normalizeText(reclamation.getEmailUser());
        if (email == null) {
            return false;
        }
        boolean nomMissing = normalizeText(reclamation.getNomUser()) == null;
        boolean telMissing = normalizeText(reclamation.getTelephoneUser()) == null;
        if (!nomMissing && !telMissing) {
            return false;
        }
        Optional<Utilisateur> userOpt = utilisateurRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            userOpt = utilisateurRepository.findByEmail(email);
        }
        if (userOpt.isEmpty()) {
            return false;
        }
        Utilisateur u = userOpt.get();
        boolean changed = false;
        if (nomMissing) {
            String fullName = buildContributorFullName(u.getFirstName(), u.getLastName());
            if (fullName != null) {
                reclamation.setNomUser(fullName);
                changed = true;
            }
        }
        if (telMissing) {
            String tel = normalizeText(u.getTelephone());
            if (tel != null) {
                reclamation.setTelephoneUser(tel);
                changed = true;
            }
        }
        return changed;
    }

    private String buildContributorFullName(String firstName, String lastName) {
        String f = normalizeText(firstName);
        String l = normalizeText(lastName);
        if (f == null && l == null) {
            return null;
        }
        if (f == null) {
            return l;
        }
        if (l == null) {
            return f;
        }
        return f + " " + l;
    }
    
    /**
     * Soumet un brouillon
     */
    public ReclamationDto submitDraft(Long id) {
        Reclamation reclamation = getReclamationEntityById(id);
        
        if (reclamation.getStatut() != Reclamation.StatutReclamation.BROUILLON) {
            throw new ReclamationException.DraftOperationException("soumettre");
        }
        
        reclamation.setStatut(Reclamation.StatutReclamation.SOUMIS);
        reclamation.setDateSoumission(LocalDateTime.now());
        reclamation.setEtatReclamation(Reclamation.EtatReclamation.EN_COURS);
        enrichContribuableFromAccountIfMissing(reclamation);
        
        Reclamation saved = reclamationRepository.save(reclamation);
        log.info("Brouillon soumis: {}", reclamation.getReference());
        sendReclamationSoumiseKafkaNotification(saved);
        
        return reclamationMapper.toDto(saved);
    }

    /**
     * Notifie agents et admins (Kafka → consumer persiste en base) qu'une réclamation vient d'être soumise.
     */
    private void sendReclamationSoumiseKafkaNotification(Reclamation reclamation) {
        if (reclamation == null || reclamation.getId() == null) {
            return;
        }
        List<Integer> recipientIds = new ArrayList<>();
        recipientIds.addAll(utilisateurRepository.findByRole(Role.AGENT).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList());
        recipientIds.addAll(utilisateurRepository.findByRole(Role.ADMIN).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList());
        List<Integer> uniqueRecipientIds = recipientIds.stream().distinct().toList();
        if (uniqueRecipientIds.isEmpty()) {
            log.debug("Aucun agent/admin cible pour notification réclamation Kafka");
            return;
        }
        String sujet = reclamation.getSujet() == null || reclamation.getSujet().isBlank()
                ? "Sans sujet"
                : reclamation.getSujet();
        String nom = reclamation.getNomUser() != null && !reclamation.getNomUser().isBlank()
                ? reclamation.getNomUser()
                : reclamation.getEmailUser();
        String message = String.format(
                "Nouvelle réclamation soumise par %s : %s — Réf. %s",
                nom != null ? nom : "Contribuable",
                sujet,
                reclamation.getReference());
        notificationProducerService.sendWithReclamation(
                "RECLAMATION_SOUMISE",
                "Nouvelle réclamation",
                message,
                reclamation.getId(),
                uniqueRecipientIds);
    }

    /**
     * Notifie la/les partie(s) destinataire(s) qu'un nouveau message de réclamation est arrivé.
     * - message contribuable -> agents + admins
     * - message agent -> contribuable
     */
    private void sendReclamationMessageKafkaNotification(Reclamation reclamation, Message.AuteurMessage auteur) {
        if (reclamation == null || reclamation.getId() == null || auteur == null) {
            return;
        }
        List<Integer> recipients = new ArrayList<>();
        String title;
        String eventType;

        if (auteur == Message.AuteurMessage.CONTRIBUTABLE) {
            recipients.addAll(utilisateurRepository.findByRole(Role.AGENT).stream()
                    .map(Utilisateur::getIdUtilisateur)
                    .toList());
            recipients.addAll(utilisateurRepository.findByRole(Role.ADMIN).stream()
                    .map(Utilisateur::getIdUtilisateur)
                    .toList());
            eventType = "RECLAMATION_MESSAGE_CONTRIBUABLE";
            title = "Nouveau message contribuable";
        } else {
            String email = normalizeText(reclamation.getEmailUser());
            if (email != null) {
                utilisateurRepository.findByEmailIgnoreCase(email)
                        .map(Utilisateur::getIdUtilisateur)
                        .ifPresent(recipients::add);
            }
            eventType = "RECLAMATION_MESSAGE_AGENT";
            title = "Nouveau message agent";
        }

        List<Integer> uniqueRecipients = recipients.stream().distinct().toList();
        if (uniqueRecipients.isEmpty()) {
            log.debug("Aucun destinataire pour notification message réclamation {}", reclamation.getId());
            return;
        }

        String sujet = normalizeText(reclamation.getSujet());
        String message = String.format(
                "Nouveau message sur la réclamation %s%s",
                reclamation.getReference(),
                sujet != null ? " (" + sujet + ")" : "");

        notificationProducerService.sendWithReclamation(
                eventType,
                title,
                message,
                reclamation.getId(),
                uniqueRecipients
        );
    }
    
    /**
     * Récupère toutes les réclamations d'un utilisateur
     */
    public List<ReclamationDto> getReclamationsByUser(String emailUser) {
        List<Reclamation> reclamations = reclamationRepository.findByEmailUserOrderByDateCreationDesc(emailUser);
        for (Reclamation r : reclamations) {
            if (enrichContribuableFromAccountIfMissing(r)) {
                reclamationRepository.save(r);
            }
        }
        List<ReclamationDto> list = reclamationMapper.toDtoList(reclamations);
        for (ReclamationDto dto : list) {
            enrichUnreadAgentMessageCountForContribuable(dto);
        }
        return list;
    }

    private void enrichUnreadAgentMessageCountForContribuable(ReclamationDto dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }
        if (isCurrentUserStaff()) {
            dto.setUnreadAgentMessageCount(null);
            return;
        }
        long c = messageRepository.countUnreadMessages(dto.getId(), Message.AuteurMessage.AGENT);
        dto.setUnreadAgentMessageCount(c);
    }

    private Sort buildReclamationListSort(String sortField, String direction) {
        Sort.Direction dir = direction != null && "ASC".equalsIgnoreCase(direction.trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        if (sortField == null || sortField.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "dateCreation");
        }
        String f = sortField.trim();
        if (!RECLAMATION_LIST_SORT_FIELDS.contains(f)) {
            return Sort.by(Sort.Direction.DESC, "dateCreation");
        }
        return Sort.by(dir, f);
    }

    private Specification<Reclamation> buildReclamationListSpec(
            Reclamation.StatutReclamation statut,
            Reclamation.EtatReclamation etat,
            Reclamation.NiveauUrgence urgence,
            String search) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (statut != null) {
                parts.add(cb.equal(root.get("statut"), statut));
            }
            if (etat != null) {
                parts.add(cb.equal(root.get("etatReclamation"), etat));
            }
            if (urgence != null) {
                parts.add(cb.equal(root.get("urgence"), urgence));
            }
            String q = search != null ? search.trim() : "";
            if (!q.isEmpty()) {
                String pattern = "%" + q.toLowerCase(Locale.ROOT) + "%";
                parts.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("reference"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("sujet"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("categorie"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("emailUser"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("nomUser"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("referenceUser"), cb.literal(""))), pattern)
                ));
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }
    
    /**
     * Récupère toutes les réclamations (pour admin / agent).
     *
     * @param statut si non null, seules les réclamations avec ce statut sont retournées (ex. {@link Reclamation.StatutReclamation#SOUMIS} pour l'agent DGI).
     * @param search filtre texte optionnel (référence, sujet, email, etc.).
     * @param sort nom d'attribut JPA autorisé (ex. dateCreation, sujet) ; ignoré si inconnu.
     * @param direction ASC ou DESC ; autre valeur traitée comme DESC.
     * @param etat filtre optionnel sur l'état de traitement ({@link Reclamation.EtatReclamation}).
     * @param urgence filtre optionnel sur le niveau d'urgence.
     */
    public Page<ReclamationDto> getAllReclamations(
            int page,
            int size,
            Reclamation.StatutReclamation statut,
            String search,
            String sort,
            String direction,
            Reclamation.EtatReclamation etat,
            Reclamation.NiveauUrgence urgence) {
        Pageable pageable = PageRequest.of(page, size, buildReclamationListSort(sort, direction));
        Specification<Reclamation> spec = buildReclamationListSpec(statut, etat, urgence, search);
        Page<Reclamation> reclamations = reclamationRepository.findAll(spec, pageable);
        for (Reclamation r : reclamations.getContent()) {
            if (enrichContribuableFromAccountIfMissing(r)) {
                reclamationRepository.save(r);
            }
        }
        return reclamations.map(reclamationMapper::toDto);
    }

    /**
     * Compteurs agrégés pour le tableau de bord agent (par statut, ex. SOUMIS).
     */
    public ReclamationAgentStatsDto getAgentReclamationStats(Reclamation.StatutReclamation statut) {
        long total = reclamationRepository.countByStatut(statut);
        long enCours = reclamationRepository.countByStatutAndEtatReclamation(statut, Reclamation.EtatReclamation.EN_COURS);
        long traite = reclamationRepository.countByStatutAndEtatReclamation(statut, Reclamation.EtatReclamation.TRAITE);
        long prioriteHaute = reclamationRepository.countByStatutAndUrgenceIn(statut,
                List.of(Reclamation.NiveauUrgence.HAUTE, Reclamation.NiveauUrgence.URGENTE));
        return new ReclamationAgentStatsDto(total, enCours, traite, prioriteHaute);
    }
    
    /**
     * Récupère une réclamation par son ID
     */
    public ReclamationDto getReclamationDtoById(Long id) {
        Reclamation reclamation = getReclamationEntityById(id);
        if (enrichContribuableFromAccountIfMissing(reclamation)) {
            reclamationRepository.save(reclamation);
        }
        ReclamationDto dto = reclamationMapper.toDto(reclamation);
        enrichUnreadAgentMessageCountForContribuable(dto);
        return dto;
    }
    
    /**
     * Récupère une réclamation par sa référence
     */
    public ReclamationDto getReclamationByReference(String reference) {
        Reclamation reclamation = reclamationRepository.findByReference(reference)
                .orElseThrow(() -> new ReclamationException.ReclamationNotFoundException(reference));
        if (enrichContribuableFromAccountIfMissing(reclamation)) {
            reclamationRepository.save(reclamation);
        }
        return reclamationMapper.toDto(reclamation);
    }
    
    /**
     * Met à jour une réclamation
     */
    public ReclamationDto updateReclamation(Long id, ReclamationDto.UpdateReclamationDto dto, MultipartFile[] files) {
        Reclamation reclamation = getReclamationEntityById(id);
        
        // Vérifier si la mise à jour est autorisée
        if (reclamation.getStatut() == Reclamation.StatutReclamation.RESOLU) {
            throw new ReclamationException.ReclamationAlreadyResolvedException(reclamation.getReference());
        }
        
        // Mettre à jour les champs
        if (dto.getType() != null) {
            reclamation.setType(reclamationMapper.mapTypeToEntity(dto.getType()));
        }
        if (dto.getCategorie() != null) {
            reclamation.setCategorie(dto.getCategorie());
        }
        if (dto.getSujet() != null) {
            reclamation.setSujet(dto.getSujet());
        }
        if (dto.getDescription() != null) {
            reclamation.setDescription(dto.getDescription());
        }
        if (dto.getUrgence() != null) {
            reclamation.setUrgence(reclamationMapper.mapUrgenceToEntity(dto.getUrgence()));
        }
        if (dto.getReferenceUser() != null) {
            reclamation.setReferenceUser(dto.getReferenceUser());
        }
        if (dto.getMotifResolution() != null) {
            reclamation.setMotifResolution(dto.getMotifResolution());
        }
        if (dto.getNomUser() != null) {
            reclamation.setNomUser(normalizeText(dto.getNomUser()));
        }
        if (dto.getTelephoneUser() != null) {
            reclamation.setTelephoneUser(normalizeText(dto.getTelephoneUser()));
        }
        enrichContribuableFromAccountIfMissing(reclamation);
        
        // Traiter les nouveaux fichiers
        if (files != null && files.length > 0) {
            List<ReclamationDto.PieceJointeDto> piecesJointes = processFiles(files, reclamation.getReference());
            
            // Ajouter aux pièces jointes existantes
            List<ReclamationDto.PieceJointeDto> existingPieces = getPiecesJointes(reclamation);
            existingPieces.addAll(piecesJointes);
            
            try {
                reclamation.setPiecesJointes(objectMapper.writeValueAsString(existingPieces));
            } catch (JsonProcessingException e) {
                log.error("Erreur lors de la sérialisation des pièces jointes", e);
            }
        }
        
        Reclamation saved = reclamationRepository.save(reclamation);
        log.info("Réclamation mise à jour: {}", reclamation.getReference());
        
        return reclamationMapper.toDto(saved);
    }
    
    /**
     * Supprime une réclamation
     */
    public void deleteReclamation(Long id) {
        Reclamation reclamation = getReclamationEntityById(id);
        
        // Brouillon: suppression autorisée.
        // Exception métier: un ADMIN peut aussi supprimer une réclamation SOUMIS.
        boolean canDelete =
                reclamation.getStatut() == Reclamation.StatutReclamation.BROUILLON
                        || (reclamation.getStatut() == Reclamation.StatutReclamation.SOUMIS && isCurrentUserAdmin());
        if (!canDelete) {
            throw new ReclamationException.DraftOperationException("supprimer");
        }
        
        // Supprimer les fichiers associés
        deleteReclamationFiles(reclamation.getReference());
        
        // Supprimer les messages
        messageRepository.deleteByReclamationId(id);
        
        // Supprimer la réclamation
        reclamationRepository.delete(reclamation);
        log.info("Réclamation supprimée: {}", reclamation.getReference());
    }
    
    /**
     * Change le statut d'une réclamation
     */
    public ReclamationDto changeStatut(Long id, Reclamation.StatutReclamation newStatut, String motif) {
        Reclamation reclamation = getReclamationEntityById(id);
        Reclamation.StatutReclamation previousStatut = reclamation.getStatut();
        
        // Valider la transition de statut
        validateStatutTransition(previousStatut, newStatut);
        
        reclamation.setStatut(newStatut);
        
        if (newStatut == Reclamation.StatutReclamation.RESOLU) {
            reclamation.setDateResolution(LocalDateTime.now());
            reclamation.setMotifResolution(motif);
            reclamation.setEtatReclamation(Reclamation.EtatReclamation.TRAITE);
        } else if (newStatut == Reclamation.StatutReclamation.SOUMIS
                || newStatut == Reclamation.StatutReclamation.EN_COURS) {
            reclamation.setEtatReclamation(Reclamation.EtatReclamation.EN_COURS);
        }
        
        Reclamation saved = reclamationRepository.save(reclamation);
        log.info("Statut changé pour {}: {} -> {}", reclamation.getReference(),
                previousStatut, newStatut);
        if (newStatut == Reclamation.StatutReclamation.SOUMIS
                && previousStatut == Reclamation.StatutReclamation.BROUILLON) {
            sendReclamationSoumiseKafkaNotification(saved);
        }
        
        return reclamationMapper.toDto(saved);
    }

    /**
     * Met à jour uniquement l'état de traitement (En cours / Traité) pour l'agent.
     */
    public ReclamationDto changeEtatReclamation(Long id, Reclamation.EtatReclamation etat) {
        Reclamation reclamation = getReclamationEntityById(id);
        if (etat != Reclamation.EtatReclamation.EN_COURS && etat != Reclamation.EtatReclamation.TRAITE) {
            throw new ReclamationException.InvalidReclamationDataException("État invalide: " + etat);
        }
        reclamation.setEtatReclamation(etat);
        Reclamation saved = reclamationRepository.save(reclamation);
        log.info("État de traitement mis à jour pour {}: {}", reclamation.getReference(), etat);
        return reclamationMapper.toDto(saved);
    }
    
    /**
     * Envoie un message
     */
    public MessageDto sendMessage(Long reclamationId, MessageDto.CreateMessageDto createDto, MultipartFile file) {
        Reclamation reclamation = getReclamationEntityById(reclamationId);
        Message.AuteurMessage auteurEnum = Message.AuteurMessage.fromValue(createDto.getAuteur());

        assertMessagerieReadable(reclamation);

        if (auteurEnum == Message.AuteurMessage.CONTRIBUTABLE) {
            if (reclamation.getStatut() == Reclamation.StatutReclamation.RESOLU) {
                throw new ReclamationException.ReclamationAlreadyResolvedException(reclamation.getReference());
            }
            long agentMessages = messageRepository.countByReclamation_IdAndAuteur(
                    reclamationId, Message.AuteurMessage.AGENT);
            if (agentMessages == 0) {
                throw new ReclamationException.AgentMustInitiateChatException();
            }
        }

        // Créer le message
        Message message = Message.builder()
                .reclamation(reclamation)
                .contenu(createDto.getContenu())
                .auteur(auteurEnum)
                .lu(false)
                .build();
        
        // Traiter la pièce jointe si présente
        if (file != null && !file.isEmpty()) {
            validateFile(file);
            String fileName = saveFile(file, reclamation.getReference() + "/messages");
            
            MessageDto.PieceJointeDto pieceJointe = MessageDto.PieceJointeDto.builder()
                    .nom(file.getOriginalFilename())
                    .taille(file.getSize())
                    .type(file.getContentType())
                    .url("/api/reclamation/files/" + reclamation.getReference() + "/" + fileName)
                    .build();
            
            try {
                message.setPieceJointe(objectMapper.writeValueAsString(pieceJointe));
            } catch (JsonProcessingException e) {
                log.error("Erreur lors de la sérialisation de la pièce jointe du message", e);
            }
        }
        
        Message saved = messageRepository.save(message);
        log.info("Message envoyé pour la réclamation: {}", reclamation.getReference());
        sendReclamationMessageKafkaNotification(reclamation, auteurEnum);

        MessageDto messageDto = reclamationMapper.toMessageDto(saved);
        broadcastReclamationMessage(reclamation, messageDto);
        return messageDto;
    }
    
    /**
     * Récupère les messages d'une réclamation
     */
    @Transactional
    public List<MessageDto> getMessages(Long reclamationId) {
        Reclamation reclamation = getReclamationEntityById(reclamationId);
        assertMessagerieReadable(reclamation);

        List<Message> messages = messageRepository.findByReclamationIdOrderByDateEnvoiAsc(reclamationId);

        // Côté contribuable : marquer les messages de l'agent comme lus. Pas lorsque l'agent consulte le fil.
        if (!isCurrentUserStaff()) {
            messageRepository.markMessagesAsRead(
                    reclamationId,
                    Message.AuteurMessage.AGENT,
                    LocalDateTime.now()
            );
        }

        return reclamationMapper.toMessageDtoList(messages);
    }
    
    /**
     * Recherche des réclamations
     */
    @Transactional(readOnly = true)
    public List<ReclamationDto> searchReclamations(String query, String emailUser) {
        List<Reclamation> reclamations;
        
        if (emailUser != null && !emailUser.isEmpty()) {
            reclamations = reclamationRepository.searchReclamationsByUser(emailUser, query);
        } else {
            reclamations = reclamationRepository.searchReclamations(query);
        }
        
        return reclamationMapper.toDtoList(reclamations);
    }
    
    /**
     * Récupère les statistiques
     */
    @Transactional(readOnly = true)
    public ReclamationDto.ReclamationStatistics getStatistics(String emailUser) {
        if (emailUser != null && !emailUser.isEmpty()) {
            // Statistiques pour un utilisateur spécifique
            return ReclamationDto.ReclamationStatistics.builder()
                    .total(reclamationRepository.countByEmailUser(emailUser))
                    .brouillons(reclamationRepository.countByStatutAndEmailUser(
                            Reclamation.StatutReclamation.BROUILLON, emailUser))
                    .soumis(reclamationRepository.countByStatutAndEmailUser(
                            Reclamation.StatutReclamation.SOUMIS, emailUser))
                    .enCours(reclamationRepository.countByStatutAndEmailUser(
                            Reclamation.StatutReclamation.EN_COURS, emailUser))
                    .resolus(reclamationRepository.countByStatutAndEmailUser(
                            Reclamation.StatutReclamation.RESOLU, emailUser))
                    .rejetes(reclamationRepository.countByStatutAndEmailUser(
                            Reclamation.StatutReclamation.REJETE, emailUser))
                    .nonLus(messageRepository.countUnreadAgentMessagesByUser(emailUser))
                    .build();
        } else {
            // Statistiques globales (pour admin)
            return ReclamationDto.ReclamationStatistics.builder()
                    .total(reclamationRepository.count())
                    .brouillons(reclamationRepository.countByStatut(Reclamation.StatutReclamation.BROUILLON))
                    .soumis(reclamationRepository.countByStatut(Reclamation.StatutReclamation.SOUMIS))
                    .enCours(reclamationRepository.countByStatut(Reclamation.StatutReclamation.EN_COURS))
                    .resolus(reclamationRepository.countByStatut(Reclamation.StatutReclamation.RESOLU))
                    .rejetes(reclamationRepository.countByStatut(Reclamation.StatutReclamation.REJETE))
                    .nonLus(0) // À implémenter selon les besoins
                    .build();
        }
    }
    
    // Méthodes utilitaires privées
    private Reclamation getReclamationEntityById(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new ReclamationException.ReclamationNotFoundException(id));
    }
    
    private String generateReference() {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "REC-" + date + "-" + random;
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ReclamationException.InvalidFileException("Le fichier est vide");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ReclamationException.FileSizeExceededException(MAX_FILE_SIZE);
        }
        
        if (!ALLOWED_FILE_TYPES.contains(file.getContentType())) {
            throw new ReclamationException.UnsupportedFileTypeException(file.getContentType());
        }
    }
    
    private String saveFile(MultipartFile file, String subDir) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR + subDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            
            return fileName;
        } catch (IOException e) {
            throw new ReclamationException.InvalidFileException("Erreur lors de la sauvegarde du fichier: " + e.getMessage());
        }
    }
    
    private List<ReclamationDto.PieceJointeDto> processFiles(MultipartFile[] files, String reference) {
        return Arrays.stream(files)
                .map(file -> {
                    validateFile(file);
                    String fileName = saveFile(file, reference);
                    
                    return ReclamationDto.PieceJointeDto.builder()
                            .nom(file.getOriginalFilename())
                            .taille(file.getSize())
                            .type(file.getContentType())
                            .url("/api/reclamation/files/" + reference + "/" + fileName)
                            .build();
                })
                .toList();
    }
    
    private List<ReclamationDto.PieceJointeDto> getPiecesJointes(Reclamation reclamation) {
        if (reclamation.getPiecesJointes() == null || reclamation.getPiecesJointes().isEmpty()) {
            return List.of();
        }
        
        try {
            return objectMapper.readValue(reclamation.getPiecesJointes(), 
                    new com.fasterxml.jackson.core.type.TypeReference<List<ReclamationDto.PieceJointeDto>>() {});
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la désérialisation des pièces jointes", e);
            return List.of();
        }
    }
    
    private void deleteReclamationFiles(String reference) {
        try {
            Path reclamationDir = Paths.get(UPLOAD_DIR + reference);
            if (Files.exists(reclamationDir)) {
                Files.walk(reclamationDir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Impossible de supprimer le fichier: " + path, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Impossible de supprimer le répertoire des fichiers pour: " + reference, e);
        }
    }
    
    private void broadcastReclamationMessage(Reclamation reclamation, MessageDto dto) {
        Long reclamationId = reclamation.getId();
        try {
            messagingTemplate.convertAndSend("/topic/reclamation/" + reclamationId + "/messages", dto);
        } catch (Exception e) {
            log.warn("Diffusion WebSocket messagerie échouée pour réclamation {}: {}", reclamationId, e.getMessage());
        }
        MessageDto.AuteurMessageDto auteur = dto.getAuteur();
        if (auteur != null && auteur.getValue() != null
                && "agent".equalsIgnoreCase(auteur.getValue().trim())) {
            String email = reclamation.getEmailUser();
            if (email != null && !email.isBlank()) {
                try {
                    String suffix = reclamationInboxTopicSuffix(email);
                    messagingTemplate.convertAndSend(
                            "/topic/reclamation/inbox/" + suffix,
                            Map.of(
                                    "type", "NEW_AGENT_MESSAGE",
                                    "reclamationId", reclamationId
                            ));
                } catch (Exception e) {
                    log.warn("Diffusion inbox contribuable échouée pour réclamation {}: {}", reclamationId, e.getMessage());
                }
            }
        }
    }

    /** Suffixe de topic STOMP (email normalisé, Base64 URL sans padding) pour éviter les caractères spéciaux. */
    static String reclamationInboxTopicSuffix(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isCurrentUserStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            String role = a.getAuthority();
            if ("ROLE_AGENT".equals(role) || "ROLE_ADMIN".equals(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** Messagerie (lecture / échange) : hors brouillon et hors rejet. */
    private void assertMessagerieReadable(Reclamation reclamation) {
        Reclamation.StatutReclamation s = reclamation.getStatut();
        if (s == Reclamation.StatutReclamation.BROUILLON || s == Reclamation.StatutReclamation.REJETE) {
            throw new ReclamationException.MessagerieUnavailableException();
        }
    }

    private void validateStatutTransition(Reclamation.StatutReclamation current, Reclamation.StatutReclamation target) {
        // Logique de validation des transitions de statut
        switch (current) {
            case BROUILLON:
                if (target != Reclamation.StatutReclamation.SOUMIS) {
                    throw new ReclamationException.InvalidStatusException(current.name(), target.name());
                }
                break;
            case SOUMIS:
                if (target != Reclamation.StatutReclamation.EN_COURS && 
                    target != Reclamation.StatutReclamation.REJETE) {
                    throw new ReclamationException.InvalidStatusException(current.name(), target.name());
                }
                break;
            case EN_COURS:
                if (target != Reclamation.StatutReclamation.RESOLU && 
                    target != Reclamation.StatutReclamation.REJETE) {
                    throw new ReclamationException.InvalidStatusException(current.name(), target.name());
                }
                break;
            case RESOLU:
            case REJETE:
                throw new ReclamationException.InvalidStatusException(current.name(), target.name());
        }
    }
}
