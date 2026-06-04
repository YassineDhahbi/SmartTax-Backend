package tn.esprit.arabsoftback.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.arabsoftback.dto.PublicationReportDto;
import tn.esprit.arabsoftback.entity.Publication;
import tn.esprit.arabsoftback.entity.Publication.PublicationStatus;
import tn.esprit.arabsoftback.entity.Utilisateur;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interface pour le service de gestion des publications fiscales
 */
public interface IPublicationService {
    
    /**
     * Crée une nouvelle publication
     * @param publication Les données de la publication
     * @param createdBy L'utilisateur qui crée la publication
     * @return La publication créée
     */
    Publication createPublication(Publication publication, Utilisateur createdBy);
    
    /**
     * Met à jour une publication existante
     * @param id L'identifiant de la publication
     * @param publicationDetails Les nouvelles données de la publication
     * @param updatedBy L'utilisateur qui met à jour la publication
     * @return La publication mise à jour
     */
    Publication updatePublication(Long id, Publication publicationDetails, Utilisateur updatedBy);

    Publication updatePublicationStatus(Long id, PublicationStatus status, String rejectionReason, Utilisateur updatedBy);
    
    /**
     * Récupère une publication par son identifiant
     * @param id L'identifiant de la publication
     * @return La publication trouvée
     */
    Publication getPublicationById(Long id);
    
    /**
     * Récupère une publication par son slug
     * @param slug Le slug de la publication
     * @return La publication trouvée
     */
    Publication getPublicationBySlug(String slug);
    
    /**
     * Récupère toutes les publications avec pagination
     * @param page Le numéro de page
     * @param size La taille de la page
     * @param sortBy Le champ de tri
     * @param sortDir La direction du tri
     * @return La page des publications
     */
    Page<Publication> getAllPublications(int page, int size, String sortBy, String sortDir);
    
    /**
     * Récupère les publications filtrées selon plusieurs critères
     * @param status Le statut de la publication
     * @param language La langue de la publication
     * @param isPinned Si la publication est épinglée
     * @param createdBy L'identifiant du créateur
     * @param dateFrom La date de début
     * @param dateTo La date de fin
     * @param search Le terme de recherche
     * @param page Le numéro de page
     * @param size La taille de la page
     * @param sortBy Le champ de tri
     * @param sortDir La direction du tri
     * @return La page des publications filtrées
     */
    Page<Publication> getFilteredPublications(
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
    );
    
    /**
     * Récupère les publications par statut
     * @param status Le statut des publications
     * @return La liste des publications
     */
    List<Publication> getPublicationsByStatus(PublicationStatus status);
    
    /**
     * Récupère les publications épinglées
     * @return La liste des publications épinglées
     */
    List<Publication> getPinnedPublications();
    
    /**
     * Récupère les publications récentes
     * @param limit Le nombre maximum de publications
     * @return La liste des publications récentes
     */
    List<Publication> getRecentPublications(int limit);
    
    /**
     * Récupère les publications populaires
     * @param page Le numéro de page
     * @param size La taille de la page
     * @return La page des publications populaires
     */
    Page<Publication> getPopularPublications(int page, int size);
    
    /**
     * Valide une publication
     * @param id L'identifiant de la publication
     * @param validatedBy L'utilisateur qui valide la publication
     * @return La publication validée
     */
    Publication validatePublication(Long id, Utilisateur validatedBy);
    
    /**
     * Rejette une publication
     * @param id L'identifiant de la publication
     * @param rejectionReason La raison du rejet
     * @param rejectedBy L'utilisateur qui rejette la publication
     * @return La publication rejetée
     */
    Publication rejectPublication(Long id, String rejectionReason, Utilisateur rejectedBy);
    
    /**
     * Archive une publication
     * @param id L'identifiant de la publication
     * @return La publication archivée
     */
    Publication archivePublication(Long id);
    
    /**
     * Supprime (soft delete) une publication
     * @param id L'identifiant de la publication
     */
    void deletePublication(Long id);
    
    /**
     * Bascule le statut épinglé d'une publication
     * @param id L'identifiant de la publication
     * @return La publication mise à jour
     */
    Publication togglePinPublication(Long id);
    
    /**
     * Incrémente le compteur de vues d'une publication
     * @param id L'identifiant de la publication
     */
    void incrementViewsCount(Long id);

    /**
     * Signale une publication et incrémente son compteur de signalements
     * @param id L'identifiant de la publication
     * @param reason La raison du signalement
     */
    void reportPublication(Long id, String reason);

    List<PublicationReportDto> getReportsByPublication(Long publicationId);

    void reportComment(Long publicationId, Long commentId, String reason);

    void blockCommentAuthor(Long publicationId, Long commentId, int durationHours, String reason);

    void unblockUserComments(Integer userId);
    
    /**
     * Gère les interactions avec une publication (like/dislike/favorite)
     * @param id L'identifiant de la publication
     * @param interactionType Le type d'interaction
     * @return La publication mise à jour
     */
    Publication handleInteraction(Long id, String interactionType);
    
    /**
     * Récupère les publications en attente de validation
     * @return La liste des publications en attente
     */
    List<Publication> getPendingValidationPublications();
    
    /**
     * Publie les publications programmées
     */
    void publishScheduledPublications();
    
    /**
     * Récupère les statistiques des publications
     * @return Les statistiques par statut
     */
    List<Object[]> getPublicationsStatistics();
    
    /**
     * Compte le nombre de publications par statut
     * @param status Le statut des publications
     * @return Le nombre de publications
     */
    long countPublicationsByStatus(PublicationStatus status);
    
    /**
     * Vérifie si une publication existe par slug
     * @param slug Le slug à vérifier
     * @return true si la publication existe, false sinon
     */
    boolean existsBySlug(String slug);
    
    /**
     * Recherche des publications par tag
     * @param tag Le tag de recherche
     * @return La liste des publications trouvées
     */
    List<Publication> searchPublicationsByTag(String tag);
    
    /**
     * Récupère les publications créées par un utilisateur
     * @param creatorId L'identifiant du créateur
     * @param page Le numéro de page
     * @param size La taille de la page
     * @return La page des publications
     */
    Page<Publication> getPublicationsByCreator(Long creatorId, int page, int size);
}
