package tn.esprit.arabsoftback.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationDto {
    private Long id;
    
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String title;
    
    @NotBlank(message = "Le résumé est obligatoire")
    @Size(max = 500, message = "Le résumé ne doit pas dépasser 500 caractères")
    private String summary;
    
    @NotBlank(message = "Le contenu est obligatoire")
    @Size(max = 10000, message = "Le contenu ne doit pas dépasser 10000 caractères")
    private String content;
    
    private String imageUrl;
    
    @NotBlank(message = "La langue est obligatoire")
    private String language = "fr";
    
    private Boolean isPinned = false;
    
    private LocalDateTime scheduledAt;
    
    private List<String> aiGeneratedTags;
    
    // Statistiques (lecture seule)
    private Integer viewsCount = 0;
    private Integer commentsCount = 0;
    private Integer likesCount = 0;
    private Integer dislikesCount = 0;
    private Integer favoritesCount = 0;
    private Integer reportsCount = 0;
    
    // Informations de création (lecture seule)
    private Integer createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    
    // Informations de validation (lecture seule)
    private Integer validatedBy;
    private String validatedByName;
    private LocalDateTime validatedAt;
    private LocalDateTime publishedAt;
    
    // Statut et métadonnées
    private String status = "DRAFT";
    private String slug;
    private Double sentimentScore = 0.0;
    private Boolean isArchived = false;
    private Boolean isDeleted = false;
    private LocalDateTime updatedAt;
    private String rejectionReason;
    
    // DTO pour la création
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePublicationRequest {
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
        private String title;
        
        @NotBlank(message = "Le résumé est obligatoire")
        @Size(max = 500, message = "Le résumé ne doit pas dépasser 500 caractères")
        private String summary;
        
        @NotBlank(message = "Le contenu est obligatoire")
        @Size(max = 10000, message = "Le contenu ne doit pas dépasser 10000 caractères")
        private String content;
        
        private String imageUrl;
        
        @NotBlank(message = "La langue est obligatoire")
        private String language = "fr";
        
        private Boolean isPinned = false;
        
        private LocalDateTime scheduledAt;
        
        private String status; // DRAFT, PUBLISHED, SCHEDULED
        
        private List<String> aiGeneratedTags;
    }
    
    // DTO pour la mise à jour
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePublicationRequest {
        @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
        private String title;
        
        @Size(max = 500, message = "Le résumé ne doit pas dépasser 500 caractères")
        private String summary;
        
        @Size(max = 10000, message = "Le contenu ne doit pas dépasser 10000 caractères")
        private String content;
        
        private String imageUrl;
        
        private String language;
        
        private Boolean isPinned;
        
        private LocalDateTime scheduledAt;
        
        private List<String> aiGeneratedTags;
        
        private String status;
        
        @Size(max = 1000, message = "La raison de rejet ne doit pas dépasser 1000 caractères")
        private String rejectionReason;
    }
    
    // DTO pour la validation
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidatePublicationRequest {
        private Boolean isValidated;
        
        @Size(max = 1000, message = "La raison de rejet ne doit pas dépasser 1000 caractères")
        private String rejectionReason;
    }
    
    // DTO pour le filtre
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationFilterRequest {
        private String status;
        private String language;
        private Boolean isPinned;
        private Long createdBy;
        private String dateFrom;
        private String dateTo;
        private String search;
        private String tag;
        private int page = 0;
        private int size = 10;
        private String sortBy = "createdAt";
        private String sortDir = "desc";
    }
    
    // DTO pour la réponse paginée
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationResponse {
        private List<PublicationDto> data;
        private PaginationInfo pagination;
        private PublicationStats stats;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PaginationInfo {
            private int currentPage;
            private int totalPages;
            private long totalItems;
            private int itemsPerPage;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PublicationStats {
            private long total;
            private long published;
            private long draft;
            private long pending;
            private long rejected;
            private long archived;
            private long totalViews;
            private long totalLikes;
            private long totalDislikes;
            private long totalFavorites;
            private long totalReports;
        }
    }
    
    // DTO pour l'interaction
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationInteractionRequest {
        @NotBlank(message = "Le type d'interaction est obligatoire")
        private String type; // like, dislike, favorite
        
        @NotBlank(message = "L'ID de la publication est obligatoire")
        private String publicationId;
    }
    
    // DTO pour la recherche avancée
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvancedSearchRequest {
        private String query;
        private List<String> tags;
        private List<String> languages;
        private List<String> statuses;
        private List<Long> creators;
        private String dateFrom;
        private String dateTo;
        private boolean includeArchived = false;
        private boolean includeDeleted = false;
        private int page = 0;
        private int size = 10;
        private String sortBy = "createdAt";
        private String sortDir = "desc";
    }
}
