package tn.esprit.arabsoftback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.arabsoftback.notification.NotificationEvent;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducerService {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${app.kafka.notification-topic}")
    private String notificationTopic;

    public void send(String eventType, String title, String message, Long publicationId, List<Integer> targetUserIds) {
        publish(new NotificationEvent(
                eventType,
                title,
                message,
                publicationId,
                null,
                targetUserIds,
                LocalDateTime.now()));
    }

    /**
     * Événement lié à une réclamation (publicationId null ; reclamationId renseigné).
     */
    public void sendWithReclamation(
            String eventType,
            String title,
            String message,
            Long reclamationId,
            List<Integer> targetUserIds) {
        publish(new NotificationEvent(
                eventType,
                title,
                message,
                null,
                reclamationId,
                targetUserIds,
                LocalDateTime.now()));
    }

    private void publish(NotificationEvent event) {
        if (event.getTargetUserIds() == null || event.getTargetUserIds().isEmpty()) {
            return;
        }
        String key = event.getEventType() != null ? event.getEventType() : "notification";
        kafkaTemplate.send(notificationTopic, key, event);
        log.info("Notification event {} sent for {} users", event.getEventType(), event.getTargetUserIds().size());
    }
}
