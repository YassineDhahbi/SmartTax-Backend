package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.Notification;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop20ByUtilisateur_IdUtilisateurOrderByCreatedAtDesc(Integer userId);
    long countByUtilisateur_IdUtilisateurAndIsReadFalse(Integer userId);
    Optional<Notification> findByIdAndUtilisateur_IdUtilisateur(Long id, Integer userId);
}
