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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.dto.OCRResponse;
import tn.esprit.arabsoftback.service.OCRService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Service OCR", description = "API pour l'extraction d'informations des CIN via OCR")
@CrossOrigin(origins = "http://localhost:4200")
public class OCRController {
    
    private final OCRService ocrService;
    
    @PostMapping("/extract-cin")
    @Operation(summary = "Extraire les informations d'une CIN", 
               description = "Extrait automatiquement les informations d'une carte d'identité tunisienne")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Extraction réussie",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = OCRResponse.class))),
        @ApiResponse(responseCode = "400", description = "Fichier invalide"),
        @ApiResponse(responseCode = "500", description = "Erreur lors de l'extraction")
    })
    public ResponseEntity<OCRResponse> extractCINInfo(
            @Parameter(description = "Image de la CIN") @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("🔍 Début extraction CIN - Fichier: {}", file.getOriginalFilename());
            
            // Validation du fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(OCRResponse.error("Le fichier est vide"));
            }
            
            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(OCRResponse.error("Le fichier doit être une image"));
            }
            
            if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                return ResponseEntity.badRequest()
                        .body(OCRResponse.error("Le fichier est trop volumineux (max 10MB)"));
            }
            
            // Extraction des informations
            OCRResponse response = ocrService.extractCINInformation(file);
            
            log.info("✅ Extraction CIN terminée - Succès: {}, Confiance: {}", 
                    response.isSuccess(), response.getConfidence());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'extraction CIN: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(OCRResponse.error("Erreur technique: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Vérifier l'état du service OCR", 
               description = "Vérifie si le service OCR est fonctionnel")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "OCR CIN Extractor");
        response.put("version", "1.0.0");
        response.put("supportedFormats", new String[]{"jpg", "jpeg", "png", "bmp"});
        response.put("maxFileSize", "10MB");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test")
    @Operation(summary = "Tester le service OCR", 
               description = "Teste la connexion avec le service OCR externe")
    public ResponseEntity<Map<String, Object>> testOCRService() {
        try {
            boolean isAvailable = ocrService.isServiceAvailable();
            Map<String, Object> response = new HashMap<>();
            response.put("available", isAvailable);
            response.put("message", isAvailable ? 
                    "Service OCR disponible et fonctionnel" : 
                    "Service OCR indisponible");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur test OCR: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("available", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/validate-extraction")
    @Operation(summary = "Valider et corriger l'extraction", 
               description = "Permet de valider ou corriger les informations extraites")
    public ResponseEntity<OCRResponse> validateExtraction(
            @RequestBody Map<String, Object> extractionData) {
        
        try {
            log.info("🔍 Validation extraction CIN");
            
            OCRResponse validated = ocrService.validateAndCorrectExtraction(extractionData);
            
            log.info("✅ Validation extraction terminée - Succès: {}", validated.isSuccess());
            
            return ResponseEntity.ok(validated);
            
        } catch (Exception e) {
            log.error("❌ Erreur validation extraction: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(OCRResponse.error("Erreur validation: " + e.getMessage()));
        }
    }
}
