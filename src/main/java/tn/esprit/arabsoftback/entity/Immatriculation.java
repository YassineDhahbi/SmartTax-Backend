package tn.esprit.arabsoftback.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "immatriculation")
@Data
@NoArgsConstructor
public class Immatriculation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String dossierNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeContribuable typeContribuable;
    
    // Informations personne physique
    private String nom;
    private String prenom;
    private String cin;
    private LocalDate dateNaissance;
    
    // Informations personne morale
    private String raisonSociale;
    private String formeJuridique;
    @Column(columnDefinition = "TEXT")
    private String actionnaire;
    private String matriculeFiscalExistant;
    @Column(name = "matricule_fiscal")
    private String matriculeFiscal; // Nouveau TIN généré
    private String registreCommerce;
    private String representantLegal;
    
    // Champs communs
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String telephone;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String adresse;
    
    private String ville;
    private String autreVille;
    private String nationalite;
    
    // Informations activité
    @Column(nullable = false)
    private String typeActivite;
    
    @Column(nullable = false)
    private String secteur;
    
    @Column(nullable = false)
    private String adresseProfessionnelle;
    
    @Column(nullable = false)
    private LocalDate dateDebutActivite;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descriptionActivite;
    
    // Fichiers (stockés en base64 ou chemins)
    @Column(columnDefinition = "TEXT")
    private String identiteFile;
    
    @Column(columnDefinition = "TEXT")
    private String activiteFile;
    
    @Column(columnDefinition = "TEXT")
    private String photoFile;
    
    @ElementCollection
    @CollectionTable(name = "immatriculation_autres_files", joinColumns = @JoinColumn(name = "immatriculation_id"))
    @Column(columnDefinition = "TEXT")
    private List<String> autresFiles;
    
    // Scores de vérification
    @Column(nullable = false)
    private Integer overallScore = 0;
    
    @Column(nullable = false)
    private Integer completenessScore = 0;
    
    @Column(nullable = false)
    private Integer verificationScore = 0;
    
    @Column(nullable = false)
    private Integer documentsScore = 0;
    
    @Column(nullable = false)
    private Integer faceRecognitionScore = 0;
    
    /**
     * Score de validation de la pièce d'identité (SWIN), en pourcentage entier.
     * Colonne nullable pour compatibilité PostgreSQL (lignes existantes avant migration) et ddl-auto.
     */
    @Column(name = "identity_validation_score")
    private Integer identityValidationScore;
    
    // Vérifications
    @Column(nullable = false)
    private Boolean duplicateDetected = false;
    
    @Column(columnDefinition = "TEXT")
    private String ocrResults;
    
    // Workflow
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DossierStatus status = DossierStatus.BROUILLON;
    
    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
    
    private LocalDateTime dateSoumission;
    private LocalDateTime dateValidation;
    private LocalDateTime dateRejet;
    
    @Column(columnDefinition = "TEXT")
    private String motifRejet;
    
    @Column(nullable = false)
    private Boolean confirmed = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionMode submissionMode = SubmissionMode.SUBMIT;
    
    // Statut du dossier
    @Column(nullable = false)
    private Boolean archived = false;
    
    private LocalDateTime dateArchivage;

    /** Hash BCrypt du code de sécurité (création de compte), émis à la validation du dossier. */
    @Column(name = "registration_security_code_hash", length = 255)
    private String registrationSecurityCodeHash;

    /**
     * Indique si le code a déjà servi à créer un compte.
     * Nullable en base pour compatibilité avec les lignes existantes avant migration (null = non consommé).
     */
    @Column(name = "registration_security_code_consumed")
    private Boolean registrationSecurityCodeConsumed = false;

    @PostLoad
    private void normalizeRegistrationFlagsAfterLoad() {
        if (registrationSecurityCodeConsumed == null) {
            registrationSecurityCodeConsumed = false;
        }
        if (identityValidationScore == null) {
            identityValidationScore = 0;
        }
    }
    
    // Enumérations
    public enum TypeContribuable {
        PHYSIQUE, MORALE
    }
    
    public enum DossierStatus {
        BROUILLON, SOUMIS, EN_COURS_VERIFICATION, VALIDE, REJETE
    }
    
    public enum SubmissionMode {
        DRAFT, SUBMIT
    }
}
