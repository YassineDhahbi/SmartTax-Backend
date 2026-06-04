package tn.esprit.arabsoftback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReclamationDto {
    
    private Long id;
    
    @NotBlank(message = "La référence est obligatoire")
    @Size(max = 50, message = "La référence ne peut pas dépasser 50 caractères")
    private String reference;
    
    @NotNull(message = "Le type est obligatoire")
    private TypeReclamationDto type;
    
    @NotBlank(message = "La catégorie est obligatoire")
    @Size(max = 100, message = "La catégorie ne peut pas dépasser 100 caractères")
    private String categorie;
    
    @NotBlank(message = "Le sujet est obligatoire")
    @Size(min = 5, max = 100, message = "Le sujet doit contenir entre 5 et 100 caractères")
    private String sujet;
    
    @NotBlank(message = "La description est obligatoire")
    @Size(min = 20, max = 1000, message = "La description doit contenir entre 20 et 1000 caractères")
    private String description;
    
    @NotNull(message = "Le niveau d'urgence est obligatoire")
    private NiveauUrgenceDto urgence;
    
    @Size(max = 100, message = "La référence utilisateur ne peut pas dépasser 100 caractères")
    private String referenceUser;
    
    @NotNull(message = "Le statut est obligatoire")
    private StatutReclamationDto statut;

    private EtatReclamationDto etatReclamation;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateSoumission;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateResolution;
    
    @Size(max = 500, message = "Le motif de résolution ne peut pas dépasser 500 caractères")
    private String motifResolution;
    
    @NotBlank(message = "L'email utilisateur est obligatoire")
    @Email(message = "L'email doit être valide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String emailUser;
    
    @Size(max = 100, message = "Le nom utilisateur ne peut pas dépasser 100 caractères")
    private String nomUser;
    
    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String telephoneUser;
    
    private List<PieceJointeDto> piecesJointes;
    
    private List<MessageDto> messages;

    /** Nombre de messages de l'agent non lus (contribuable) ; renseigné sur la liste / détail côté utilisateur. */
    private Long unreadAgentMessageCount;
    
    private Boolean archived;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateArchivage;
    
    // DTOs imbriqués
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypeReclamationDto {
        private String value;
        private String label;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NiveauUrgenceDto {
        private String value;
        private String label;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatutReclamationDto {
        private String value;
        private String label;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EtatReclamationDto {
        private String value;
        private String label;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PieceJointeDto {
        private String nom;
        private Long taille;
        private String type;
        private String url;
    }
    
    // DTO pour la création
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReclamationDto {
        @NotNull(message = "Le type est obligatoire")
        private TypeReclamationDto type;
        
        @NotBlank(message = "La catégorie est obligatoire")
        private String categorie;
        
        @NotBlank(message = "Le sujet est obligatoire")
        @Size(min = 5, max = 100, message = "Le sujet doit contenir entre 5 et 100 caractères")
        private String sujet;
        
        @NotBlank(message = "La description est obligatoire")
        @Size(min = 20, max = 1000, message = "La description doit contenir entre 20 et 1000 caractères")
        private String description;
        
        @NotNull(message = "Le niveau d'urgence est obligatoire")
        private NiveauUrgenceDto urgence;
        
        @Size(max = 100, message = "La référence utilisateur ne peut pas dépasser 100 caractères")
        private String referenceUser;
        
        @Size(max = 100, message = "Le nom utilisateur ne peut pas dépasser 100 caractères")
        private String nomUser;
        
        @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
        private String telephoneUser;
        
        private List<PieceJointeDto> piecesJointes;
    }
    
    // DTO pour la mise à jour
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReclamationDto {
        private TypeReclamationDto type;

        @Size(max = 100, message = "La catégorie ne peut pas dépasser 100 caractères")
        private String categorie;

        @Size(min = 5, max = 100, message = "Le sujet doit contenir entre 5 et 100 caractères")
        private String sujet;
        
        @Size(min = 20, max = 1000, message = "La description doit contenir entre 20 et 1000 caractères")
        private String description;
        
        private NiveauUrgenceDto urgence;
        
        private String referenceUser;
        
        private String motifResolution;

        @Size(max = 100, message = "Le nom utilisateur ne peut pas dépasser 100 caractères")
        private String nomUser;

        @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
        private String telephoneUser;
        
        private List<PieceJointeDto> piecesJointes;
    }
    
    // DTO pour la réponse de création
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReclamationResponse {
        private Long id;
        private String reference;
        private String message;
        private StatutReclamationDto statut;
    }
    
    // DTO pour les statistiques
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReclamationStatistics {
        private long total;
        private long brouillons;
        private long soumis;
        private long enCours;
        private long resolus;
        private long rejetes;
        private long nonLus;
    }
}
