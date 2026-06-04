package tn.esprit.arabsoftback.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.Reclamation;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long>, JpaSpecificationExecutor<Reclamation> {
    
    // Recherche par référence
    Optional<Reclamation> findByReference(String reference);
    
    // Recherche par email utilisateur
    List<Reclamation> findByEmailUserOrderByDateCreationDesc(String emailUser);
    
    // Recherche par statut
    List<Reclamation> findByStatutOrderByDateCreationDesc(Reclamation.StatutReclamation statut);
    
    // Recherche par type
    List<Reclamation> findByTypeOrderByDateCreationDesc(Reclamation.TypeReclamation type);
    
    // Recherche par urgence
    List<Reclamation> findByUrgenceOrderByDateCreationDesc(Reclamation.NiveauUrgence urgence);
    
    // Recherche par statut et email utilisateur (avec pagination)
    Page<Reclamation> findByEmailUserAndStatutOrderByDateCreationDesc(
            String emailUser, 
            Reclamation.StatutReclamation statut, 
            Pageable pageable
    );
    
    // Recherche par statut (avec pagination)
    Page<Reclamation> findByStatutOrderByDateCreationDesc(
            Reclamation.StatutReclamation statut, 
            Pageable pageable
    );

    /** Pagination par statut ; le tri est entièrement piloté par {@link Pageable}. */
    Page<Reclamation> findByStatut(Reclamation.StatutReclamation statut, Pageable pageable);

    @Query("SELECT r FROM Reclamation r WHERE r.statut = :statut AND (" +
           "LOWER(COALESCE(r.reference, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.sujet, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.categorie, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.emailUser, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.nomUser, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.referenceUser, '')) LIKE LOWER(CONCAT('%', :q, '%')) )")
    Page<Reclamation> findByStatutAndTextSearch(
            @Param("statut") Reclamation.StatutReclamation statut,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("SELECT r FROM Reclamation r WHERE " +
           "(LOWER(COALESCE(r.reference, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.sujet, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.categorie, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.emailUser, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.nomUser, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(r.referenceUser, '')) LIKE LOWER(CONCAT('%', :q, '%')) )")
    Page<Reclamation> findAllByTextSearch(@Param("q") String q, Pageable pageable);

    long countByStatutAndEtatReclamation(
            Reclamation.StatutReclamation statut,
            Reclamation.EtatReclamation etatReclamation
    );

    long countByStatutAndUrgenceIn(
            Reclamation.StatutReclamation statut,
            Collection<Reclamation.NiveauUrgence> urgences
    );
    
    // Recherche par email utilisateur (avec pagination)
    Page<Reclamation> findByEmailUserOrderByDateCreationDesc(
            String emailUser, 
            Pageable pageable
    );
    
    // Recherche textuelle
    @Query("SELECT r FROM Reclamation r WHERE " +
           "LOWER(r.sujet) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.reference) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.referenceUser) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Reclamation> searchReclamations(@Param("query") String query);
    
    // Recherche textuelle par utilisateur
    @Query("SELECT r FROM Reclamation r WHERE " +
           "r.emailUser = :emailUser AND (" +
           "LOWER(r.sujet) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.reference) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.referenceUser) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Reclamation> searchReclamationsByUser(
            @Param("emailUser") String emailUser, 
            @Param("query") String query
    );
    
    // Statistiques
    @Query("SELECT COUNT(r) FROM Reclamation r WHERE r.statut = :statut")
    long countByStatut(@Param("statut") Reclamation.StatutReclamation statut);
    
    @Query("SELECT COUNT(r) FROM Reclamation r WHERE r.emailUser = :emailUser")
    long countByEmailUser(@Param("emailUser") String emailUser);
    
    @Query("SELECT COUNT(r) FROM Reclamation r WHERE r.statut = :statut AND r.emailUser = :emailUser")
    long countByStatutAndEmailUser(
            @Param("statut") Reclamation.StatutReclamation statut, 
            @Param("emailUser") String emailUser
    );
    
    // Recherche par plage de dates
    @Query("SELECT r FROM Reclamation r WHERE " +
           "r.dateCreation BETWEEN :startDate AND :endDate " +
           "ORDER BY r.dateCreation DESC")
    List<Reclamation> findByDateCreationBetween(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate
    );
    
    // Reclamation non lues par l'agent
    @Query("SELECT r FROM Reclamation r WHERE " +
           "r.statut = 'SOUMIS' AND " +
           "NOT EXISTS (SELECT m FROM Message m WHERE m.reclamation = r AND m.auteur = 'agent')")
    List<Reclamation> findUnreadReclamations();
    
    // Réclamations en retard (plus de X jours sans réponse)
    @Query("SELECT r FROM Reclamation r WHERE " +
           "r.statut IN ('SOUMIS', 'EN_COURS') AND " +
           "r.dateCreation < :thresholdDate " +
           "ORDER BY r.dateCreation ASC")
    List<Reclamation> findOverdueReclamations(@Param("thresholdDate") LocalDateTime thresholdDate);
    
    // Réclamations non archivées
    @Query("SELECT r FROM Reclamation r WHERE r.archived = false ORDER BY r.dateCreation DESC")
    List<Reclamation> findActiveReclamations();
    
    // Réclamations archivées
    @Query("SELECT r FROM Reclamation r WHERE r.archived = true ORDER BY r.dateArchivage DESC")
    List<Reclamation> findArchivedReclamations();
    
    // Vérifier si une référence existe
    boolean existsByReference(String reference);
}
