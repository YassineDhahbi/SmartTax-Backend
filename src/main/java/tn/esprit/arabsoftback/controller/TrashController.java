package tn.esprit.arabsoftback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.arabsoftback.dto.TrashDto;
import tn.esprit.arabsoftback.entity.Trash;
import tn.esprit.arabsoftback.service.TrashService;

import java.util.List;

@RestController
@RequestMapping("/api/trash")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion de la Corbeille", description = "API pour la gestion de la corbeille des dossiers")
@CrossOrigin(origins = "http://localhost:4200")
public class TrashController {
    
    private final TrashService trashService;
    
    @PostMapping("/move")
    @Operation(summary = "Déplacer vers la corbeille", 
               description = "Déplace une immatriculation vers la corbeille")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Élément déplacé vers la corbeille",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TrashDto.class))),
        @ApiResponse(responseCode = "404", description = "Immatriculation non trouvée"),
        @ApiResponse(responseCode = "409", description = "Élément déjà dans la corbeille")
    })
    public ResponseEntity<TrashDto> moveToTrash(
            @Parameter(description = "ID de l'immatriculation") @RequestParam Long immatriculationId,
            @Parameter(description = "Utilisateur qui supprime") @RequestParam String deletedBy) {
        
        log.info("Déplacement de l'immatriculation {} vers la corbeille par {}", immatriculationId, deletedBy);
        
        TrashDto result = trashService.moveImmatriculationToTrash(immatriculationId, deletedBy);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @GetMapping("/items")
    @Operation(summary = "Lister les éléments de la corbeille", 
               description = "Récupère tous les éléments présents dans la corbeille")
    public ResponseEntity<List<TrashDto>> getTrashItems() {
        List<TrashDto> items = trashService.getAllTrashItems();
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/items/type/{type}")
    @Operation(summary = "Lister par type", 
               description = "Récupère les éléments de la corbeille par type")
    public ResponseEntity<List<TrashDto>> getTrashItemsByType(
            @Parameter(description = "Type d'élément") @PathVariable Trash.ItemType type) {
        List<TrashDto> items = trashService.getTrashItemsByType(type);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Statistiques de la corbeille", 
               description = "Récupère les statistiques des éléments dans la corbeille")
    public ResponseEntity<TrashDto.TrashStats> getTrashStats() {
        TrashDto.TrashStats stats = trashService.getTrashStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/items/expiring-soon")
    @Operation(summary = "Éléments expirant bientôt", 
               description = "Récupère les éléments qui expireront dans les 3 prochains jours")
    public ResponseEntity<List<TrashDto>> getItemsExpiringSoon() {
        List<TrashDto> items = trashService.getItemsExpiringSoon();
        return ResponseEntity.ok(items);
    }
    
    @PostMapping("/restore/{trashId}")
    @Operation(summary = "Restaurer un élément", 
               description = "Restaure un élément depuis la corbeille")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Élément restauré avec succès"),
        @ApiResponse(responseCode = "404", description = "Élément non trouvé dans la corbeille"),
        @ApiResponse(responseCode = "400", description = "Impossible de restaurer l'élément")
    })
    public ResponseEntity<TrashDto> restoreFromTrash(
            @Parameter(description = "ID de l'élément dans la corbeille") @PathVariable Long trashId) {
        
        log.info("Restauration de l'élément {} depuis la corbeille", trashId);
        
        TrashDto result = trashService.restoreFromTrash(trashId);
        
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/permanent/{trashId}")
    @Operation(summary = "Suppression définitive", 
               description = "Supprime définitivement un élément de la corbeille")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Élément supprimé définitivement"),
        @ApiResponse(responseCode = "404", description = "Élément non trouvé")
    })
    public ResponseEntity<Void> permanentDelete(
            @Parameter(description = "ID de l'élément dans la corbeille") @PathVariable Long trashId) {
        
        log.info("Suppression définitive de l'élément {} de la corbeille", trashId);
        
        trashService.permanentDelete(trashId);
        
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/empty")
    @Operation(summary = "Vider la corbeille", 
               description = "Supprime définitivement tous les éléments de la corbeille")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Corbeille vidée avec succès")
    })
    public ResponseEntity<Void> emptyTrash() {
        log.info("Vidage complet de la corbeille");
        
        trashService.emptyTrash();
        
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/clean-expired")
    @Operation(summary = "Nettoyer les éléments expirés", 
               description = "Supprime définitivement les éléments de plus de 30 jours")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nettoyage effectué",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Integer.class)))
    })
    public ResponseEntity<Integer> cleanExpiredItems() {
        log.info("Nettoyage des éléments expirés de la corbeille");
        
        int cleanedCount = trashService.cleanExpiredItems();
        
        return ResponseEntity.ok(cleanedCount);
    }
    
    @PostMapping("/restore-batch")
    @Operation(summary = "Restauration multiple", 
               description = "Restaure plusieurs éléments en même temps")
    public ResponseEntity<String> restoreBatch(
            @Parameter(description = "Liste des IDs à restaurer") @RequestBody List<Long> trashIds) {
        
        log.info("Restauration batch de {} éléments", trashIds.size());
        
        int successCount = 0;
        int errorCount = 0;
        
        for (Long trashId : trashIds) {
            try {
                trashService.restoreFromTrash(trashId);
                successCount++;
            } catch (Exception e) {
                log.error("Erreur lors de la restauration de l'élément {}", trashId, e);
                errorCount++;
            }
        }
        
        String message = String.format("Restauration terminée: %d succès, %d erreurs", successCount, errorCount);
        
        return ResponseEntity.ok(message);
    }
    
    @DeleteMapping("/permanent-batch")
    @Operation(summary = "Suppression définitive multiple", 
               description = "Supprime définitivement plusieurs éléments en même temps")
    public ResponseEntity<String> permanentDeleteBatch(
            @Parameter(description = "Liste des IDs à supprimer") @RequestBody List<Long> trashIds) {
        
        log.info("Suppression définitive batch de {} éléments", trashIds.size());
        
        int successCount = 0;
        int errorCount = 0;
        
        for (Long trashId : trashIds) {
            try {
                trashService.permanentDelete(trashId);
                successCount++;
            } catch (Exception e) {
                log.error("Erreur lors de la suppression définitive de l'élément {}", trashId, e);
                errorCount++;
            }
        }
        
        String message = String.format("Suppression terminée: %d succès, %d erreurs", successCount, errorCount);
        
        return ResponseEntity.ok(message);
    }
}
