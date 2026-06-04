package tn.esprit.arabsoftback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationCommentDto {

    private Long id;
    private Long publicationId;
    private Integer userId;
    private String userFullName;
    private String photo;
    private String userPhoto;
    private String content;
    private String sentimentLabel;
    private Double sentimentScore;
    private Double sentimentConfidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCommentRequest {
        @NotBlank(message = "Le commentaire est obligatoire")
        @Size(max = 2000, message = "Le commentaire ne doit pas depasser 2000 caracteres")
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCommentRequest {
        @NotBlank(message = "Le commentaire est obligatoire")
        @Size(max = 2000, message = "Le commentaire ne doit pas depasser 2000 caracteres")
        private String content;
    }
}
