package tn.esprit.arabsoftback.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventType;
    private String title;
    private String message;
    private Long publicationId;
    /** Réclamation concernée (ex. événement RECLAMATION_SOUMISE). */
    private Long reclamationId;
    private List<Integer> targetUserIds;
    private LocalDateTime createdAt;
}
