package tn.esprit.arabsoftback.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.arabsoftback.dto.PublicationDto;
import tn.esprit.arabsoftback.entity.Publication;
import tn.esprit.arabsoftback.entity.Utilisateur;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PublicationMapper {

    public PublicationDto toDto(Publication publication) {
        if (publication == null) {
            return null;
        }

        PublicationDto dto = new PublicationDto();
        dto.setId(publication.getId());
        dto.setTitle(publication.getTitle());
        dto.setSummary(publication.getSummary());
        dto.setContent(publication.getContent());
        dto.setImageUrl(publication.getImageUrl());
        dto.setLanguage(publication.getLanguage());
        dto.setIsPinned(publication.getIsPinned());
        dto.setScheduledAt(publication.getScheduledAt());
        dto.setAiGeneratedTags(publication.getAiGeneratedTags());
        
        // Statistiques
        dto.setViewsCount(publication.getViewsCount());
        dto.setLikesCount(publication.getLikesCount());
        dto.setDislikesCount(publication.getDislikesCount());
        dto.setFavoritesCount(publication.getFavoritesCount());
        dto.setReportsCount(publication.getReportsCount());
        
        // Informations de création
        if (publication.getCreatedBy() != null) {
            dto.setCreatedBy(publication.getCreatedBy().getIdUtilisateur());
            dto.setCreatedByName(getFullName(publication.getCreatedBy()));
        }
        dto.setCreatedAt(publication.getCreatedAt());
        
        // Informations de validation
        if (publication.getValidatedBy() != null) {
            dto.setValidatedBy(publication.getValidatedBy().getIdUtilisateur());
            dto.setValidatedByName(getFullName(publication.getValidatedBy()));
        }
        dto.setValidatedAt(publication.getValidatedAt());
        dto.setPublishedAt(publication.getPublishedAt());
        
        // Statut et métadonnées
        if (publication.getStatus() != null) {
            dto.setStatus(publication.getStatus().name());
        }
        dto.setSlug(publication.getSlug());
        dto.setSentimentScore(publication.getSentimentScore());
        dto.setIsArchived(publication.getIsArchived());
        dto.setIsDeleted(publication.getIsDeleted());
        dto.setUpdatedAt(publication.getUpdatedAt());
        dto.setRejectionReason(publication.getRejectionReason());
        
        return dto;
    }

    public Publication toEntity(PublicationDto dto) {
        if (dto == null) {
            return null;
        }

        Publication publication = new Publication();
        publication.setTitle(dto.getTitle());
        publication.setSummary(dto.getSummary());
        publication.setContent(dto.getContent());
        publication.setImageUrl(dto.getImageUrl());
        publication.setLanguage(dto.getLanguage());
        publication.setIsPinned(dto.getIsPinned());
        publication.setScheduledAt(dto.getScheduledAt());
        publication.setAiGeneratedTags(dto.getAiGeneratedTags());
        
        // Statistiques
        publication.setViewsCount(dto.getViewsCount());
        publication.setLikesCount(dto.getLikesCount());
        publication.setDislikesCount(dto.getDislikesCount());
        publication.setFavoritesCount(dto.getFavoritesCount());
        publication.setReportsCount(dto.getReportsCount());
        
        // Statut
        if (dto.getStatus() != null) {
            try {
                publication.setStatus(Publication.PublicationStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                publication.setStatus(Publication.PublicationStatus.DRAFT);
            }
        }
        
        publication.setSlug(dto.getSlug());
        publication.setSentimentScore(dto.getSentimentScore());
        publication.setIsArchived(dto.getIsArchived());
        publication.setIsDeleted(dto.getIsDeleted());
        publication.setUpdatedAt(dto.getUpdatedAt());
        publication.setRejectionReason(dto.getRejectionReason());
        
        return publication;
    }

    public List<PublicationDto> toDtoList(List<Publication> publications) {
        if (publications == null) {
            return List.of();
        }
        return publications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private String getFullName(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        
        String firstName = utilisateur.getFirstName();
        String lastName = utilisateur.getLastName();
        
        if (firstName != null && lastName != null) {
            return firstName.trim() + " " + lastName.trim();
        } else if (firstName != null) {
            return firstName.trim();
        } else if (lastName != null) {
            return lastName.trim();
        } else {
            return utilisateur.getEmail();
        }
    }
}
