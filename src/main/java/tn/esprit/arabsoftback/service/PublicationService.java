package tn.esprit.arabsoftback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arabsoftback.dto.PublicationCommentDto;
import tn.esprit.arabsoftback.dto.PublicationReportDto;
import tn.esprit.arabsoftback.entity.Publication;
import tn.esprit.arabsoftback.entity.PublicationComment;
import tn.esprit.arabsoftback.entity.PublicationCommentReport;
import tn.esprit.arabsoftback.entity.Publication.PublicationStatus;
import tn.esprit.arabsoftback.entity.PublicationReport;
import tn.esprit.arabsoftback.entity.PublicationReaction;
import tn.esprit.arabsoftback.entity.PublicationReaction.ReactionType;
import tn.esprit.arabsoftback.entity.Role;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.exception.ResourceNotFoundException;
import tn.esprit.arabsoftback.repository.PublicationCommentRepository;
import tn.esprit.arabsoftback.repository.PublicationCommentReportRepository;
import tn.esprit.arabsoftback.repository.IUtilisateurRepository;
import tn.esprit.arabsoftback.repository.PublicationReportRepository;
import tn.esprit.arabsoftback.repository.PublicationReactionRepository;
import tn.esprit.arabsoftback.repository.PublicationRepository;
import tn.esprit.arabsoftback.service.EmailService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PublicationService implements IPublicationService {
    
    private final PublicationRepository publicationRepository;
    private final PublicationReactionRepository publicationReactionRepository;
    private final PublicationCommentRepository publicationCommentRepository;
    private final PublicationCommentReportRepository publicationCommentReportRepository;
    private final PublicationReportRepository publicationReportRepository;
    private final IUtilisateurRepository utilisateurRepository;
    private final EmailService emailService;
    private final NotificationProducerService notificationProducerService;
    private final SentimentAnalysisClient sentimentAnalysisClient;

    private static final Set<String> FORBIDDEN_WORDS = Set.of(
            "fuck", "shit", "bitch", "asshole",
            "pute", "putain", "merde", "con", "connard", "salope",
            "couille", "zob", "nik", "nique", "ntm"
    );
    
    /**
     * Crée une nouvelle publication
     */
    public Publication createPublication(Publication publication, Utilisateur createdBy) {
        if (createdBy == null) {
            throw new AccessDeniedException("Utilisateur non authentifie: creation de publication impossible");
        }
        publication.setCreatedBy(createdBy);
        
        // Logs pour les tags avant sauvegarde
        log.info("Tags avant sauvegarde: {}", publication.getAiGeneratedTags());
        log.info("Nombre de tags avant sauvegarde: {}", 
                publication.getAiGeneratedTags() != null ? publication.getAiGeneratedTags().size() : 0);
        
        // Ne définir le statut que s'il n'est pas déjà défini
        if (publication.getStatus() == null) {
            publication.setStatus(PublicationStatus.DRAFT);
            log.info("Statut DRAFT défini par défaut pour la publication");
        } else {
            log.info("Statut existant conservé: {}", publication.getStatus());
        }
        
        Publication saved = publicationRepository.save(publication);
        
        // Logs pour les tags après sauvegarde
        log.info("Tags après sauvegarde: {}", saved.getAiGeneratedTags());
        log.info("Nombre de tags après sauvegarde: {}", 
                saved.getAiGeneratedTags() != null ? saved.getAiGeneratedTags().size() : 0);
        
        log.info("Nouvelle publication créée: {} par {} avec statut: {}", saved.getId(), createdBy.getEmail(), saved.getStatus());

        if (saved.getStatus() == PublicationStatus.PUBLISHED) {
            notifyUsersAboutNewPublication(saved, createdBy);
            notifyPublicationPublished(saved);
        }

        return saved;
    }
    
    /**
     * Met à jour une publication existante
     */
    public Publication updatePublication(Long id, Publication publicationDetails, Utilisateur updatedBy) {
        Publication publication = getPublicationById(id);
        
        // Mise à jour des champs
        if (publicationDetails.getTitle() != null) {
            publication.setTitle(publicationDetails.getTitle());
        }
        if (publicationDetails.getSummary() != null) {
            publication.setSummary(publicationDetails.getSummary());
        }
        if (publicationDetails.getContent() != null) {
            publication.setContent(publicationDetails.getContent());
        }
        if (publicationDetails.getImageUrl() != null) {
            publication.setImageUrl(publicationDetails.getImageUrl());
        }
        if (publicationDetails.getLanguage() != null) {
            publication.setLanguage(publicationDetails.getLanguage());
        }
        if (publicationDetails.getIsPinned() != null) {
            publication.setIsPinned(publicationDetails.getIsPinned());
        }
        if (publicationDetails.getAiGeneratedTags() != null) {
            publication.setAiGeneratedTags(publicationDetails.getAiGeneratedTags());
        }
        if (publicationDetails.getRejectionReason() != null) {
            publication.setRejectionReason(publicationDetails.getRejectionReason());
        }
        
        if (updatedBy != null) {
            publication.setUpdatedBy(updatedBy);
        }
        Publication updated = publicationRepository.save(publication);
        String updater = updatedBy != null ? updatedBy.getEmail() : "utilisateur-inconnu";
        log.info("Publication {} mise à jour par {}", id, updater);
        return updated;
    }

    public Publication updatePublicationStatus(Long id, PublicationStatus status, String rejectionReason, Utilisateur updatedBy) {
        Publication publication = getPublicationById(id);
        publication.setStatus(status);
        if (rejectionReason != null) {
            publication.setRejectionReason(rejectionReason);
        }
        if (updatedBy != null) {
            publication.setUpdatedBy(updatedBy);
        }
        Publication updated = publicationRepository.save(publication);
        String updater = updatedBy != null ? updatedBy.getEmail() : "utilisateur-inconnu";
        log.info("Statut de la publication {} mis à jour à {} par {}", id, status, updater);
        if (status == PublicationStatus.PUBLISHED) {
            notifyPublicationPublished(updated);
        }
        return updated;
    }
    
    /**
     * Récupère une publication par son ID
     */
    public Publication getPublicationById(Long id) {
        return publicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publication non trouvée avec l'ID: " + id));
    }
    
    /**
     * Récupère une publication par son slug
     */
    public Publication getPublicationBySlug(String slug) {
        return publicationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Publication non trouvée avec le slug: " + slug));
    }
    
    /**
     * Récupère toutes les publications (non supprimées) avec pagination
     */
    public Page<Publication> getAllPublications(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return publicationRepository.findByIsDeletedFalse(pageable);
    }
    
    /**
     * Récupère les publications filtrées
     */
    public Page<Publication> getFilteredPublications(
            PublicationStatus status,
            String language,
            Boolean isPinned,
            Long createdBy,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return publicationRepository.filterPublications(
                status, language, isPinned, createdBy, dateFrom, dateTo, search, pageable);
    }
    
    /**
     * Récupère les publications par statut
     */
    public List<Publication> getPublicationsByStatus(PublicationStatus status) {
        return publicationRepository.findByStatusAndIsDeletedFalse(status);
    }
    
    /**
     * Récupère les publications épinglées
     */
    public List<Publication> getPinnedPublications() {
        return publicationRepository.findByIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc();
    }
    
    /**
     * Récupère les publications récentes
     */
    public List<Publication> getRecentPublications(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return publicationRepository.findAll(pageable).getContent();
    }
    
    /**
     * Récupère les publications populaires
     */
    public Page<Publication> getPopularPublications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return publicationRepository.findPopularPublications(pageable);
    }
    
    /**
     * Valide une publication
     */
    public Publication validatePublication(Long id, Utilisateur validatedBy) {
        Publication publication = getPublicationById(id);
        publication.setStatus(PublicationStatus.PUBLISHED);
        publication.setValidatedBy(validatedBy);
        publication.setValidatedAt(LocalDateTime.now());
        publication.setPublishedAt(LocalDateTime.now());
        
        Publication validated = publicationRepository.save(publication);
        log.info("Publication {} validée par {}", id, validatedBy.getEmail());
        notifyPublicationPublished(validated);
        return validated;
    }
    
    /**
     * Rejette une publication
     */
    public Publication rejectPublication(Long id, String rejectionReason, Utilisateur rejectedBy) {
        Publication publication = getPublicationById(id);
        publication.setStatus(PublicationStatus.REJECTED);
        publication.setRejectionReason(rejectionReason);
        publication.setValidatedBy(rejectedBy);
        publication.setValidatedAt(LocalDateTime.now());
        
        Publication rejected = publicationRepository.save(publication);
        log.info("Publication {} rejetée par {} - Raison: {}", id, rejectedBy.getEmail(), rejectionReason);
        return rejected;
    }
    
    /**
     * Archive une publication
     */
    public Publication archivePublication(Long id) {
        Publication publication = getPublicationById(id);
        publicationRepository.archivePublication(id);
        log.info("Publication {} archivée", id);
        return publication;
    }
    
    /**
     * Supprime (soft delete) une publication
     */
    public void deletePublication(Long id) {
        Publication publication = getPublicationById(id);
        publicationRepository.softDelete(id, LocalDateTime.now());
        log.info("Publication {} supprimée", id);
    }
    
    /**
     * Bascule le statut épinglé d'une publication
     */
    public Publication togglePinPublication(Long id) {
        Publication publication = getPublicationById(id);
        publication.setIsPinned(!publication.getIsPinned());
        
        Publication updated = publicationRepository.save(publication);
        log.info("Publication {} épinglée: {}", id, updated.getIsPinned());
        return updated;
    }
    
    /**
     * Incrémente le compteur de vues
     */
    public void incrementViewsCount(Long id) {
        publicationRepository.incrementViewsCount(id);
    }

    /**
     * Signale une publication
     */
    public void reportPublication(Long id, String reason) {
        Publication publication = getPublicationById(id);
        Optional<Utilisateur> currentUserOpt = getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            throw new AccessDeniedException("Utilisateur non authentifie: signalement impossible");
        }

        Utilisateur currentUser = currentUserOpt.get();
        String safeReason = reason != null ? reason.trim() : "";
        if (safeReason.isEmpty()) {
            safeReason = "Signalement sans motif";
        }

        PublicationReport report = new PublicationReport();
        report.setPublication(publication);
        report.setUtilisateur(currentUser);
        report.setReason(safeReason);
        publicationReportRepository.save(report);

        int currentReports = publication.getReportsCount() != null ? publication.getReportsCount() : 0;
        publication.setReportsCount(currentReports + 1);
        publicationRepository.save(publication);
        log.info(
                "Publication {} signalee par user {}. Nouveau compteur: {}. Raison: {}",
                id,
                currentUser.getIdUtilisateur(),
                publication.getReportsCount(),
                safeReason
        );
        sendNotificationToAdmins(
                "NEW_PUBLICATION_REPORT",
                "Nouveau signalement",
                "La publication \"" + (publication.getTitle() != null ? publication.getTitle() : "Sans titre") + "\" a ete signalee.",
                publication.getId()
        );
    }

    public List<PublicationReportDto> getReportsByPublication(Long publicationId) {
        getPublicationById(publicationId);
        return publicationReportRepository.findByPublication_IdOrderByCreatedAtDesc(publicationId)
                .stream()
                .map(this::toReportDto)
                .toList();
    }

    public void reportComment(Long publicationId, Long commentId, String reason) {
        Optional<Utilisateur> currentUserOpt = getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            throw new AccessDeniedException("Utilisateur non authentifie: signalement impossible");
        }

        Utilisateur currentUser = currentUserOpt.get();
        Publication publication = getPublicationById(publicationId);
        PublicationComment comment = publicationCommentRepository
                .findByIdAndPublication_Id(commentId, publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouve"));

        String safeReason = reason != null ? reason.trim() : "";
        if (safeReason.isEmpty()) {
            safeReason = "Signalement sans motif";
        }

        PublicationCommentReport report = new PublicationCommentReport();
        report.setPublication(publication);
        report.setComment(comment);
        report.setUtilisateur(currentUser);
        report.setReason(safeReason);
        publicationCommentReportRepository.save(report);

        log.info(
                "Commentaire {} (publication {}) signale par user {}. Raison: {}",
                commentId,
                publicationId,
                currentUser.getIdUtilisateur(),
                safeReason
        );

        String publicationTitle = publication.getTitle() != null ? publication.getTitle() : "Sans titre";
        sendNotificationToAdmins(
                "NEW_COMMENT_REPORT",
                "Nouveau signalement",
                "Un commentaire de la publication \"" + publicationTitle + "\" a ete signale.",
                publication.getId()
        );
    }

    public void blockCommentAuthor(Long publicationId, Long commentId, int durationHours, String reason) {
        Optional<Utilisateur> currentUserOpt = getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            throw new AccessDeniedException("Utilisateur non authentifie: blocage impossible");
        }
        Utilisateur admin = currentUserOpt.get();
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seul un administrateur peut bloquer un utilisateur");
        }
        if (durationHours <= 0) {
            throw new IllegalArgumentException("La duree de blocage doit etre positive");
        }

        PublicationComment comment = publicationCommentRepository
                .findByIdAndPublication_Id(commentId, publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouve"));

        Utilisateur targetUser = comment.getUtilisateur();
        if (targetUser == null) {
            throw new ResourceNotFoundException("Auteur du commentaire introuvable");
        }

        LocalDateTime blockedUntil = LocalDateTime.now().plusHours(durationHours);
        targetUser.setCommentBlockedUntil(blockedUntil);
        targetUser.setCommentBlockReason(reason != null ? reason.trim() : null);
        utilisateurRepository.save(targetUser);

        log.info(
                "Utilisateur {} bloque pour commentaires jusqu'a {} par admin {} (publication {}, commentaire {})",
                targetUser.getIdUtilisateur(),
                blockedUntil,
                admin.getIdUtilisateur(),
                publicationId,
                commentId
        );
    }

    public void unblockUserComments(Integer userId) {
        Optional<Utilisateur> currentUserOpt = getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            throw new AccessDeniedException("Utilisateur non authentifie: deblocage impossible");
        }
        Utilisateur admin = currentUserOpt.get();
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seul un administrateur peut debloquer un utilisateur");
        }

        Utilisateur targetUser = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        targetUser.setCommentBlockedUntil(null);
        targetUser.setCommentBlockReason(null);
        utilisateurRepository.save(targetUser);

        log.info("Utilisateur {} debloque pour commentaires par admin {}", userId, admin.getIdUtilisateur());
    }

    public List<Map<String, Object>> getCommentReportsByPublication(Long publicationId) {
        getPublicationById(publicationId);
        return publicationCommentReportRepository.findByPublication_IdOrderByCreatedAtDesc(publicationId)
                .stream()
                .map(this::toCommentReportDto)
                .toList();
    }
    
    /**
     * Gère les interactions (like/dislike/favorite)
     */
    public Publication handleInteraction(Long id, String interactionType) {
        Publication publication = getPublicationById(id);

        if ("favorite".equalsIgnoreCase(interactionType)) {
            publicationRepository.incrementFavoritesCount(id);
            return getPublicationById(id);
        }

        ReactionType newType;
        if ("like".equalsIgnoreCase(interactionType)) {
            newType = ReactionType.LIKE;
        } else if ("dislike".equalsIgnoreCase(interactionType)) {
            newType = ReactionType.DISLIKE;
        } else {
            throw new IllegalArgumentException("Type d'interaction non valide: " + interactionType);
        }

        Optional<Utilisateur> currentUserOpt = getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            throw new AccessDeniedException("Utilisateur non authentifié: réaction impossible");
        }

        Utilisateur currentUser = currentUserOpt.get();
        Optional<PublicationReaction> existingReactionOpt =
                publicationReactionRepository.findByPublication_IdAndUtilisateur_IdUtilisateur(
                        publication.getId(),
                        currentUser.getIdUtilisateur()
                );

        if (existingReactionOpt.isEmpty()) {
            PublicationReaction reaction = new PublicationReaction();
            reaction.setPublication(publication);
            reaction.setUtilisateur(currentUser);
            reaction.setReactionType(newType);
            publicationReactionRepository.save(reaction);
            applyReactionCount(publication, newType, true);
            return publicationRepository.save(publication);
        }

        PublicationReaction existingReaction = existingReactionOpt.get();
        if (existingReaction.getReactionType() == newType) {
            // Déjà réagi avec le même type: une seule réaction autorisée, on ne recalcule rien
            return publication;
        }

        applyReactionCount(publication, existingReaction.getReactionType(), false);
        existingReaction.setReactionType(newType);
        publicationReactionRepository.save(existingReaction);
        applyReactionCount(publication, newType, true);
        return publicationRepository.save(publication);
    }

    public List<PublicationCommentDto> getCommentsByPublication(Long publicationId) {
        getPublicationById(publicationId);
        return publicationCommentRepository.findByPublication_IdOrderByCreatedAtDesc(publicationId)
                .stream()
                .map(this::toCommentDto)
                .toList();
    }

    public PublicationCommentDto addComment(Long publicationId, String content) {
        Publication publication = getPublicationById(publicationId);
        Optional<Utilisateur> currentUserOpt = getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            throw new AccessDeniedException("Utilisateur non authentifie: commentaire impossible");
        }
        Utilisateur currentUser = currentUserOpt.get();
        if (currentUser.getCommentBlockedUntil() != null && currentUser.getCommentBlockedUntil().isAfter(LocalDateTime.now())) {
            String reason = currentUser.getCommentBlockReason() != null && !currentUser.getCommentBlockReason().isBlank()
                    ? currentUser.getCommentBlockReason()
                    : "motif non precise";
            throw new AccessDeniedException(
                    "Vous etes temporairement bloque pour commenter jusqu'au " + currentUser.getCommentBlockedUntil()
                            + " en raison de: " + reason + "."
            );
        }

        String safeContent = sanitizeCommentContent(content);
        if (safeContent.isEmpty()) {
            throw new IllegalArgumentException("Le contenu du commentaire est obligatoire");
        }
        validateNoForbiddenWords(safeContent);

        PublicationComment comment = new PublicationComment();
        comment.setPublication(publication);
        comment.setUtilisateur(currentUser);
        comment.setContent(safeContent);
        applyCommentSentiment(comment, safeContent);

        PublicationComment saved = publicationCommentRepository.save(comment);
        String publicationTitle = publication.getTitle() != null ? publication.getTitle() : "Sans titre";
        sendNotificationToAdmins(
                "NEW_COMMENT",
                "Nouveau commentaire",
                "Un nouveau commentaire a ete ajoute sur la publication \"" + publicationTitle + "\".",
                publication.getId()
        );
        return toCommentDto(saved);
    }

    public long getCommentsCount(Long publicationId) {
        return publicationCommentRepository.countByPublication_Id(publicationId);
    }

    private void applyReactionCount(Publication publication, ReactionType type, boolean increment) {
        int delta = increment ? 1 : -1;
        if (type == ReactionType.LIKE) {
            int current = publication.getLikesCount() != null ? publication.getLikesCount() : 0;
            publication.setLikesCount(Math.max(0, current + delta));
        } else {
            int current = publication.getDislikesCount() != null ? publication.getDislikesCount() : 0;
            publication.setDislikesCount(Math.max(0, current + delta));
        }
    }

    private Optional<Utilisateur> getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return Optional.empty();
        }
        return utilisateurRepository.findByEmail(email);
    }

    private PublicationCommentDto toCommentDto(PublicationComment comment) {
        PublicationCommentDto dto = new PublicationCommentDto();
        dto.setId(comment.getId());
        dto.setPublicationId(comment.getPublication().getId());
        dto.setContent(comment.getContent());
        dto.setSentimentLabel(comment.getSentimentLabel());
        dto.setSentimentScore(comment.getSentimentScore());
        dto.setSentimentConfidence(comment.getSentimentConfidence());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        Utilisateur user = comment.getUtilisateur();
        if (user != null) {
            dto.setUserId(user.getIdUtilisateur());
            dto.setUserFullName(buildUserFullName(user));
            dto.setPhoto(user.getPhoto());
            dto.setUserPhoto(user.getPhoto());
        }
        return dto;
    }

    private String buildUserFullName(Utilisateur user) {
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        return user.getEmail();
    }

    private PublicationReportDto toReportDto(PublicationReport report) {
        PublicationReportDto dto = new PublicationReportDto();
        dto.setId(report.getId());
        dto.setPublicationId(report.getPublication().getId());
        dto.setReason(report.getReason());
        dto.setCreatedAt(report.getCreatedAt());

        Utilisateur user = report.getUtilisateur();
        if (user != null) {
            dto.setUserId(user.getIdUtilisateur());
            dto.setUserFullName(buildUserFullName(user));
            dto.setUserEmail(user.getEmail());
        }
        return dto;
    }

    private Map<String, Object> toCommentReportDto(PublicationCommentReport report) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", report.getId());
        dto.put("publicationId", report.getPublication() != null ? report.getPublication().getId() : null);
        dto.put("commentId", report.getComment() != null ? report.getComment().getId() : null);
        dto.put("commentContent", report.getComment() != null ? report.getComment().getContent() : null);
        dto.put("reason", report.getReason());
        dto.put("createdAt", report.getCreatedAt());

        Utilisateur user = report.getUtilisateur();
        if (user != null) {
            dto.put("userId", user.getIdUtilisateur());
            dto.put("userFullName", buildUserFullName(user));
            dto.put("userEmail", user.getEmail());
            dto.put("userCommentBlockedUntil", user.getCommentBlockedUntil());
            dto.put("isUserCommentBlocked",
                    user.getCommentBlockedUntil() != null && user.getCommentBlockedUntil().isAfter(LocalDateTime.now()));
        }
        return dto;
    }
    
    /**
     * Récupère les publications en attente de validation
     */
    public List<Publication> getPendingValidationPublications() {
        return publicationRepository.findPendingValidation();
    }
    
    /**
     * Publie les publications programmées
     */
    @Transactional
    public void publishScheduledPublications() {
        List<Publication> scheduledPublications = publicationRepository.findScheduledToPublish(LocalDateTime.now());
        
        for (Publication publication : scheduledPublications) {
            publication.setStatus(PublicationStatus.PUBLISHED);
            publication.setPublishedAt(LocalDateTime.now());
            publicationRepository.save(publication);
            log.info("Publication programmée {} publiée automatiquement", publication.getId());
            notifyPublicationPublished(publication);
        }
    }
    
    /**
     * Récupère les statistiques des publications
     */
    public List<Object[]> getPublicationsStatistics() {
        return publicationRepository.getStatisticsByStatus();
    }
    
    /**
     * Compte le nombre de publications par statut
     */
    public long countPublicationsByStatus(PublicationStatus status) {
        return publicationRepository.countByStatusAndIsDeletedFalse(status);
    }
    
    /**
     * Vérifie si une publication existe par slug
     */
    public boolean existsBySlug(String slug) {
        return publicationRepository.existsBySlug(slug);
    }
    
    /**
     * Recherche des publications par tag
     */
    public List<Publication> searchPublicationsByTag(String tag) {
        return publicationRepository.findByTag(tag);
    }
    
    /**
     * Récupère les publications créées par un utilisateur
     */
    public Page<Publication> getPublicationsByCreator(Long creatorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return publicationRepository.findByCreatedBy_IdUtilisateurAndIsDeletedFalse(creatorId, pageable);
    }
    
    /**
     * Supprime un commentaire d'une publication
     */
    public void deleteComment(Long publicationId, Long commentId) {
        Optional<Utilisateur> currentUserOpt = getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            throw new AccessDeniedException("Utilisateur non authentifie: suppression impossible");
        }

        Utilisateur currentUser = currentUserOpt.get();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        PublicationComment existingComment = publicationCommentRepository
                .findByIdAndPublication_Id(commentId, publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouve"));

        if (!isAdmin && (existingComment.getUtilisateur() == null ||
                !currentUser.getIdUtilisateur().equals(existingComment.getUtilisateur().getIdUtilisateur()))) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que votre propre commentaire");
        }

        // Nettoyer les signalements lies au commentaire avant suppression pour eviter les erreurs FK.
        publicationCommentReportRepository.deleteByComment_Id(commentId);
        publicationCommentRepository.delete(existingComment);
        log.info(
                "Commentaire {} supprime de la publication {} par user {} (admin={})",
                commentId,
                publicationId,
                currentUser.getIdUtilisateur(),
                isAdmin
        );
    }
    
    /**
     * Met à jour un commentaire d'une publication
     */
    public PublicationCommentDto updateComment(Long publicationId, Long commentId, String content) {
        Optional<Utilisateur> currentUserOpt = getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            throw new AccessDeniedException("Utilisateur non authentifie: modification impossible");
        }

        PublicationComment comment = publicationCommentRepository
                .findByIdAndPublication_Id(commentId, publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouve"));

        Utilisateur currentUser = currentUserOpt.get();
        if (comment.getUtilisateur() == null ||
                !currentUser.getIdUtilisateur().equals(comment.getUtilisateur().getIdUtilisateur())) {
            throw new AccessDeniedException("Vous ne pouvez modifier que votre propre commentaire");
        }

        String safeContent = sanitizeCommentContent(content);
        if (safeContent.isEmpty()) {
            throw new IllegalArgumentException("Le contenu du commentaire est obligatoire");
        }
        validateNoForbiddenWords(safeContent);

        comment.setContent(safeContent);
        applyCommentSentiment(comment, safeContent);
        PublicationComment updated = publicationCommentRepository.save(comment);
        return toCommentDto(updated);
    }

    private void applyCommentSentiment(PublicationComment comment, String content) {
        SentimentAnalysisClient.SentimentResult result = sentimentAnalysisClient.analyze(content);
        comment.setSentimentLabel(result.label());
        comment.setSentimentScore(result.score());
        comment.setSentimentConfidence(result.confidence());
    }

    private String sanitizeCommentContent(String rawContent) {
        return rawContent != null ? rawContent.trim() : "";
    }

    private void validateNoForbiddenWords(String content) {
        for (String badWord : FORBIDDEN_WORDS) {
            String regex = "(?i)\\b" + Pattern.quote(badWord) + "\\b";
            if (Pattern.compile(regex).matcher(content).find()) {
                throw new IllegalArgumentException("Commentaire refuse: veuillez eviter les mots inappropries.");
            }
        }
    }

    private void notifyUsersAboutNewPublication(Publication publication, Utilisateur creator) {
        String creatorEmail = creator != null ? creator.getEmail() : null;
        List<Utilisateur> recipients = utilisateurRepository.findByEmailIsNotNull().stream()
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .filter(user -> creatorEmail == null || !user.getEmail().equalsIgnoreCase(creatorEmail))
                .toList();

        if (recipients.isEmpty()) {
            log.info("Aucun destinataire pour la notification de publication {}", publication.getId());
            return;
        }

        String subject = "Nouvelle publication fiscale disponible";
        String title = publication.getTitle() != null ? publication.getTitle() : "Sans titre";
        String summary = publication.getSummary() != null ? publication.getSummary() : "";
        String detailsLink = publication.getId() != null
                ? "http://localhost:4200/DetailActualite?id=" + publication.getId()
                : "http://localhost:4200/actualite";

        String bodyTemplate = """
                <!doctype html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1.0">
                  <title>Nouvelle publication</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f6fb;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f6fb;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e2e8f0;">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0ea5e9,#2563eb);padding:26px 24px;">
                              <h1 style="margin:0;color:#ffffff;font-size:22px;line-height:1.3;">Nouvelle publication fiscale</h1>
                              <p style="margin:8px 0 0;color:#dbeafe;font-size:14px;">SmartTax - Notification automatique</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:24px;">
                              <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#334155;">
                                Une nouvelle publication vient d'etre publiee.
                              </p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0;border-radius:12px;background:#f8fafc;">
                                <tr>
                                  <td style="padding:16px 18px;">
                                    <p style="margin:0 0 8px;font-size:12px;font-weight:700;letter-spacing:.4px;color:#64748b;text-transform:uppercase;">Titre</p>
                                    <p style="margin:0 0 14px;font-size:18px;font-weight:700;color:#0f172a;">%s</p>
                                    <p style="margin:0 0 8px;font-size:12px;font-weight:700;letter-spacing:.4px;color:#64748b;text-transform:uppercase;">Resume</p>
                                    <p style="margin:0;font-size:14px;line-height:1.7;color:#334155;">%s</p>
                                  </td>
                                </tr>
                              </table>
                              <table role="presentation" cellspacing="0" cellpadding="0" style="margin-top:22px;">
                                <tr>
                                  <td align="center" style="border-radius:10px;background:#2563eb;">
                                    <a href="%s" style="display:inline-block;padding:12px 18px;font-size:14px;font-weight:700;color:#ffffff;text-decoration:none;">
                                      Consulter la publication
                                    </a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:20px 0 0;font-size:12px;line-height:1.6;color:#94a3b8;">
                                Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>
                                <span style="color:#2563eb;">%s</span>
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:14px 24px;background:#f8fafc;border-top:1px solid #e2e8f0;">
                              <p style="margin:0;font-size:12px;color:#64748b;">Email automatique SmartTax. Merci de ne pas repondre directement a ce message.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """;
        String body = String.format(
                bodyTemplate,
                escapeHtml(title),
                escapeHtml(summary),
                detailsLink,
                detailsLink
        );

        for (Utilisateur recipient : recipients) {
            boolean sent = emailService.sendEmail(recipient.getEmail(), subject, body);
            if (!sent) {
                log.warn("Echec envoi email publication {} vers {}", publication.getId(), recipient.getEmail());
            }
        }
        log.info("Notification email publication {} envoyee a {} utilisateurs", publication.getId(), recipients.size());
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void notifyPublicationPublished(Publication publication) {
        String title = publication.getTitle() != null ? publication.getTitle() : "Sans titre";
        sendNotificationToAdmins(
                "PUBLICATION_PUBLISHED",
                "Publication publiee",
                "La publication \"" + title + "\" est publiee.",
                publication.getId()
        );
    }

    private void sendNotificationToAdmins(String eventType, String title, String message, Long publicationId) {
        List<Integer> recipientIds = new ArrayList<>();
        recipientIds.addAll(utilisateurRepository.findByRole(Role.ADMIN).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList());
        recipientIds.addAll(utilisateurRepository.findByRole(Role.AGENT).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList());
        List<Integer> uniqueRecipientIds = recipientIds.stream().distinct().toList();
        notificationProducerService.send(eventType, title, message, publicationId, uniqueRecipientIds);
    }
}
