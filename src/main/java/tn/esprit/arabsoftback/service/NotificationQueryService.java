package tn.esprit.arabsoftback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arabsoftback.dto.NotificationDto;
import tn.esprit.arabsoftback.entity.Notification;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.exception.ResourceNotFoundException;
import tn.esprit.arabsoftback.repository.IUtilisateurRepository;
import tn.esprit.arabsoftback.repository.NotificationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final IUtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public List<NotificationDto> getMyNotifications() {
        Utilisateur currentUser = getCurrentUserOrThrow();
        return notificationRepository.findTop20ByUtilisateur_IdUtilisateurOrderByCreatedAtDesc(currentUser.getIdUtilisateur())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getMyUnreadCount() {
        Utilisateur currentUser = getCurrentUserOrThrow();
        return notificationRepository.countByUtilisateur_IdUtilisateurAndIsReadFalse(currentUser.getIdUtilisateur());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Utilisateur currentUser = getCurrentUserOrThrow();
        Notification notification = notificationRepository
                .findByIdAndUtilisateur_IdUtilisateur(notificationId, currentUser.getIdUtilisateur())
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteMyNotification(Long notificationId) {
        Utilisateur currentUser = getCurrentUserOrThrow();
        Notification notification = notificationRepository
                .findByIdAndUtilisateur_IdUtilisateur(notificationId, currentUser.getIdUtilisateur())
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        notificationRepository.delete(notification);
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getPublicationId(),
                notification.getReclamationId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private Utilisateur getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Utilisateur non authentifie");
        }
        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Utilisateur non authentifie");
        }
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur introuvable"));
    }
}
