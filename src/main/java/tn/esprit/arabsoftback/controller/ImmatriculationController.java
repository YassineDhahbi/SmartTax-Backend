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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.dto.ImmatriculationDto;
import tn.esprit.arabsoftback.dto.ImmatriculationDto.UpdateImmatriculationDto;
import tn.esprit.arabsoftback.entity.Immatriculation;
import tn.esprit.arabsoftback.mapper.ImmatriculationMapper;
import tn.esprit.arabsoftback.repository.ImmatriculationRepository;
import tn.esprit.arabsoftback.service.ImmatriculationService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/immatriculation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion de l'Immatriculation", description = "API pour la gestion des dossiers d'immatriculation fiscale")
@CrossOrigin(origins = "http://localhost:4200")
public class ImmatriculationController {
    
    private final ImmatriculationService immatriculationService;
    private final ImmatriculationMapper immatriculationMapper;
    private final ImmatriculationRepository immatriculationRepository;
    private final tn.esprit.arabsoftback.service.TrashService trashService;
    private final tn.esprit.arabsoftback.service.OCRService ocrService;
    
    @PostMapping("/extract-cin")
    @Operation(summary = "Extraire les informations d'une CIN", 
               description = "Extrait automatiquement les informations d'une carte d'identité tunisienne")
    public ResponseEntity<tn.esprit.arabsoftback.dto.OCRResponse> extractCINInfo(
            @Parameter(description = "Image de la CIN") @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("🔍 Extraction CIN pour immatriculation - Fichier: {}", file.getOriginalFilename());
            
            tn.esprit.arabsoftback.dto.OCRResponse response = ocrService.extractCINInformation(file);
            
            log.info("✅ Extraction CIN terminée - Succès: {}, Confiance: {}", 
                    response.isSuccess(), response.getConfidence());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur extraction CIN: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(tn.esprit.arabsoftback.dto.OCRResponse.error("Erreur extraction: " + e.getMessage()));
        }
    }
    
    @PostMapping("/check-duplicates")
    @Operation(summary = "Vérifier les doublons", 
               description = "Vérifie si un CIN, email ou registre de commerce existe déjà")
    public ResponseEntity<Map<String, Boolean>> checkDuplicates(
            @Parameter(description = "CIN à vérifier") @RequestParam(required = false) String cin,
            @Parameter(description = "Email à vérifier") @RequestParam(required = false) String email,
            @Parameter(description = "Registre de commerce à vérifier") @RequestParam(required = false) String registreCommerce) {
        
        Map<String, Boolean> result = new HashMap<>();
        
        if (cin != null && !cin.trim().isEmpty()) {
            result.put("cinExists", immatriculationRepository.existsByCin(cin));
        }
        
        if (email != null && !email.trim().isEmpty()) {
            result.put("emailExists", immatriculationRepository.existsByEmail(email));
        }
        
        if (registreCommerce != null && !registreCommerce.trim().isEmpty()) {
            result.put("registreCommerceExists", immatriculationRepository.existsByRegistreCommerce(registreCommerce));
        }
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    @Operation(summary = "Créer un nouveau dossier d'immatriculation", 
               description = "Crée un nouveau dossier d'immatriculation avec les informations fournies")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Dossier créé avec succès",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ImmatriculationDto.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "409", description = "Doublon détecté")
    })
    public ResponseEntity<ImmatriculationDto> createImmatriculation(
            @RequestBody ImmatriculationDto.CreateImmatriculationDto dto) {
        
        log.info("Création d'un nouveau dossier d'immatriculation pour: {}", dto.getEmail());
        
        Immatriculation entity = immatriculationMapper.toEntity(dto);
        Immatriculation saved = immatriculationService.createImmatriculation(entity);
        ImmatriculationDto response = immatriculationMapper.toDto(saved);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/create-with-files")
    @Operation(summary = "Créer un dossier avec fichiers", 
               description = "Crée un nouveau dossier avec téléversement de fichiers")
    public ResponseEntity<ImmatriculationDto> createImmatriculationWithFiles(
            @RequestPart("data") @Valid ImmatriculationDto.CreateImmatriculationDto dto,
            @RequestPart(value = "identiteFile", required = false) MultipartFile identiteFile,
            @RequestPart(value = "activiteFile", required = false) MultipartFile activiteFile,
            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile,
            @RequestPart(value = "autresFiles", required = false) MultipartFile[] autresFiles) {
        
        try {
            log.info("Création d'un dossier avec fichiers pour: {}", dto.getEmail());
            
            // Convertir les fichiers en Base64
            if (identiteFile != null) {
                dto.setIdentiteFile(immatriculationService.convertFileToBase64(identiteFile));
            }
            if (activiteFile != null) {
                dto.setActiviteFile(immatriculationService.convertFileToBase64(activiteFile));
            }
            if (photoFile != null) {
                dto.setPhotoFile(immatriculationService.convertFileToBase64(photoFile));
            }
            if (autresFiles != null) {
                List<String> autresFilesBase64 = List.of(autresFiles).stream()
                        .map(file -> {
                            try {
                                return immatriculationService.convertFileToBase64(file);
                            } catch (Exception e) {
                                log.error("Erreur lors de la conversion du fichier: {}", file.getOriginalFilename(), e);
                                return null;
                            }
                        })
                        .filter(file -> file != null)
                        .collect(Collectors.toList());
                dto.setAutresFiles(autresFilesBase64);
            }
            
            Immatriculation entity = immatriculationMapper.toEntity(dto);
            Immatriculation saved = immatriculationService.createImmatriculation(entity);
            ImmatriculationDto response = immatriculationMapper.toDto(saved);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création du dossier avec fichiers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un dossier", 
               description = "Met à jour les informations d'un dossier existant")
    public ResponseEntity<ImmatriculationDto> updateImmatriculation(
            @Parameter(description = "ID du dossier") @PathVariable Long id,
            @Valid @RequestBody UpdateImmatriculationDto dto) {
        
        log.info("Mise à jour du dossier d'immatriculation ID: {}", id);
        log.info("DTO reçu: {}", dto);
        
        Immatriculation existing = immatriculationService.getImmatriculationById(id);
        log.info("Statut actuel du dossier: {}", existing.getStatus());
        log.info("Statut demandé dans DTO: {}", dto.getStatus());
        
        immatriculationMapper.updateEntity(existing, dto);
        log.info("Statut après updateEntity: {}", existing.getStatus());
        
        Immatriculation updated = immatriculationService.updateImmatriculation(id, existing);
        log.info("Statut final après sauvegarde: {}", updated.getStatus());
        
        ImmatriculationDto response = immatriculationMapper.toDto(updated);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un dossier", 
               description = "Récupère les détails d'un dossier par son ID")
    public ResponseEntity<ImmatriculationDto> getImmatriculation(
            @Parameter(description = "ID du dossier") @PathVariable Long id) {
        
        Immatriculation entity = immatriculationService.getImmatriculationById(id);
        ImmatriculationDto response = immatriculationMapper.toDto(entity);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{dossierNumber}")
    @Operation(summary = "Récupérer un dossier par numéro", 
               description = "Récupère les détails d'un dossier par son numéro")
    public ResponseEntity<ImmatriculationDto> getImmatriculationByNumber(
            @Parameter(description = "Numéro du dossier") @PathVariable String dossierNumber) {
        
        return immatriculationService.getImmatriculationByNumber(dossierNumber)
                .map(entity -> {
                    ImmatriculationDto response = immatriculationMapper.toDto(entity);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @Operation(summary = "Liste de tous les dossiers", 
               description = "Récupère la liste de tous les dossiers d'immatriculation")
    public ResponseEntity<List<ImmatriculationDto>> getAllImmatriculations() {
        
        List<Immatriculation> entities = immatriculationService.getAllImmatriculations();
        List<ImmatriculationDto> response = immatriculationMapper.toDtoList(entities);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Liste des dossiers par statut", 
               description = "Récupère la liste des dossiers selon leur statut")
    public ResponseEntity<List<ImmatriculationDto>> getImmatriculationsByStatus(
            @Parameter(description = "Statut du dossier") @PathVariable Immatriculation.DossierStatus status) {
        
        List<Immatriculation> entities = immatriculationService.getImmatriculationsByStatus(status);
        List<ImmatriculationDto> response = immatriculationMapper.toDtoList(entities);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Rechercher des dossiers", 
               description = "Recherche des dossiers selon plusieurs critères")
    public ResponseEntity<List<ImmatriculationDto>> searchImmatriculations(
            @Parameter(description = "Nom") @RequestParam(required = false) String nom,
            @Parameter(description = "Prénom") @RequestParam(required = false) String prenom,
            @Parameter(description = "Email") @RequestParam(required = false) String email,
            @Parameter(description = "CIN") @RequestParam(required = false) String cin,
            @Parameter(description = "Statut") @RequestParam(required = false) Immatriculation.DossierStatus status,
            @Parameter(description = "Type de contribuable") @RequestParam(required = false) Immatriculation.TypeContribuable typeContribuable) {
        
        List<Immatriculation> entities = immatriculationService.searchDossiers(
                nom, prenom, email, cin, status, typeContribuable);
        List<ImmatriculationDto> response = immatriculationMapper.toDtoList(entities);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-dossier")
    @Operation(summary = "Dossier du contribuable connecte",
               description = "Recupere le(s) dossier(s) par email exact ou TIN (matricule fiscal)")
    public ResponseEntity<List<ImmatriculationDto>> getMyDossier(
            @Parameter(description = "Email du compte") @RequestParam(required = false) String email,
            @Parameter(description = "TIN / matricule fiscal") @RequestParam(required = false) String tin) {

        if ((email == null || email.trim().isEmpty()) && (tin == null || tin.trim().isEmpty())) {
            return ResponseEntity.badRequest().build();
        }

        List<Immatriculation> entities = immatriculationService.getDossiersForContribuable(email, tin);
        List<ImmatriculationDto> response = immatriculationMapper.toDtoList(entities);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paginated")
    @Operation(summary = "Liste paginée des dossiers", 
               description = "Récupère la liste des dossiers avec pagination")
    public ResponseEntity<Page<ImmatriculationDto>> getImmatriculationsPaginated(
            @Parameter(description = "Page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "dateCreation") String sortBy,
            @Parameter(description = "Direction de tri") @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // Note: Cette méthode nécessite l'ajout de la pagination dans le service et repository
        // Pour l'instant, retourner une liste complète
        List<Immatriculation> entities = immatriculationService.getAllImmatriculations();
        List<ImmatriculationDto> dtos = immatriculationMapper.toDtoList(entities);
        
        // Simulation de pagination (à remplacer par vraie pagination)
        int start = Math.min(page * size, dtos.size());
        int end = Math.min(start + size, dtos.size());
        List<ImmatriculationDto> pageContent = dtos.subList(start, end);
        
        // Créer une page manuelle (à remplacer par PageImpl du repository)
        Page<ImmatriculationDto> response = new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, dtos.size());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre un dossier", 
               description = "Soumet un dossier pour validation")
    public ResponseEntity<ImmatriculationDto> submitDossier(
            @Parameter(description = "ID du dossier") @PathVariable Long id) {
        
        log.info("Soumission du dossier d'immatriculation ID: {}", id);
        
        Immatriculation submitted = immatriculationService.submitDossier(id);
        ImmatriculationDto response = immatriculationMapper.toDto(submitted);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/validate")
    @Operation(summary = "Valider un dossier", 
               description = "Valide un dossier et génère le matricule fiscal")
    public ResponseEntity<ImmatriculationDto> validateDossier(
            @Parameter(description = "ID du dossier") @PathVariable Long id) {
        
        log.info("Validation du dossier d'immatriculation ID: {}", id);
        
        Immatriculation validated = immatriculationService.validateDossier(id);
        ImmatriculationDto response = immatriculationMapper.toDto(validated);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeter un dossier", 
               description = "Rejette un dossier avec un motif")
    public ResponseEntity<ImmatriculationDto> rejectDossier(
            @Parameter(description = "ID du dossier") @PathVariable Long id,
            @Valid @RequestBody ImmatriculationDto.ValidationDto validationDto) {
        
        log.info("Rejet du dossier d'immatriculation ID: {} - Motif: {}", id, validationDto.getMotifRejet());
        
        Immatriculation rejected = immatriculationService.rejectDossier(id, validationDto.getMotifRejet());
        ImmatriculationDto response = immatriculationMapper.toDto(rejected);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Déplacer vers la corbeille", 
               description = "Déplace une immatriculation vers la corbeille (conservation 30 jours)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Dossier déplacé vers la corbeille",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = tn.esprit.arabsoftback.dto.TrashDto.class))),
        @ApiResponse(responseCode = "404", description = "Dossier non trouvé"),
        @ApiResponse(responseCode = "409", description = "Dossier déjà dans la corbeille")
    })
    public ResponseEntity<tn.esprit.arabsoftback.dto.TrashDto> deleteImmatriculation(
            @Parameter(description = "ID du dossier") @PathVariable Long id,
            @Parameter(description = "Utilisateur qui supprime") @RequestParam(defaultValue = "system") String deletedBy) {
        
        log.info("Déplacement du dossier d'immatriculation {} vers la corbeille par {}", id, deletedBy);
        
        try {
            tn.esprit.arabsoftback.dto.TrashDto trashItem = trashService.moveImmatriculationToTrash(id, deletedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(trashItem);
        } catch (Exception e) {
            log.error("Erreur lors du déplacement du dossier ID: {} vers la corbeille", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}/archive")
    @Operation(summary = "Archiver un dossier", 
               description = "Archive un dossier (suppression logique)")
    public ResponseEntity<Void> archiveDossier(
            @Parameter(description = "ID du dossier") @PathVariable Long id) {
        
        log.info("Archivage du dossier d'immatriculation ID: {}", id);
        
        immatriculationService.archiveDossier(id);
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques des dossiers", 
               description = "Récupère les statistiques des dossiers par statut")
    public ResponseEntity<List<ImmatriculationDto.StatistiqueDto>> getStatistics() {
        
        List<Object[]> stats = immatriculationService.getStatisticsByStatus();
        List<ImmatriculationDto.StatistiqueDto> response = stats.stream()
                .map(stat -> {
                    ImmatriculationDto.StatistiqueDto dto = new ImmatriculationDto.StatistiqueDto();
                    dto.setStatus((String) stat[0]);
                    dto.setCount((Long) stat[1]);
                    return dto;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/dashboard")
    @Operation(summary = "Tableau de bord", 
               description = "Récupère les données pour le tableau de bord")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        
        Map<String, Object> dashboard = new HashMap<>();
        
        // Statistiques par statut
        List<Object[]> stats = immatriculationService.getStatisticsByStatus();
        Map<String, Long> statsMap = new HashMap<>();
        long total = 0;
        
        for (Object[] stat : stats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];
            statsMap.put(status, count);
            total += count;
        }
        
        dashboard.put("statistics", statsMap);
        dashboard.put("total", total);
        
        // Dossiers récents
        List<Immatriculation> recentDossiers = immatriculationService
                .getImmatriculationsByStatus(Immatriculation.DossierStatus.SOUMIS);
        List<ImmatriculationDto.DossierSummaryDto> recentSummaries = 
                immatriculationMapper.toSummaryDtoList(recentDossiers.subList(0, 
                        Math.min(5, recentDossiers.size())));
        dashboard.put("recentDossiers", recentSummaries);
        
        // Dossiers à faible score
        List<Immatriculation> lowScoreDossiers = immatriculationService.getLowScoreDossiers();
        List<ImmatriculationDto.DossierSummaryDto> lowScoreSummaries = 
                immatriculationMapper.toSummaryDtoList(lowScoreDossiers.subList(0, 
                        Math.min(5, lowScoreDossiers.size())));
        dashboard.put("lowScoreDossiers", lowScoreSummaries);
        
        // Dossiers du jour
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayCount = immatriculationService.countSinceDate(today);
        dashboard.put("todayCount", todayCount);
        
        return ResponseEntity.ok(dashboard);
    }
    
    @GetMapping("/{id}/download/{fileType}")
    @Operation(summary = "Télécharger un fichier", 
               description = "Télécharge un fichier spécifique du dossier")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @Parameter(description = "ID du dossier") @PathVariable Long id,
            @Parameter(description = "Type de fichier") @PathVariable String fileType) {
        
        try {
            Immatriculation entity = immatriculationService.getImmatriculationById(id);
            String fileData = null;
            String fileName = "";
            String contentType = "";
            
            switch (fileType.toLowerCase()) {
                case "identite":
                    fileData = entity.getIdentiteFile();
                    fileName = "identite_" + entity.getDossierNumber();
                    contentType = "application/pdf";
                    break;
                case "activite":
                    fileData = entity.getActiviteFile();
                    fileName = "activite_" + entity.getDossierNumber();
                    contentType = "application/pdf";
                    break;
                case "photo":
                    fileData = entity.getPhotoFile();
                    fileName = "photo_" + entity.getDossierNumber();
                    contentType = "image/jpeg";
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            if (fileData == null || fileData.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] bytes = java.util.Base64.getDecoder().decode(fileData);
            ByteArrayResource resource = new ByteArrayResource(bytes);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(bytes.length)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du fichier", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/export")
    @Operation(summary = "Exporter les dossiers", 
               description = "Exporte la liste des dossiers en Excel ou CSV")
    public ResponseEntity<ByteArrayResource> exportDossiers(
            @Parameter(description = "Format d'export") @RequestParam(defaultValue = "excel") String format,
            @Parameter(description = "Statut") @RequestParam(required = false) Immatriculation.DossierStatus status) {
        
        try {
            // TODO: Implémenter l'exportation Excel/CSV
            // Pour l'instant, retourner une réponse vide
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Erreur lors de l'exportation des dossiers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
