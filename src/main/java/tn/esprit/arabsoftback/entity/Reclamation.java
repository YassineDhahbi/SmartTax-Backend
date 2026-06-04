package tn.esprit.arabsoftback.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reclamation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reference", unique = true, nullable = false, length = 50)
    private String reference;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeReclamation type;
    
    @Column(name = "categorie", nullable = false)
    private String categorie;
    
    @Column(name = "sujet", nullable = false)
    private String sujet;
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "urgence", nullable = false)
    private NiveauUrgence urgence;
    
    @Column(name = "reference_user", length = 100)
    private String referenceUser;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutReclamation statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_reclamation")
    @Builder.Default
    private EtatReclamation etatReclamation = EtatReclamation.EN_COURS;
    
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
    
    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;
    
    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;
    
    @Column(name = "motif_resolution", columnDefinition = "TEXT")
    private String motifResolution;
    
    @Column(name = "email_user", nullable = false)
    private String emailUser;
    
    @Column(name = "nom_user")
    private String nomUser;
    
    @Column(name = "telephone_user")
    private String telephoneUser;
    
    @Column(name = "pieces_jointes", columnDefinition = "TEXT")
    private String piecesJointes; // JSON array of file info
    
    @OneToMany(mappedBy = "reclamation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages;
    
    @Column(name = "archived", nullable = false)
    @Builder.Default
    private Boolean archived = false;
    
    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;
    
    // Enums
    public enum TypeReclamation {
        TECHNIQUE("Problème Technique"),
        FISCAL("Question Fiscale"),
        COMPTE("Problème de Compte"),
        DOCUMENT("Document Manquant"),
        PAIEMENT("Problème de Paiement"),
        AUTRE("Autre");
        
        private final String label;
        
        TypeReclamation(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    public enum NiveauUrgence {
        BASSE("Basse"),
        MOYENNE("Moyenne"),
        HAUTE("Haute"),
        URGENTE("Urgente");
        
        private final String label;
        
        NiveauUrgence(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    public enum StatutReclamation {
        BROUILLON("Brouillon"),
        SOUMIS("Soumis"),
        EN_COURS("En cours de traitement"),
        RESOLU("Résolu"),
        REJETE("Rejeté");
        
        private final String label;
        
        StatutReclamation(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }

    public enum EtatReclamation {
        EN_COURS("En cours"),
        TRAITE("Traite");

        private final String label;

        EtatReclamation(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
