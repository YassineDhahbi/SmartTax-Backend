package tn.esprit.arabsoftback.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import tn.esprit.arabsoftback.entity.Immatriculation;
import tn.esprit.arabsoftback.entity.Trash;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrashDto {
    
    private Long id;
    private String originalId;
    private Trash.ItemType type;
    private TrashData data;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private Integer daysRemaining;
    private Boolean expired;
    private Boolean expiringSoon;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrashData {
        private String dossierNumber;
        private String nomContribuable;
        private Immatriculation.TypeContribuable typeContribuable;
        private String email;
        private String telephone;
        private Immatriculation.DossierStatus status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrashStats {
        private Long totalItems;
        private Long expiringSoon;
        private Long expired;
    }
}
