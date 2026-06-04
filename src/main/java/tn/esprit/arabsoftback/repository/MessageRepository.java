package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.Message;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // Messages par réclamation
    List<Message> findByReclamationIdOrderByDateEnvoiAsc(Long reclamationId);
    
    // Messages par réclamation et auteur
    List<Message> findByReclamationIdAndAuteurOrderByDateEnvoiAsc(
            Long reclamationId, 
            Message.AuteurMessage auteur
    );

    long countByReclamation_IdAndAuteur(Long reclamationId, Message.AuteurMessage auteur);
    
    // Messages non lus
    @Query("SELECT m FROM Message m WHERE " +
           "m.reclamation.id = :reclamationId AND " +
           "m.auteur = :auteur AND " +
           "m.lu = false")
    List<Message> findUnreadMessages(
            @Param("reclamationId") Long reclamationId, 
            @Param("auteur") Message.AuteurMessage auteur
    );
    
    // Messages non lus du contribuable (pour l'agent)
    @Query("SELECT m FROM Message m WHERE " +
           "m.reclamation.id = :reclamationId AND " +
           "m.auteur = 'CONTRIBUTABLE' AND " +
           "m.lu = false")
    List<Message> findUnreadContribuableMessages(@Param("reclamationId") Long reclamationId);
    
    // Messages non lus de l'agent (pour le contribuable)
    @Query("SELECT m FROM Message m WHERE " +
           "m.reclamation.id = :reclamationId AND " +
           "m.auteur = 'AGENT' AND " +
           "m.lu = false")
    List<Message> findUnreadAgentMessages(@Param("reclamationId") Long reclamationId);
    
    // Compter les messages non lus
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
           "m.reclamation.id = :reclamationId AND " +
           "m.auteur = :auteur AND " +
           "m.lu = false")
    long countUnreadMessages(
            @Param("reclamationId") Long reclamationId, 
            @Param("auteur") Message.AuteurMessage auteur
    );
    
    // Compter tous les messages non lus pour un utilisateur
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
           "m.reclamation.emailUser = :emailUser AND " +
           "m.auteur = 'AGENT' AND " +
           "m.lu = false")
    long countUnreadAgentMessagesByUser(@Param("emailUser") String emailUser);
    
    // Dernier message d'une réclamation
    @Query("SELECT m FROM Message m WHERE " +
           "m.reclamation.id = :reclamationId " +
           "ORDER BY m.dateEnvoi DESC")
    List<Message> findLastMessages(@Param("reclamationId") Long reclamationId);
    
    // Messages dans une plage de dates
    @Query("SELECT m FROM Message m WHERE " +
           "m.reclamation.id = :reclamationId AND " +
           "m.dateEnvoi BETWEEN :startDate AND :endDate " +
           "ORDER BY m.dateEnvoi ASC")
    List<Message> findByReclamationIdAndDateEnvoiBetween(
            @Param("reclamationId") Long reclamationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    // Messages avec pièce jointe
    @Query("SELECT m FROM Message m WHERE " +
           "m.reclamation.id = :reclamationId AND " +
           "m.pieceJointe IS NOT NULL " +
           "ORDER BY m.dateEnvoi DESC")
    List<Message> findMessagesWithAttachments(@Param("reclamationId") Long reclamationId);
    
    // Marquer les messages comme lus
    @Modifying
    @Query("UPDATE Message m SET m.lu = true, m.dateLecture = :dateLecture WHERE " +
           "m.reclamation.id = :reclamationId AND " +
           "m.auteur = :auteur AND " +
           "m.lu = false")
    void markMessagesAsRead(
            @Param("reclamationId") Long reclamationId, 
            @Param("auteur") Message.AuteurMessage auteur,
            @Param("dateLecture") LocalDateTime dateLecture
    );
    
    // Supprimer les messages d'une réclamation
    void deleteByReclamationId(Long reclamationId);
}
