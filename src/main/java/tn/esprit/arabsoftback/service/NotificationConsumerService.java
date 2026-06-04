package tn.esprit.arabsoftback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arabsoftback.entity.Notification;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.notification.NotificationEvent;
import tn.esprit.arabsoftback.repository.IUtilisateurRepository;
import tn.esprit.arabsoftback.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumerService {

    private final NotificationRepository notificationRepository;
    private final IUtilisateurRepository utilisateurRepository;

    @KafkaListener(topics = "${app.kafka.notification-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(NotificationEvent event) {
        if (event == null || event.getTargetUserIds() == null || event.getTargetUserIds().isEmpty()) {
            return;
        }

        for (Integer userId : event.getTargetUserIds()) {
            utilisateurRepository.findById(userId).ifPresent(user -> saveNotification(user, event));
        }
        log.info("Notification event consumed: {}", event.getEventType());
    }

    private void saveNotification(Utilisateur user, NotificationEvent event) {
        Notification notification = new Notification();
        notification.setUtilisateur(user);
        notification.setEventType(event.getEventType());
        notification.setTitle(event.getTitle());
        notification.setMessage(event.getMessage());
        notification.setPublicationId(event.getPublicationId());
        notification.setReclamationId(event.getReclamationId());
        notification.setRead(false);
        notificationRepository.save(notification);
    }
}
