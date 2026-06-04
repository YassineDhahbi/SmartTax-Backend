package tn.esprit.arabsoftback.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "trash")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trash {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String originalId; // ID original de l'immatriculation
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String data; // Données sérialisées de l'immatriculation
    
    @Column(nullable = false)
    private LocalDateTime deletedAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private String deletedBy;
    
    // Constructeurs utiles
    public Trash(String originalId, ItemType type, String data, String deletedBy) {
        this.originalId = originalId;
        this.type = type;
        this.data = data;
        this.deletedBy = deletedBy;
        this.deletedAt = LocalDateTime.now();
    }
    
    // Méthodes utilitaires
    public int getDaysRemaining() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = deletedAt.plusDays(30);
        return (int) java.time.Duration.between(now, expiryDate).toDays();
    }
    
    public boolean isExpired() {
        return getDaysRemaining() <= 0;
    }
    
    public boolean isExpiringSoon() {
        int days = getDaysRemaining();
        return days > 0 && days <= 3;
    }
    
    public enum ItemType {
        IMMATRICULATION
    }
}
