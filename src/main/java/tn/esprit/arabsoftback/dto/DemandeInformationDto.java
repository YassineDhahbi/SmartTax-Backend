package tn.esprit.arabsoftback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class DemandeInformationDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "Le nom complet est obligatoire")
        @Size(max = 120, message = "Le nom complet ne doit pas depasser 120 caracteres")
        private String nomComplet;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format email invalide")
        @Size(max = 150, message = "L'email ne doit pas depasser 150 caracteres")
        private String email;

        @Size(max = 30, message = "Le telephone ne doit pas depasser 30 caracteres")
        private String telephone;

        @NotBlank(message = "Le sujet est obligatoire")
        @Size(max = 200, message = "Le sujet ne doit pas depasser 200 caracteres")
        private String sujet;

        @NotBlank(message = "Le message est obligatoire")
        @Size(max = 5000, message = "Le message ne doit pas depasser 5000 caracteres")
        private String message;

        private Boolean urgent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateResponse {
        private Long id;
        private String message;
        private LocalDateTime dateCreation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemResponse {
        private Long id;
        private String nomComplet;
        private String email;
        private String telephone;
        private String sujet;
        private String message;
        private Boolean urgent;
        private String traitementStatus;
        private Integer assignedAgentId;
        private String assignedAgentName;
        private LocalDateTime dateCreation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateTraitementStatusRequest {
        @NotBlank(message = "Le statut de traitement est obligatoire")
        @Pattern(regexp = "TRAITE|NON_TRAITE", message = "Le statut doit etre TRAITE ou NON_TRAITE")
        private String traitementStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignAgentRequest {
        private Integer agentId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {
        private long total;
        private List<ItemResponse> items;
        /** Renseignés uniquement en mode paginé ({@code page} non null côté API). */
        private Integer page;
        private Integer size;
        private Integer totalPages;
    }
}

