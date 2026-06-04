package tn.esprit.arabsoftback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationReportDto {
    private Long id;
    private Long publicationId;
    private Integer userId;
    private String userFullName;
    private String userEmail;
    private String reason;
    private LocalDateTime createdAt;
}
