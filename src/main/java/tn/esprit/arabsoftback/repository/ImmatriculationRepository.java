package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.Immatriculation;
import tn.esprit.arabsoftback.entity.Immatriculation.DossierStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImmatriculationRepository extends JpaRepository<Immatriculation, Long> {
    
    // Recherche par numéro de dossier
    Optional<Immatriculation> findByDossierNumber(String dossierNumber);
    
    // Recherche par matricule fiscal (TIN)
    Optional<Immatriculation> findByMatriculeFiscal(String matriculeFiscal);
    
    // Recherche par TIN avec toutes les informations
    @Query("SELECT i FROM Immatriculation i WHERE i.matriculeFiscal = :tin")
    Optional<Immatriculation> getByTIN(@Param("tin") String tin);

    // Recherche robuste TIN (nouveau champ + compatibilité ancien champ)
    Optional<Immatriculation> findTopByMatriculeFiscalOrMatriculeFiscalExistantOrderByIdDesc(
            String matriculeFiscal, String matriculeFiscalExistant);
    
    // Vérification des doublons
    boolean existsByCin(String cin);
    boolean existsByEmail(String email);
    boolean existsByRegistreCommerce(String registreCommerce);
    
    // Vérification des doublons (en excluant EN_COURS_VERIFICATION)
    @Query("SELECT COUNT(i) > 0 FROM Immatriculation i WHERE i.cin = :cin AND i.status != 'EN_COURS_VERIFICATION'")
    boolean existsByCinExcludingPending(@Param("cin") String cin);
    
    @Query("SELECT COUNT(i) > 0 FROM Immatriculation i WHERE i.email = :email AND i.status != 'EN_COURS_VERIFICATION'")
    boolean existsByEmailExcludingPending(@Param("email") String email);
    
    @Query("SELECT COUNT(i) > 0 FROM Immatriculation i WHERE i.registreCommerce = :registreCommerce AND i.status != 'EN_COURS_VERIFICATION'")
    boolean existsByRegistreCommerceExcludingPending(@Param("registreCommerce") String registreCommerce);
    
    // Recherche par statut
    List<Immatriculation> findByStatus(Immatriculation.DossierStatus status);
    List<Immatriculation> findByStatusAndArchivedFalse(Immatriculation.DossierStatus status);
    
    // Recherche par type de contribuable
    List<Immatriculation> findByTypeContribuable(Immatriculation.TypeContribuable typeContribuable);
    
    // Recherche par plage de dates
    List<Immatriculation> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);
    List<Immatriculation> findByDateSoumissionBetween(LocalDateTime debut, LocalDateTime fin);
    
    // Recherche par email ou téléphone
    List<Immatriculation> findByEmailContainingIgnoreCase(String email);
    List<Immatriculation> findByEmailIgnoreCaseAndArchivedFalseOrderByDateCreationDesc(String email);
    List<Immatriculation> findByTelephoneContainingIgnoreCase(String telephone);
    
    // Comptage par statut
    long countByStatus(Immatriculation.DossierStatus status);
    long countByStatusAndArchivedFalse(Immatriculation.DossierStatus status);
    
    // Dossiers récents
    @Query("SELECT i FROM Immatriculation i WHERE i.status = :status ORDER BY i.dateCreation DESC")
    List<Immatriculation> findRecentByStatus(@Param("status") DossierStatus status);
    
    // Recherche multi-critères
    @Query("SELECT i FROM Immatriculation i WHERE " +
           "(:nom IS NULL OR LOWER(i.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
           "(:prenom IS NULL OR LOWER(i.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) AND " +
           "(:email IS NULL OR LOWER(i.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:cin IS NULL OR i.cin = :cin) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:typeContribuable IS NULL OR i.typeContribuable = :typeContribuable)")
    List<Immatriculation> searchDossiers(@Param("nom") String nom,
                                        @Param("prenom") String prenom,
                                        @Param("email") String email,
                                        @Param("cin") String cin,
                                        @Param("status") DossierStatus status,
                                        @Param("typeContribuable") Immatriculation.TypeContribuable typeContribuable);
    
    // Statistiques
    @Query("SELECT COUNT(i) FROM Immatriculation i WHERE i.dateCreation >= :date")
    long countSinceDate(@Param("date") LocalDateTime date);
    
    @Query("SELECT i.status, COUNT(i) FROM Immatriculation i WHERE i.archived = false GROUP BY i.status")
    List<Object[]> getStatisticsByStatus();
    
    // Dossiers nécessitant une attention (scores faibles)
    @Query("SELECT i FROM Immatriculation i WHERE i.overallScore < 60 AND i.status IN ('SOUMIS', 'EN_COURS_VERIFICATION')")
    List<Immatriculation> findLowScoreDossiers();
    
    // Doublons potentiels
    @Query("SELECT i FROM Immatriculation i WHERE i.cin = :cin OR i.email = :email OR i.registreCommerce = :registreCommerce")
    List<Immatriculation> findPotentialDuplicates(@Param("cin") String cin,
                                                 @Param("email") String email,
                                                 @Param("registreCommerce") String registreCommerce);
}
