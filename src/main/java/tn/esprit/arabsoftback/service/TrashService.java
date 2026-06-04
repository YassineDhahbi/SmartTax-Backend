package tn.esprit.arabsoftback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arabsoftback.dto.TrashDto;
import tn.esprit.arabsoftback.entity.Immatriculation;
import tn.esprit.arabsoftback.entity.Trash;
import tn.esprit.arabsoftback.exception.TrashException;
import tn.esprit.arabsoftback.repository.ImmatriculationRepository;
import tn.esprit.arabsoftback.repository.TrashRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrashService {
    
    private final TrashRepository trashRepository;
    private final ImmatriculationRepository immatriculationRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Déplace une immatriculation vers la corbeille
     */
    public TrashDto moveImmatriculationToTrash(Long immatriculationId, String deletedBy) {
        log.info("Déplacement de l'immatriculation {} vers la corbeille par {}", immatriculationId, deletedBy);
        
        // Récupérer l'immatriculation
        Immatriculation immatriculation = immatriculationRepository.findById(immatriculationId)
                .orElseThrow(() -> new TrashException.ItemNotFoundException("Immatriculation", immatriculationId));
        
        // Vérifier si déjà dans la corbeille
        if (trashRepository.existsByOriginalIdAndType(
                immatriculationId.toString(), Trash.ItemType.IMMATRICULATION)) {
            throw new TrashException.ItemAlreadyInTrashException("Immatriculation", immatriculationId);
        }
        
        try {
            // Sérialiser les données
            String jsonData = objectMapper.writeValueAsString(immatriculation);
            
            // Créer l'entrée dans la corbeille
            Trash trash = new Trash(
                    immatriculationId.toString(),
                    Trash.ItemType.IMMATRICULATION,
                    jsonData,
                    deletedBy
            );
            
            // Sauvegarder dans la corbeille
            Trash savedTrash = trashRepository.save(trash);
            
            // Supprimer l'immatriculation originale
            immatriculationRepository.delete(immatriculation);
            
            log.info("Immatriculation {} déplacée vers la corbeille avec succès", immatriculationId);
            
            return convertToDto(savedTrash);
            
        } catch (Exception e) {
            log.error("Erreur lors du déplacement de l'immatriculation {} vers la corbeille", immatriculationId, e);
            throw new TrashException.TrashOperationException("Erreur lors du déplacement vers la corbeille", e);
        }
    }
    
    /**
     * Récupère tous les éléments de la corbeille
     */
    @Transactional(readOnly = true)
    public List<TrashDto> getAllTrashItems() {
        List<Trash> items = trashRepository.findAllByOrderByDeletedAtDesc();
        return items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère les éléments de la corbeille par type
     */
    @Transactional(readOnly = true)
    public List<TrashDto> getTrashItemsByType(Trash.ItemType type) {
        List<Trash> items = trashRepository.findByTypeOrderByDeletedAtDesc(type);
        return items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Restaure un élément depuis la corbeille
     */
    public TrashDto restoreFromTrash(Long trashId) {
        log.info("Restauration de l'élément {} depuis la corbeille", trashId);
        
        Trash trash = trashRepository.findById(trashId)
                .orElseThrow(() -> new TrashException.ItemNotFoundException("Trash", trashId));
        
        try {
            if (trash.getType() == Trash.ItemType.IMMATRICULATION) {
                // Désérialiser les données
                Immatriculation immatriculation = objectMapper.readValue(trash.getData(), Immatriculation.class);
                
                // Réinitialiser l'ID pour qu'il soit généré automatiquement
                immatriculation.setId(null);
                
                // Sauvegarder l'immatriculation restaurée
                Immatriculation restored = immatriculationRepository.save(immatriculation);
                
                log.info("Immatriculation {} restaurée avec succès", restored.getId());
            }
            
            // Supprimer l'entrée de la corbeille
            trashRepository.delete(trash);
            
            return convertToDto(trash);
            
        } catch (Exception e) {
            log.error("Erreur lors de la restauration de l'élément {}", trashId, e);
            throw new TrashException.TrashOperationException("Erreur lors de la restauration", e);
        }
    }
    
    /**
     * Supprime définitivement un élément de la corbeille
     */
    public void permanentDelete(Long trashId) {
        log.info("Suppression définitive de l'élément {} de la corbeille", trashId);
        
        Trash trash = trashRepository.findById(trashId)
                .orElseThrow(() -> new TrashException.ItemNotFoundException("Trash", trashId));
        
        trashRepository.delete(trash);
        log.info("Élément {} supprimé définitivement de la corbeille", trashId);
    }
    
    /**
     * Vide complètement la corbeille
     */
    public void emptyTrash() {
        log.info("Vidage complet de la corbeille");
        
        List<Trash> allItems = trashRepository.findAll();
        trashRepository.deleteAll(allItems);
        
        log.info("Corbeille vidée - {} éléments supprimés définitivement", allItems.size());
    }
    
    /**
     * Nettoie les éléments expirés (plus de 30 jours)
     */
    public int cleanExpiredItems() {
        log.info("Nettoyage des éléments expirés de la corbeille");
        
        LocalDateTime expiryDate = LocalDateTime.now().minusDays(30);
        List<Trash> expiredItems = trashRepository.findExpiredItems(expiryDate);
        
        if (!expiredItems.isEmpty()) {
            trashRepository.deleteAll(expiredItems);
            log.info("Nettoyage terminé - {} éléments expirés supprimés définitivement", expiredItems.size());
        } else {
            log.info("Aucun élément expiré à nettoyer");
        }
        
        return expiredItems.size();
    }
    
    /**
     * Récupère les statistiques de la corbeille
     */
    @Transactional(readOnly = true)
    public TrashDto.TrashStats getTrashStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningDate = now.minusDays(27); // 30 - 3 = 27 jours
        
        long totalItems = trashRepository.count();
        long expiredCount = trashRepository.countExpiredItems(now.minusDays(30));
        long expiringSoonCount = trashRepository.countItemsExpiringSoon(now.minusDays(30), warningDate);
        
        return new TrashDto.TrashStats(totalItems, expiringSoonCount, expiredCount);
    }
    
    /**
     * Récupère les éléments expirant bientôt
     */
    @Transactional(readOnly = true)
    public List<TrashDto> getItemsExpiringSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningDate = now.minusDays(27);
        
        List<Trash> items = trashRepository.findItemsExpiringSoon(now.minusDays(30), warningDate);
        return items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Convertit une entité Trash en DTO
     */
    private TrashDto convertToDto(Trash trash) {
        TrashDto dto = new TrashDto();
        dto.setId(trash.getId());
        dto.setOriginalId(trash.getOriginalId());
        dto.setType(trash.getType());
        dto.setDeletedAt(trash.getDeletedAt());
        dto.setDeletedBy(trash.getDeletedBy());
        dto.setDaysRemaining(trash.getDaysRemaining());
        dto.setExpired(trash.isExpired());
        dto.setExpiringSoon(trash.isExpiringSoon());
        
        // Extraire les données spécifiques selon le type
        try {
            if (trash.getType() == Trash.ItemType.IMMATRICULATION) {
                Immatriculation immatriculation = objectMapper.readValue(trash.getData(), Immatriculation.class);
                dto.setData(extractImmatriculationData(immatriculation));
            }
        } catch (Exception e) {
            log.warn("Erreur lors de l'extraction des données pour l'élément {}", trash.getId(), e);
            dto.setData(new TrashDto.TrashData());
        }
        
        return dto;
    }
    
    /**
     * Extrait les données pertinentes d'une immatriculation
     */
    private TrashDto.TrashData extractImmatriculationData(Immatriculation immatriculation) {
        TrashDto.TrashData data = new TrashDto.TrashData();
        data.setDossierNumber(immatriculation.getDossierNumber());
        data.setNomContribuable(getNomContribuable(immatriculation));
        data.setTypeContribuable(immatriculation.getTypeContribuable());
        data.setEmail(immatriculation.getEmail());
        data.setTelephone(immatriculation.getTelephone());
        data.setStatus(immatriculation.getStatus());
        return data;
    }
    
    /**
     * Extrait le nom du contribuable selon le type
     */
    private String getNomContribuable(Immatriculation immatriculation) {
        if (immatriculation.getTypeContribuable() == Immatriculation.TypeContribuable.PHYSIQUE) {
            return String.format("%s %s", 
                    immatriculation.getNom() != null ? immatriculation.getNom() : "",
                    immatriculation.getPrenom() != null ? immatriculation.getPrenom() : "").trim();
        } else {
            return immatriculation.getRaisonSociale() != null ? immatriculation.getRaisonSociale() : "";
        }
    }
}
