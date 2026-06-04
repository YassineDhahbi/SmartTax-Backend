package tn.esprit.arabsoftback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDto {
    
    private Long id;
    
    private Long reclamationId;
    
    @NotBlank(message = "Le contenu du message est obligatoire")
    @Size(min = 5, max = 500, message = "Le message doit contenir entre 5 et 500 caractères")
    private String contenu;
    
    private AuteurMessageDto auteur;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateEnvoi;
    
    private Boolean lu;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateLecture;
    
    private PieceJointeDto pieceJointe;
    
    // DTO pour l'auteur
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuteurMessageDto {
        private String value;
        private String label;
    }
    
    // DTO pour pièce jointe
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
    public static class CreateMessageDto {
        @Size(max = 500, message = "Le message ne peut pas dépasser 500 caractères")
        private String contenu;
        
        private String auteur; // "contribuable" ou "agent"
        
        private PieceJointeDto pieceJointe;
    }
    
    // DTO pour la réponse
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageResponse {
        private Long id;
        private String contenu;
        private AuteurMessageDto auteur;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime dateEnvoi;
        private Boolean lu;
        private String message;
    }
}
