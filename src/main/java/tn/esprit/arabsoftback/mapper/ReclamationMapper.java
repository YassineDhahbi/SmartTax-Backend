package tn.esprit.arabsoftback.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.arabsoftback.dto.MessageDto;
import tn.esprit.arabsoftback.dto.ReclamationDto;
import tn.esprit.arabsoftback.entity.Message;
import tn.esprit.arabsoftback.entity.Reclamation;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReclamationMapper {
    
    private final ObjectMapper objectMapper;
    
    // Entity vers DTO
    public ReclamationDto toDto(Reclamation entity) {
        if (entity == null) {
            return null;
        }
        
        ReclamationDto dto = ReclamationDto.builder()
                .id(entity.getId())
                .reference(entity.getReference())
                .type(mapTypeToDto(entity.getType()))
                .categorie(entity.getCategorie())
                .sujet(entity.getSujet())
                .description(entity.getDescription())
                .urgence(mapUrgenceToDto(entity.getUrgence()))
                .referenceUser(entity.getReferenceUser())
                .statut(mapStatutToDto(entity.getStatut()))
                .etatReclamation(mapEtatToDto(entity.getEtatReclamation()))
                .dateCreation(entity.getDateCreation())
                .dateSoumission(entity.getDateSoumission())
                .dateResolution(entity.getDateResolution())
                .motifResolution(entity.getMotifResolution())
                .emailUser(entity.getEmailUser())
                .nomUser(entity.getNomUser())
                .telephoneUser(entity.getTelephoneUser())
                .archived(entity.getArchived())
                .dateArchivage(entity.getDateArchivage())
                .build();
        
        // Mapper les pièces jointes
        if (entity.getPiecesJointes() != null && !entity.getPiecesJointes().isEmpty()) {
            try {
                List<ReclamationDto.PieceJointeDto> piecesJointes = objectMapper.readValue(
                    entity.getPiecesJointes(),
                    new TypeReference<List<ReclamationDto.PieceJointeDto>>() {}
                );
                dto.setPiecesJointes(piecesJointes);
            } catch (JsonProcessingException e) {
                // En cas d'erreur, on laisse la liste vide
                dto.setPiecesJointes(List.of());
            }
        }
        
        // Mapper les messages
        if (entity.getMessages() != null) {
            List<MessageDto> messageDtos = entity.getMessages().stream()
                    .map(this::toMessageDto)
                    .collect(Collectors.toList());
            dto.setMessages(messageDtos);
        }
        
        return dto;
    }
    
    // DTO vers Entity
    public Reclamation toEntity(ReclamationDto dto) {
        if (dto == null) {
            return null;
        }
        
        Reclamation entity = Reclamation.builder()
                .id(dto.getId())
                .reference(dto.getReference())
                .type(mapTypeToEntity(dto.getType()))
                .categorie(dto.getCategorie())
                .sujet(dto.getSujet())
                .description(dto.getDescription())
                .urgence(mapUrgenceToEntity(dto.getUrgence()))
                .referenceUser(dto.getReferenceUser())
                .statut(mapStatutToEntity(dto.getStatut()))
                .etatReclamation(mapEtatToEntity(dto.getEtatReclamation()))
                .dateCreation(dto.getDateCreation())
                .dateSoumission(dto.getDateSoumission())
                .dateResolution(dto.getDateResolution())
                .motifResolution(dto.getMotifResolution())
                .emailUser(dto.getEmailUser())
                .nomUser(dto.getNomUser())
                .telephoneUser(dto.getTelephoneUser())
                .archived(dto.getArchived())
                .dateArchivage(dto.getDateArchivage())
                .build();
        
        // Mapper les pièces jointes
        if (dto.getPiecesJointes() != null && !dto.getPiecesJointes().isEmpty()) {
            try {
                String piecesJointesJson = objectMapper.writeValueAsString(dto.getPiecesJointes());
                entity.setPiecesJointes(piecesJointesJson);
            } catch (JsonProcessingException e) {
                // En cas d'erreur, on laisse le champ vide
                entity.setPiecesJointes(null);
            }
        }
        
        return entity;
    }
    
    // Create DTO vers Entity
    public Reclamation toEntity(ReclamationDto.CreateReclamationDto dto) {
        if (dto == null) {
            return null;
        }
        
        Reclamation entity = Reclamation.builder()
                .type(mapTypeToEntity(dto.getType()))
                .categorie(dto.getCategorie())
                .sujet(dto.getSujet())
                .description(dto.getDescription())
                .urgence(mapUrgenceToEntity(dto.getUrgence()))
                .referenceUser(dto.getReferenceUser())
                .nomUser(dto.getNomUser())
                .telephoneUser(dto.getTelephoneUser())
                .statut(Reclamation.StatutReclamation.BROUILLON) // Par défaut en brouillon
                .etatReclamation(Reclamation.EtatReclamation.EN_COURS)
                .archived(false)
                .build();
        
        // Mapper les pièces jointes
        if (dto.getPiecesJointes() != null && !dto.getPiecesJointes().isEmpty()) {
            try {
                String piecesJointesJson = objectMapper.writeValueAsString(dto.getPiecesJointes());
                entity.setPiecesJointes(piecesJointesJson);
            } catch (JsonProcessingException e) {
                entity.setPiecesJointes(null);
            }
        }
        
        return entity;
    }
    
    // Message Entity vers DTO
    public MessageDto toMessageDto(Message entity) {
        if (entity == null) {
            return null;
        }
        
        MessageDto dto = MessageDto.builder()
                .id(entity.getId())
                .reclamationId(entity.getReclamation() != null ? entity.getReclamation().getId() : null)
                .contenu(entity.getContenu())
                .auteur(mapAuteurToDto(entity.getAuteur()))
                .dateEnvoi(entity.getDateEnvoi())
                .lu(entity.getLu())
                .dateLecture(entity.getDateLecture())
                .build();
        
        // Mapper la pièce jointe si présente
        if (entity.getPieceJointe() != null && !entity.getPieceJointe().isEmpty()) {
            try {
                MessageDto.PieceJointeDto pieceJointe = objectMapper.readValue(
                    entity.getPieceJointe(),
                    MessageDto.PieceJointeDto.class
                );
                dto.setPieceJointe(pieceJointe);
            } catch (JsonProcessingException e) {
                dto.setPieceJointe(null);
            }
        }
        
        return dto;
    }
    
    // Message DTO vers Entity
    public Message toMessageEntity(MessageDto dto, Reclamation reclamation) {
        if (dto == null) {
            return null;
        }
        
        Message entity = Message.builder()
                .id(dto.getId())
                .reclamation(reclamation)
                .contenu(dto.getContenu())
                .auteur(mapAuteurToEntity(dto.getAuteur()))
                .dateEnvoi(dto.getDateEnvoi())
                .lu(dto.getLu())
                .dateLecture(dto.getDateLecture())
                .build();
        
        // Mapper la pièce jointe si présente
        if (dto.getPieceJointe() != null) {
            try {
                String pieceJointeJson = objectMapper.writeValueAsString(dto.getPieceJointe());
                entity.setPieceJointe(pieceJointeJson);
            } catch (JsonProcessingException e) {
                entity.setPieceJointe(null);
            }
        }
        
        return entity;
    }
    
    // Méthodes de mapping pour les enums
    public ReclamationDto.TypeReclamationDto mapTypeToDto(Reclamation.TypeReclamation type) {
        if (type == null) return null;
        return ReclamationDto.TypeReclamationDto.builder()
                .value(type.name())
                .label(type.getLabel())
                .build();
    }
    
    public Reclamation.TypeReclamation mapTypeToEntity(ReclamationDto.TypeReclamationDto dto) {
        if (dto == null || dto.getValue() == null) return null;
        return Reclamation.TypeReclamation.valueOf(dto.getValue());
    }
    
    public ReclamationDto.NiveauUrgenceDto mapUrgenceToDto(Reclamation.NiveauUrgence urgence) {
        if (urgence == null) return null;
        return ReclamationDto.NiveauUrgenceDto.builder()
                .value(urgence.name())
                .label(urgence.getLabel())
                .build();
    }
    
    public Reclamation.NiveauUrgence mapUrgenceToEntity(ReclamationDto.NiveauUrgenceDto dto) {
        if (dto == null || dto.getValue() == null) return null;
        return Reclamation.NiveauUrgence.valueOf(dto.getValue());
    }
    
    public ReclamationDto.StatutReclamationDto mapStatutToDto(Reclamation.StatutReclamation statut) {
        if (statut == null) return null;
        return ReclamationDto.StatutReclamationDto.builder()
                .value(statut.name())
                .label(statut.getLabel())
                .build();
    }
    
    public Reclamation.StatutReclamation mapStatutToEntity(ReclamationDto.StatutReclamationDto dto) {
        if (dto == null || dto.getValue() == null) return null;
        return Reclamation.StatutReclamation.valueOf(dto.getValue());
    }

    public ReclamationDto.EtatReclamationDto mapEtatToDto(Reclamation.EtatReclamation etat) {
        if (etat == null) return null;
        return ReclamationDto.EtatReclamationDto.builder()
                .value(etat.name())
                .label(etat.getLabel())
                .build();
    }

    public Reclamation.EtatReclamation mapEtatToEntity(ReclamationDto.EtatReclamationDto dto) {
        if (dto == null || dto.getValue() == null) return null;
        return Reclamation.EtatReclamation.valueOf(dto.getValue());
    }
    
    public MessageDto.AuteurMessageDto mapAuteurToDto(Message.AuteurMessage auteur) {
        if (auteur == null) return null;
        return MessageDto.AuteurMessageDto.builder()
                .value(auteur.getValue())
                .label(auteur.getValue())
                .build();
    }
    
    public Message.AuteurMessage mapAuteurToEntity(MessageDto.AuteurMessageDto dto) {
        if (dto == null || dto.getValue() == null) return null;
        return Message.AuteurMessage.fromValue(dto.getValue());
    }
    
    // Mapping des listes
    public List<ReclamationDto> toDtoList(List<Reclamation> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<MessageDto> toMessageDtoList(List<Message> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }
}
