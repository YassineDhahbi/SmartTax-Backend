package tn.esprit.arabsoftback.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.arabsoftback.dto.ImmatriculationDto;
import tn.esprit.arabsoftback.entity.Immatriculation;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImmatriculationMapper {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Convertit Entity en DTO
     */
    public ImmatriculationDto toDto(Immatriculation entity) {
        if (entity == null) {
            return null;
        }
        
        ImmatriculationDto dto = new ImmatriculationDto();
        
        // Informations de base
        dto.setId(entity.getId());
        dto.setDossierNumber(entity.getDossierNumber());
        dto.setTypeContribuable(entity.getTypeContribuable());
        
        // Personne Physique
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setCin(entity.getCin());
        dto.setDateNaissance(entity.getDateNaissance());
        
        // Personne Morale
        dto.setRaisonSociale(entity.getRaisonSociale());
        dto.setFormeJuridique(entity.getFormeJuridique());
        dto.setActionnaire(entity.getActionnaire());
        dto.setMatriculeFiscal(entity.getMatriculeFiscal());
        dto.setRegistreCommerce(entity.getRegistreCommerce());
        dto.setRepresentantLegal(entity.getRepresentantLegal());
        
        // Contact
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setAdresse(entity.getAdresse());
        dto.setVille(entity.getVille());
        dto.setAutreVille(entity.getAutreVille());
        dto.setNationalite(entity.getNationalite());
        
        // Activité
        dto.setTypeActivite(entity.getTypeActivite());
        dto.setSecteur(entity.getSecteur());
        dto.setAdresseProfessionnelle(entity.getAdresseProfessionnelle());
        dto.setDateDebutActivite(entity.getDateDebutActivite());
        dto.setDescriptionActivite(entity.getDescriptionActivite());
        
        // Fichiers
        dto.setIdentiteFile(entity.getIdentiteFile());
        dto.setActiviteFile(entity.getActiviteFile());
        dto.setPhotoFile(entity.getPhotoFile());
        
        // Garder les autres fichiers comme List<String>
        dto.setAutresFiles(entity.getAutresFiles());
        
        // Scores
        dto.setOverallScore(entity.getOverallScore());
        dto.setCompletenessScore(entity.getCompletenessScore());
        dto.setVerificationScore(entity.getVerificationScore());
        dto.setDocumentsScore(entity.getDocumentsScore());
        dto.setFaceRecognitionScore(entity.getFaceRecognitionScore());
        dto.setIdentityValidationScore(
                entity.getIdentityValidationScore() != null ? entity.getIdentityValidationScore() : 0);
        
        // Vérifications
        dto.setDuplicateDetected(entity.getDuplicateDetected());
        dto.setOcrResults(entity.getOcrResults());
        
        // Workflow
        dto.setStatus(entity.getStatus());
        dto.setSubmissionMode(entity.getSubmissionMode());
        dto.setConfirmed(entity.getConfirmed());
        
        // Dates
        dto.setDateCreation(formatDate(entity.getDateCreation()));
        dto.setDateSoumission(formatDate(entity.getDateSoumission()));
        dto.setDateValidation(formatDate(entity.getDateValidation()));
        dto.setDateRejet(formatDate(entity.getDateRejet()));

        // Motif de rejet
        dto.setMotifRejet(entity.getMotifRejet());
        
        return dto;
    }
    
    
    /**
     * Convertit DTO de création en Entity
     */
    public Immatriculation toEntity(ImmatriculationDto.CreateImmatriculationDto dto) {
        if (dto == null) {
            return null;
        }
        
        Immatriculation entity = new Immatriculation();
        
        // Informations de base
        entity.setTypeContribuable(dto.getTypeContribuable());
        
        // Personne Physique
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setCin(dto.getCin());
        entity.setDateNaissance(dto.getDateNaissance());
        
        // Personne Morale
        entity.setRaisonSociale(dto.getRaisonSociale());
        entity.setFormeJuridique(dto.getFormeJuridique());
        entity.setActionnaire(dto.getActionnaire());
        entity.setMatriculeFiscalExistant(dto.getMatriculeFiscal());
        entity.setRegistreCommerce(dto.getRegistreCommerce());
        entity.setRepresentantLegal(dto.getRepresentantLegal());
        
        // Contact
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setAdresse(dto.getAdresse());
        entity.setVille(dto.getVille());
        entity.setAutreVille(dto.getAutreVille());
        entity.setNationalite(dto.getNationalite());
        
        // Activité
        entity.setTypeActivite(dto.getTypeActivite());
        entity.setSecteur(dto.getSecteur());
        entity.setAdresseProfessionnelle(dto.getAdresseProfessionnelle());
        entity.setDateDebutActivite(dto.getDateDebutActivite());
        entity.setDescriptionActivite(dto.getDescriptionActivite());
        
        // Fichiers
        entity.setIdentiteFile(dto.getIdentiteFile());
        entity.setActiviteFile(dto.getActiviteFile());
        entity.setPhotoFile(dto.getPhotoFile());
        
        // Garder les autres fichiers comme List<String> directement
        entity.setAutresFiles(dto.getAutresFiles());
        
        // Scores et vérifications
        entity.setFaceRecognitionScore(dto.getFaceRecognitionScore() != null ? dto.getFaceRecognitionScore() : 0);
        if (dto.getIdentityValidationScore() != null) {
            int swin = Math.max(0, Math.min(100, dto.getIdentityValidationScore()));
            entity.setIdentityValidationScore(swin);
        } else {
            entity.setIdentityValidationScore(0);
        }
        entity.setOcrResults(dto.getOcrResults());
        
        // Workflow
        entity.setSubmissionMode(dto.getSubmissionMode());
        entity.setConfirmed(dto.getConfirmed());
        
        
        // Statut initial
        // Statut initial - toujours EN_COURS_VERIFICATION pour nouvelle soumission
        entity.setStatus(Immatriculation.DossierStatus.EN_COURS_VERIFICATION);
        
        return entity;
    }
    
    /**
     * Met à jour une entity à partir d'un DTO de mise à jour
     */
    public void updateEntity(Immatriculation entity, ImmatriculationDto.UpdateImmatriculationDto dto) {
        if (entity == null || dto == null) {
            return;
        }
        
        System.out.println("DEBUG updateEntity - DTO status: " + dto.getStatus());
        System.out.println("DEBUG updateEntity - Entity status avant: " + entity.getStatus());
        
        // Personne Physique
        if (dto.getNom() != null) entity.setNom(dto.getNom());
        if (dto.getPrenom() != null) entity.setPrenom(dto.getPrenom());
        if (dto.getCin() != null) entity.setCin(dto.getCin());
        if (dto.getDateNaissance() != null) entity.setDateNaissance(dto.getDateNaissance());
        
        // Personne Morale
        if (dto.getRaisonSociale() != null) entity.setRaisonSociale(dto.getRaisonSociale());
        if (dto.getFormeJuridique() != null) entity.setFormeJuridique(dto.getFormeJuridique());
        if (dto.getActionnaire() != null) entity.setActionnaire(dto.getActionnaire());
        if (dto.getMatriculeFiscal() != null) entity.setMatriculeFiscalExistant(dto.getMatriculeFiscal());
        if (dto.getRegistreCommerce() != null) entity.setRegistreCommerce(dto.getRegistreCommerce());
        if (dto.getRepresentantLegal() != null) entity.setRepresentantLegal(dto.getRepresentantLegal());
        
        // Contact
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        if (dto.getTelephone() != null) entity.setTelephone(dto.getTelephone());
        if (dto.getAdresse() != null) entity.setAdresse(dto.getAdresse());
        if (dto.getVille() != null) entity.setVille(dto.getVille());
        if (dto.getAutreVille() != null) entity.setAutreVille(dto.getAutreVille());
        if (dto.getNationalite() != null) entity.setNationalite(dto.getNationalite());
        
        // Activité
        if (dto.getTypeActivite() != null) entity.setTypeActivite(dto.getTypeActivite());
        if (dto.getSecteur() != null) entity.setSecteur(dto.getSecteur());
        if (dto.getAdresseProfessionnelle() != null) entity.setAdresseProfessionnelle(dto.getAdresseProfessionnelle());
        if (dto.getDateDebutActivite() != null) entity.setDateDebutActivite(dto.getDateDebutActivite());
        if (dto.getDescriptionActivite() != null) entity.setDescriptionActivite(dto.getDescriptionActivite());
        
        // Fichiers
        if (dto.getIdentiteFile() != null) entity.setIdentiteFile(dto.getIdentiteFile());
        if (dto.getActiviteFile() != null) entity.setActiviteFile(dto.getActiviteFile());
        if (dto.getPhotoFile() != null) entity.setPhotoFile(dto.getPhotoFile());
        if (dto.getAutresFiles() != null) entity.setAutresFiles(dto.getAutresFiles());
        
        // Scores et vérifications
        if (dto.getFaceRecognitionScore() != null) entity.setFaceRecognitionScore(dto.getFaceRecognitionScore());
        if (dto.getIdentityValidationScore() != null) {
            int swin = Math.max(0, Math.min(100, dto.getIdentityValidationScore()));
            entity.setIdentityValidationScore(swin);
        }
        if (dto.getOcrResults() != null) entity.setOcrResults(dto.getOcrResults());
        
        // Confirmation
        if (dto.getConfirmed() != null) entity.setConfirmed(dto.getConfirmed());

        // Motif de rejet (important pour l'auto-save)
        if (dto.getMotifRejet() != null) entity.setMotifRejet(dto.getMotifRejet());
        
        // Statut (très important pour la réactivation)
        if (dto.getStatus() != null) {
            System.out.println("DEBUG updateEntity - Mise à jour du statut vers: " + dto.getStatus());
            entity.setStatus(dto.getStatus());
        }
        
        System.out.println("DEBUG updateEntity - Entity status après: " + entity.getStatus());
    }
    
    private Immatriculation.DossierStatus convertStatus(String status) {
        if (status == null) return null;
        
        switch (status) {
            case "BROUILLON":
                return Immatriculation.DossierStatus.BROUILLON;
            case "EN_COURS_VERIFICATION":
                return Immatriculation.DossierStatus.EN_COURS_VERIFICATION;
            case "SOUMIS":
                return Immatriculation.DossierStatus.SOUMIS;
            case "VALIDE":
                return Immatriculation.DossierStatus.VALIDE;
            case "REJETE":
                return Immatriculation.DossierStatus.REJETE;
           
            default:
                return null;
        }
    }
    
    /**
     * Convertit une liste d'entities en DTOs
     */
    public List<ImmatriculationDto> toDtoList(List<Immatriculation> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Convertit en DTO de résumé
     */
    public ImmatriculationDto.DossierSummaryDto toSummaryDto(Immatriculation entity) {
        if (entity == null) {
            return null;
        }
        
        ImmatriculationDto.DossierSummaryDto dto = new ImmatriculationDto.DossierSummaryDto();
        
        dto.setId(entity.getId());
        dto.setDossierNumber(entity.getDossierNumber());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setTypeContribuable(entity.getTypeContribuable());
        dto.setStatus(entity.getStatus());
        dto.setOverallScore(entity.getOverallScore());
        dto.setDateCreation(formatDate(entity.getDateCreation()));
        dto.setDateSoumission(formatDate(entity.getDateSoumission()));
        
        // Nom ou raison sociale selon le type
        if (entity.getTypeContribuable() == Immatriculation.TypeContribuable.PHYSIQUE) {
            dto.setNomOuRaisonSociale(entity.getNom() + " " + entity.getPrenom());
        } else {
            dto.setNomOuRaisonSociale(entity.getRaisonSociale());
        }
        
        return dto;
    }
    
    /**
     * Convertit une liste en DTOs de résumé
     */
    public List<ImmatriculationDto.DossierSummaryDto> toSummaryDtoList(List<Immatriculation> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
    
    // Méthodes utilitaires
    private String formatDate(java.time.LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }
}
