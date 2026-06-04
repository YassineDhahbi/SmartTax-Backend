package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.Trash;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrashRepository extends JpaRepository<Trash, Long> {
    
    /**
     * Trouve tous les éléments de la corbeille
     */
    List<Trash> findAllByOrderByDeletedAtDesc();
    
    /**
     * Trouve les éléments par type
     */
    List<Trash> findByTypeOrderByDeletedAtDesc(Trash.ItemType type);
    
    /**
     * Trouve les éléments expirés (plus de 30 jours)
     */
    @Query("SELECT t FROM Trash t WHERE t.deletedAt < :expiryDate")
    List<Trash> findExpiredItems(@Param("expiryDate") LocalDateTime expiryDate);
    
    /**
     * Trouve les éléments expirant bientôt (dans les 3 jours)
     */
    @Query("SELECT t FROM Trash t WHERE t.deletedAt >= :now AND t.deletedAt < :warningDate")
    List<Trash> findItemsExpiringSoon(@Param("now") LocalDateTime now, 
                                     @Param("warningDate") LocalDateTime warningDate);
    
    /**
     * Compte les éléments par type
     */
    long countByType(Trash.ItemType type);
    
    /**
     * Compte les éléments expirés
     */
    @Query("SELECT COUNT(t) FROM Trash t WHERE t.deletedAt < :expiryDate")
    long countExpiredItems(@Param("expiryDate") LocalDateTime expiryDate);
    
    /**
     * Compte les éléments expirant bientôt
     */
    @Query("SELECT COUNT(t) FROM Trash t WHERE t.deletedAt >= :now AND t.deletedAt < :warningDate")
    long countItemsExpiringSoon(@Param("now") LocalDateTime now, 
                               @Param("warningDate") LocalDateTime warningDate);
    
    /**
     * Supprime les éléments expirés
     */
    @Modifying
    @Query("DELETE FROM Trash t WHERE t.deletedAt < :expiryDate")
    int deleteExpiredItems(@Param("expiryDate") LocalDateTime expiryDate);
    
    /**
     * Vérifie si un ID original existe dans la corbeille
     */
    boolean existsByOriginalIdAndType(String originalId, Trash.ItemType type);
    
    /**
     * Trouve par ID original et type
     */
    Trash findByOriginalIdAndType(String originalId, Trash.ItemType type);
}
