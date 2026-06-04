package tn.esprit.arabsoftback.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.Publication;
import tn.esprit.arabsoftback.entity.Publication.PublicationStatus;
import tn.esprit.arabsoftback.entity.Utilisateur;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {
    
    // Recherche par slug
    Optional<Publication> findBySlug(String slug);
    
    // Recherche par statut
    List<Publication> findByStatus(PublicationStatus status);
    List<Publication> findByStatusAndIsDeletedFalse(PublicationStatus status);
    List<Publication> findByStatusAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(PublicationStatus status);
    
    // Recherche par langue
    List<Publication> findByLanguage(String language);
    List<Publication> findByLanguageAndIsDeletedFalse(String language);
    
    // Recherche par auteur
    List<Publication> findByCreatedBy_IdUtilisateur(Long createdBy);
    List<Publication> findByCreatedBy_IdUtilisateurAndIsDeletedFalse(Long createdBy);
    Page<Publication> findByCreatedBy_IdUtilisateurAndIsDeletedFalse(Long createdBy, Pageable pageable);
    
    // Recherche par statut épinglé
    List<Publication> findByIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc();
    
    // Publications récentes
    List<Publication> findTop10ByIsDeletedFalseOrderByCreatedAtDesc();
    List<Publication> findTop5ByStatusAndIsDeletedFalseOrderByCreatedAtDesc(PublicationStatus status);
    
    // Recherche par plage de dates
    List<Publication> findByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);
    List<Publication> findByCreatedAtBetweenAndIsDeletedFalse(LocalDateTime debut, LocalDateTime fin);
    
    // Recherche par titre ou contenu
    @Query("SELECT p FROM Publication p WHERE " +
           "p.isDeleted = false AND " +
           "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.summary) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Publication> searchPublications(@Param("search") String search, Pageable pageable);
    
    // Recherche multi-critères
    @Query("SELECT p FROM Publication p WHERE " +
           "p.isDeleted = false AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:language IS NULL OR p.language = :language) AND " +
           "(:isPinned IS NULL OR p.isPinned = :isPinned) AND " +
           "(:createdBy IS NULL OR p.createdBy.idUtilisateur = :createdBy) AND " +
           "(:dateFrom IS NULL OR p.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR p.createdAt <= :dateTo) AND " +
           "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.summary) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Publication> filterPublications(@Param("status") PublicationStatus status,
                                     @Param("language") String language,
                                     @Param("isPinned") Boolean isPinned,
                                     @Param("createdBy") Long createdBy,
                                     @Param("dateFrom") LocalDateTime dateFrom,
                                     @Param("dateTo") LocalDateTime dateTo,
                                     @Param("search") String search,
                                     Pageable pageable);
    
    // Comptage par statut
    long countByStatusAndIsDeletedFalse(PublicationStatus status);
    long countByIsPinnedTrueAndIsDeletedFalse();
    long countByLanguageAndIsDeletedFalse(String language);
    long countByCreatedBy_IdUtilisateurAndIsDeletedFalse(Long createdBy);
    
    // Statistiques
    @Query("SELECT p.status, COUNT(p) FROM Publication p WHERE p.isDeleted = false GROUP BY p.status")
    List<Object[]> getStatisticsByStatus();
    
    @Query("SELECT p.language, COUNT(p) FROM Publication p WHERE p.isDeleted = false GROUP BY p.language")
    List<Object[]> getStatisticsByLanguage();
    
    @Query("SELECT COUNT(p) FROM Publication p WHERE p.isDeleted = false AND p.createdAt >= :date")
    long countSinceDate(@Param("date") LocalDateTime date);
    
    // Publications populaires
    @Query("SELECT p FROM Publication p WHERE p.isDeleted = false AND p.status = 'PUBLISHED' ORDER BY p.viewsCount DESC, p.likesCount DESC")
    Page<Publication> findPopularPublications(Pageable pageable);
    
    // Publications avec le plus d'interactions
    @Query("SELECT p FROM Publication p WHERE p.isDeleted = false AND p.status = 'PUBLISHED' ORDER BY (p.likesCount + p.dislikesCount + p.favoritesCount) DESC")
    List<Publication> findMostInteractedPublications(Pageable pageable);
    
    // Publications par tags
    @Query("SELECT DISTINCT p FROM Publication p JOIN p.aiGeneratedTags tag WHERE " +
           "p.isDeleted = false AND " +
           "(:tag IS NULL OR LOWER(tag) LIKE LOWER(CONCAT('%', :tag, '%')))")
    List<Publication> findByTag(@Param("tag") String tag);
    
    // Vérification des doublons
    boolean existsBySlug(String slug);
    boolean existsByTitleAndCreatedBy_IdUtilisateur(String title, Long createdBy);
    
    // Publications en attente de validation
    @Query("SELECT p FROM Publication p WHERE p.status = 'PENDING' AND p.isDeleted = false ORDER BY p.createdAt ASC")
    List<Publication> findPendingValidation();
    
    // Publications programmées
    @Query("SELECT p FROM Publication p WHERE p.scheduledAt IS NOT NULL AND p.scheduledAt <= :now AND p.status = 'SCHEDULED' AND p.isDeleted = false")
    List<Publication> findScheduledToPublish(@Param("now") LocalDateTime now);
    
    // Mises à jour des compteurs
    @Modifying
    @Query("UPDATE Publication p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :id")
    void incrementViewsCount(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE Publication p SET p.likesCount = p.likesCount + 1 WHERE p.id = :id")
    void incrementLikesCount(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE Publication p SET p.likesCount = GREATEST(p.likesCount - 1, 0) WHERE p.id = :id")
    void decrementLikesCount(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE Publication p SET p.dislikesCount = p.dislikesCount + 1 WHERE p.id = :id")
    void incrementDislikesCount(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE Publication p SET p.dislikesCount = GREATEST(p.dislikesCount - 1, 0) WHERE p.id = :id")
    void decrementDislikesCount(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE Publication p SET p.favoritesCount = p.favoritesCount + 1 WHERE p.id = :id")
    void incrementFavoritesCount(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE Publication p SET p.favoritesCount = GREATEST(p.favoritesCount - 1, 0) WHERE p.id = :id")
    void decrementFavoritesCount(@Param("id") Long id);
    
    // Soft delete
    @Modifying
    @Query("UPDATE Publication p SET p.isDeleted = true, p.deletedAt = :deletedAt WHERE p.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
    
    // Publications non supprimées avec pagination
    Page<Publication> findByIsDeletedFalse(Pageable pageable);
    
    // Archivage
    @Modifying
    @Query("UPDATE Publication p SET p.isArchived = true, p.status = 'ARCHIVED' WHERE p.id = :id")
    void archivePublication(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Publication p SET p.createdBy = :replacementUser WHERE p.createdBy.idUtilisateur = :deletedUserId")
    int reassignCreatedBy(@Param("deletedUserId") Integer deletedUserId, @Param("replacementUser") Utilisateur replacementUser);

    @Modifying
    @Query("UPDATE Publication p SET p.updatedBy = NULL WHERE p.updatedBy.idUtilisateur = :deletedUserId")
    int clearUpdatedBy(@Param("deletedUserId") Integer deletedUserId);

    @Modifying
    @Query("UPDATE Publication p SET p.validatedBy = NULL WHERE p.validatedBy.idUtilisateur = :deletedUserId")
    int clearValidatedBy(@Param("deletedUserId") Integer deletedUserId);
}
