package tn.esprit.arabsoftback.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arabsoftback.dto.DemandeInformationDto;
import tn.esprit.arabsoftback.dto.DemandeInformationStatsDto;
import tn.esprit.arabsoftback.entity.DemandeInformation;
import tn.esprit.arabsoftback.entity.Role;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.repository.IUtilisateurRepository;
import tn.esprit.arabsoftback.repository.DemandeInformationRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DemandeInformationService {

    private final DemandeInformationRepository demandeInformationRepository;
    private final IUtilisateurRepository utilisateurRepository;
    private final NotificationProducerService notificationProducerService;
    private static final String STATUS_TRAITE = "TRAITE";
    private static final String STATUS_NON_TRAITE = "NON_TRAITE";

    public DemandeInformationDto.CreateResponse createPublicRequest(DemandeInformationDto.CreateRequest request) {
        DemandeInformation demande = DemandeInformation.builder()
                .nomComplet(request.getNomComplet())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .sujet(request.getSujet())
                .message(request.getMessage())
                .urgent(Boolean.TRUE.equals(request.getUrgent()))
                .traitementStatus(STATUS_NON_TRAITE)
                .build();

        DemandeInformation saved = demandeInformationRepository.save(demande);
        log.info("Nouvelle demande information creee: id={}, email={}", saved.getId(), saved.getEmail());
        sendNotificationToAgents(saved);

        return DemandeInformationDto.CreateResponse.builder()
                .id(saved.getId())
                .message("Votre demande d'information a ete enregistree avec succes.")
                .dateCreation(saved.getDateCreation())
                .build();
    }

    @Transactional(readOnly = true)
    public DemandeInformationDto.ListResponse getAllDemandes() {
        List<DemandeInformationDto.ItemResponse> items = demandeInformationRepository.findAllByOrderByDateCreationDesc()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return DemandeInformationDto.ListResponse.builder()
                .total(items.size())
                .items(items)
                .page(null)
                .size(null)
                .totalPages(null)
                .build();
    }

    @Transactional(readOnly = true)
    public DemandeInformationStatsDto getStats() {
        long total = demandeInformationRepository.count();
        long traitees = demandeInformationRepository.countByTraitementStatus(STATUS_TRAITE);
        long urgentes = demandeInformationRepository.countByUrgentTrue();
        long nonTraitees = Math.max(0, total - traitees);
        return new DemandeInformationStatsDto(total, traitees, nonTraitees, urgentes);
    }

    @Transactional(readOnly = true)
    public DemandeInformationDto.ItemResponse getDemandeById(Long id) {
        DemandeInformation demande = demandeInformationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'information introuvable avec id: " + id));
        return toItemResponse(demande);
    }

    /**
     * Liste paginée avec filtres. {@code section} : {@code assigned} (mes dossiers ouverts) ou {@code main} (reste), pour {@code agentId} non null.
     */
    @Transactional(readOnly = true)
    public DemandeInformationDto.ListResponse getDemandesPaged(
            int page,
            int size,
            String search,
            String traitement,
            String urgence,
            String section,
            Integer agentId
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("dateCreation").descending());
        Specification<DemandeInformation> spec = buildSpecification(search, traitement, urgence, section, agentId);
        Page<DemandeInformation> result = demandeInformationRepository.findAll(spec, pageable);
        List<DemandeInformationDto.ItemResponse> items = result.getContent().stream()
                .map(this::toItemResponse)
                .toList();
        return DemandeInformationDto.ListResponse.builder()
                .total(result.getTotalElements())
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    private Specification<DemandeInformation> buildSpecification(
            String search,
            String traitement,
            String urgence,
            String section,
            Integer agentId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("nomComplet"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("sujet"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("message"), cb.literal(""))), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("assignedAgentName"), cb.literal(""))), pattern)
                ));
            }
            if (traitement != null && !traitement.isBlank() && !"all".equalsIgnoreCase(traitement.trim())) {
                String t = traitement.trim();
                if (STATUS_TRAITE.equalsIgnoreCase(t)) {
                    predicates.add(cb.equal(root.get("traitementStatus"), STATUS_TRAITE));
                } else if (STATUS_NON_TRAITE.equalsIgnoreCase(t)) {
                    predicates.add(cb.or(
                            cb.isNull(root.get("traitementStatus")),
                            cb.notEqual(root.get("traitementStatus"), STATUS_TRAITE)
                    ));
                }
            }
            if (urgence != null && !urgence.isBlank() && !"all".equalsIgnoreCase(urgence.trim())) {
                String u = urgence.trim().toLowerCase();
                if ("urgent".equals(u)) {
                    predicates.add(cb.isTrue(root.get("urgent")));
                } else if ("normal".equals(u)) {
                    predicates.add(cb.or(
                            cb.isFalse(root.get("urgent")),
                            cb.isNull(root.get("urgent"))
                    ));
                }
            }
            if (section != null && agentId != null) {
                String s = section.trim().toLowerCase();
                if ("assigned".equals(s)) {
                    predicates.add(cb.equal(root.get("assignedAgentId"), agentId));
                    predicates.add(cb.or(
                            cb.isNull(root.get("traitementStatus")),
                            cb.notEqual(root.get("traitementStatus"), STATUS_TRAITE)
                    ));
                } else if ("main".equals(s)) {
                    Predicate myOpen = cb.and(
                            cb.equal(root.get("assignedAgentId"), agentId),
                            cb.or(
                                    cb.isNull(root.get("traitementStatus")),
                                    cb.notEqual(root.get("traitementStatus"), STATUS_TRAITE)
                            )
                    );
                    predicates.add(cb.not(myOpen));
                }
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DemandeInformationDto.ItemResponse toItemResponse(DemandeInformation item) {
        return DemandeInformationDto.ItemResponse.builder()
                .id(item.getId())
                .nomComplet(item.getNomComplet())
                .email(item.getEmail())
                .telephone(item.getTelephone())
                .sujet(item.getSujet())
                .message(item.getMessage())
                .urgent(item.getUrgent())
                .traitementStatus(normalizeTraitementStatus(item.getTraitementStatus()))
                .assignedAgentId(item.getAssignedAgentId())
                .assignedAgentName(item.getAssignedAgentName())
                .dateCreation(item.getDateCreation())
                .build();
    }

    public void deleteDemande(Long id) {
        if (!demandeInformationRepository.existsById(id)) {
            throw new IllegalArgumentException("Demande d'information introuvable avec id: " + id);
        }
        demandeInformationRepository.deleteById(id);
        log.info("Demande d'information supprimee: id={}", id);
    }

    public DemandeInformationDto.ItemResponse updateTraitementStatus(Long id, String traitementStatus) {
        DemandeInformation demande = demandeInformationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'information introuvable avec id: " + id));

        String normalizedStatus = normalizeTraitementStatus(traitementStatus);
        demande.setTraitementStatus(normalizedStatus);
        DemandeInformation saved = demandeInformationRepository.save(demande);
        log.info("Statut traitement mis a jour: id={}, status={}", saved.getId(), saved.getTraitementStatus());

        return toItemResponse(saved);
    }

    public DemandeInformationDto.ItemResponse assignAgent(Long id, Integer agentId) {
        DemandeInformation demande = demandeInformationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'information introuvable avec id: " + id));

        if (agentId == null) {
            demande.setAssignedAgentId(null);
            demande.setAssignedAgentName(null);
        } else {
            Utilisateur agent = utilisateurRepository.findById(agentId)
                    .orElseThrow(() -> new IllegalArgumentException("Agent introuvable avec id: " + agentId));
            if (agent.getRole() != Role.AGENT) {
                throw new IllegalArgumentException("L'utilisateur s�lectionn� n'est pas un agent.");
            }
            demande.setAssignedAgentId(agent.getIdUtilisateur());
            String fullName = (agent.getFirstName() == null ? "" : agent.getFirstName()) + " " +
                    (agent.getLastName() == null ? "" : agent.getLastName());
            String displayName = fullName.trim().isEmpty() ? agent.getEmail() : fullName.trim();
            demande.setAssignedAgentName(displayName);
        }

        DemandeInformation saved = demandeInformationRepository.save(demande);
        log.info("Affectation agent mise a jour: demandeId={}, agentId={}", saved.getId(), saved.getAssignedAgentId());

        return toItemResponse(saved);
    }

    private String normalizeTraitementStatus(String traitementStatus) {
        if (STATUS_TRAITE.equalsIgnoreCase(traitementStatus)) {
            return STATUS_TRAITE;
        }
        return STATUS_NON_TRAITE;
    }

    private void sendNotificationToAgents(DemandeInformation demande) {
        List<Integer> recipientIds = new ArrayList<>();
        recipientIds.addAll(utilisateurRepository.findByRole(Role.AGENT).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList());
        recipientIds.addAll(utilisateurRepository.findByRole(Role.ADMIN).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList());

        List<Integer> uniqueRecipientIds = recipientIds.stream().distinct().toList();
        String sujet = demande.getSujet() == null || demande.getSujet().isBlank() ? "Sans sujet" : demande.getSujet();
        String message = "Nouvelle demande d'information de " + demande.getNomComplet() + " : " + sujet;
        notificationProducerService.send(
                "DEMANDE_INFORMATION_CREATED",
                "Nouvelle demande d'information",
                message,
                demande.getId(),
                uniqueRecipientIds
        );
    }
}

