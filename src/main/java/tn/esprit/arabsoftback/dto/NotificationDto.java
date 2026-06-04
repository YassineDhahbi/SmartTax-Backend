package tn.esprit.arabsoftback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String eventType;
    private String title;
    private String message;
    private Long publicationId;
    private Long reclamationId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
