package tn.esprit.arabsoftback.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.arabsoftback.dto.NotificationDto;
import tn.esprit.arabsoftback.service.NotificationQueryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping("/me")
    public ResponseEntity<List<NotificationDto>> getMyNotifications() {
        return ResponseEntity.ok(notificationQueryService.getMyNotifications());
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> getMyUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationQueryService.getMyUnreadCount()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationQueryService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyNotification(@PathVariable Long id) {
        notificationQueryService.deleteMyNotification(id);
        return ResponseEntity.noContent().build();
    }
}
