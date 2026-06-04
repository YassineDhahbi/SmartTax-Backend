package tn.esprit.arabsoftback.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Setter(AccessLevel.NONE)
    Integer idUtilisateur;

    String firstName;

    String lastName;

    String email;

    String password;

    String photo;

    String cinImagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'CONTRIBUABLE'")
    Role role;

    String status;

    @Column
    Date dateNaissance;

    @Column
    Date dateInscription;

    // Nouveaux champs pour la validation CIN
    private String cinValidationStatus; // 'valid', 'invalid', 'pending'
    private Double cinConfidence; // Pourcentage de confiance

    // Champs supplémentaires pour les agents
    private String telephone;
    private String departement;
    private String adresse;
    private String cin; // Numéro de CIN du contribuable
    private String matricule;
    private LocalDateTime commentBlockedUntil;
    private String commentBlockReason;

    /** Code à 8 chiffres reçu par email avec le TIN (non persisté). */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String securityCode;

    @PrePersist
    protected void onCreate() {
        this.dateInscription = new Date(System.currentTimeMillis());
        if (this.role == null) {
            this.role = Role.CONTRIBUABLE; // Valeur par défaut si non définie
        }
        if (this.status == null) {
            this.status = "actif"; // Statut par défaut 'actif' pour les nouveaux utilisateurs
        }
        if (this.cinValidationStatus == null) {
            this.cinValidationStatus = "pending";
        }
        if (this.cinConfidence == null) {
            this.cinConfidence = 0.0;
        }
    }
}