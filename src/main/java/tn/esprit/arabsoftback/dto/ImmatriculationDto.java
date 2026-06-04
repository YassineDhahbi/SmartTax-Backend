package tn.esprit.arabsoftback.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import tn.esprit.arabsoftback.entity.Immatriculation;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImmatriculationDto {
    
    // Informations de base
    private Long id;
    private String dossierNumber;
    private Immatriculation.TypeContribuable typeContribuable;
    
    // Personne Physique
    private String nom;
    private String prenom;
    private String cin;
    private LocalDate dateNaissance;
    
    // Personne Morale
    private String raisonSociale;
    private String formeJuridique;
    private String actionnaire;
    private String matriculeFiscal;
    private String registreCommerce;
    private String representantLegal;
    
    // Contact
    private String email;
    private String telephone;
    private String adresse;
    private String ville;
    private String autreVille;
    private String nationalite;
    
    // Activité
    private String typeActivite;
    private String secteur;
    private String adresseProfessionnelle;
    private LocalDate dateDebutActivite;
    private String descriptionActivite;
    
    // Fichiers (Base64)
    private String identiteFile;
    private String activiteFile;
    private String photoFile;
    private List<String> autresFiles;
    
    // Scores de validation
    private Integer overallScore;
    private Integer completenessScore;
    private Integer verificationScore;
    private Integer documentsScore;
    private Integer faceRecognitionScore;
    private Integer identityValidationScore;
    
    // Vérifications
    private Boolean duplicateDetected;
    private String ocrResults;
    
    // Workflow
    private Immatriculation.DossierStatus status;
    private Immatriculation.SubmissionMode submissionMode;
    private Boolean confirmed;
    
    // Métadonnées
    private String dateCreation;
    private String dateSoumission;
    private String dateValidation;
    private String dateRejet;
    private String motifRejet;
    
    // DTO pour la création (sans ID et dates)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateImmatriculationDto {
        private Immatriculation.TypeContribuable typeContribuable;
        private String nom;
        private String prenom;
        private String cin;
        private LocalDate dateNaissance;
        private String raisonSociale;
        private String formeJuridique;
        private String actionnaire;
        private String matriculeFiscal;
        private String registreCommerce;
        private String representantLegal;
        private String email;
        private String telephone;
        private String adresse;
        private String ville;
        private String autreVille;
        private String nationalite;
        private String typeActivite;
        private String secteur;
        private String adresseProfessionnelle;
        private LocalDate dateDebutActivite;
        private String descriptionActivite;
        private String identiteFile;
        private String activiteFile;
        private String photoFile;
        private List<String> autresFiles;
        private Integer faceRecognitionScore;
        private Integer identityValidationScore;
        private String ocrResults;
        private Immatriculation.SubmissionMode submissionMode;
        private Boolean confirmed;
    }
    
    // DTO pour la mise à jour
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateImmatriculationDto {
        private String nom;
        private String prenom;
        private String cin;
        private LocalDate dateNaissance;
        private String raisonSociale;
        private String formeJuridique;
        private String actionnaire;
        private String matriculeFiscal;
        private String registreCommerce;
        private String representantLegal;
        private String email;
        private String telephone;
        private String adresse;
        private String ville;
        private String autreVille;
        private String nationalite;
        private String typeActivite;
        private String secteur;
        private String adresseProfessionnelle;
        private LocalDate dateDebutActivite;
        private String descriptionActivite;
        private String identiteFile;
        private String activiteFile;
        private String photoFile;
        private List<String> autresFiles;
        private Integer faceRecognitionScore;
        private Integer identityValidationScore;
        private String ocrResults;
        private Immatriculation.DossierStatus status;
        private Boolean confirmed;
        private String motifRejet;

    }
    
    // DTO pour la recherche
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchImmatriculationDto {
        private String nom;
        private String prenom;
        private String email;
        private String cin;
        private Immatriculation.DossierStatus status;
        private Immatriculation.TypeContribuable typeContribuable;
        private Integer page = 0;
        private Integer size = 10;
        private String sortBy = "dateCreation";
        private String sortDirection = "desc";
    }
    
    // DTO pour la validation/rejet
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationDto {
        private String motifRejet;
    }
    
    // DTO pour les statistiques
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiqueDto {
        private String status;
        private Long count;
        private Double percentage;
    }
    
    // DTO pour le résumé
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DossierSummaryDto {
        private Long id;
        private String dossierNumber;
        private String nomOuRaisonSociale;
        private String email;
        private String telephone;
        private Immatriculation.TypeContribuable typeContribuable;
        private Immatriculation.DossierStatus status;
        private Integer overallScore;
        private String dateCreation;
        private String dateSoumission;
    }
}
