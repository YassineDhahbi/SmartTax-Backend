package tn.esprit.arabsoftback.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_reclamation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclamation_id", nullable = false)
    private Reclamation reclamation;
    
    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "auteur", nullable = false)
    private AuteurMessage auteur;
    
    @CreationTimestamp
    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;
    
    @Column(name = "lu", nullable = false)
    private Boolean lu = false;
    
    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;
    
    @Column(name = "piece_jointe", columnDefinition = "TEXT")
    private String pieceJointe; // JSON file info if any
    
    // Enum pour l'auteur du message
    public enum AuteurMessage {
        CONTRIBUTABLE("contribuable"),
        AGENT("agent");
        
        private final String value;
        
        AuteurMessage(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static AuteurMessage fromValue(String value) {
            for (AuteurMessage auteur : values()) {
                if (auteur.value.equals(value)) {
                    return auteur;
                }
            }
            throw new IllegalArgumentException("Unknown auteur: " + value);
        }
    }
}
