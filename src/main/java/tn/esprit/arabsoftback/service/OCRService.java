package tn.esprit.arabsoftback.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.dto.OCRResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.http.HttpClient;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OCRService {
    
    @Value("${ocr.service.url:http://localhost:8004}")
    private String ocrServiceUrl;
    
    private int timeoutSeconds;
    
    @Value("${ocr.service.enabled:true}")
    private boolean ocrEnabled;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();
    
    /**
     * Extrait les informations d'une CIN en utilisant le service OCR externe
     */
    public OCRResponse extractCINInformation(MultipartFile file) {
        try {
            log.info("🔍 Début extraction CIN - Service URL: {}", ocrServiceUrl);
            
            if (!ocrEnabled) {
                log.warn("⚠️ Service OCR désactivé - Retour réponse vide");
                return OCRResponse.success(createEmptyData(), "OCR Disabled", 0.0, null);
            }
            
            // Créer un fichier temporaire
            Path tempFile = createTempFile(file);
            
            try {
                // Appeler le service OCR
                OCRResponse response = callOCRService(tempFile.toFile());
                
                // Valider et corriger la réponse
                return validateAndEnhanceResponse(response);
                
            } finally {
                // Nettoyer le fichier temporaire
                cleanupTempFile(tempFile);
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur extraction CIN: {}", e.getMessage(), e);
            return OCRResponse.error("Erreur lors de l'extraction: " + e.getMessage());
        }
    }
    
    /**
     * Appelle le service OCR externe
     */
    private OCRResponse callOCRService(File imageFile) {
        try {
            log.info("📡 Appel du service OCR: {}", ocrServiceUrl + "/extract-cin");
            
            // Créer le multipart request avec RestTemplate
            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Ajouter le fichier
            FileSystemResource fileResource = new FileSystemResource(imageFile);
            body.add("file", fileResource);
            
            // Créer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Créer l'entité HTTP
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Envoyer la requête
            ResponseEntity<String> response = restTemplate.postForEntity(
                ocrServiceUrl + "/extract-cin", 
                requestEntity, 
                String.class
            );
            
            log.info("📨 Réponse OCR reçue - Status: {}", response.getStatusCode());
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Service OCR retourne status: " + response.getStatusCode());
            }
            
            // Parser la réponse JSON
            return parseOCRResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("❌ Erreur communication avec service OCR: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur communication avec service OCR", e);
        }
    }
    
    /**
     * Parse la réponse JSON du service OCR
     */
    private OCRResponse parseOCRResponse(String jsonResponse) {
        try {
            log.debug("📝 Réponse OCR brute: {}", jsonResponse);
            
            // Utiliser Jackson pour parser le JSON correctement
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            
            boolean success = (Boolean) responseMap.getOrDefault("success", false);
            Map<String, Object> data = (Map<String, Object>) responseMap.getOrDefault("data", new HashMap<>());
            String method = (String) responseMap.getOrDefault("method", "Unknown");
            double confidence = ((Number) responseMap.getOrDefault("confidence", 0.0)).doubleValue();
            String realText = (String) responseMap.get("real_text");
            
            log.info("✅ Parsing OCR réussi - Success: {}, Confidence: {}", success, confidence);
            
            return OCRResponse.builder()
                    .success(success)
                    .data(data)
                    .method(method)
                    .confidence(confidence)
                    .realText(realText)
                    .message(success ? "Extraction réussie" : "Échec de l'extraction")
                    .timestamp(System.currentTimeMillis())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Erreur parsing OCR response: {}", e.getMessage(), e);
            return OCRResponse.builder()
                    .success(false)
                    .data(createEmptyData())
                    .method("Error")
                    .confidence(0.0)
                    .realText(null)
                    .message("Erreur de parsing de la réponse OCR: " + e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }
    
    /**
     * Parse simple JSON en Map (évite dépendance Jackson)
     */
    private Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();
        
        // Enlever les accolades
        json = json.trim().substring(1, json.length() - 1).trim();
        
        // Parser les champs
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim();
                
                // Parser la valeur
                Object parsedValue = parseJsonValue(value);
                map.put(key, parsedValue);
            }
        }
        
        return map;
    }
    
    /**
     * Parse une valeur JSON
     */
    private Object parseJsonValue(String value) {
        value = value.trim();
        
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;
        if (value.equals("null")) return null;
        
        // Si c'est une chaîne
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        
        // Si c'est un nombre
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // Si c'est un objet imbriqué, retourner la chaîne brute
            return value;
        }
    }
    
    /**
     * Valide et améliore la réponse OCR
     */
    private OCRResponse validateAndEnhanceResponse(OCRResponse response) {
        if (!response.isSuccess()) {
            return response;
        }
        
        // Valider les données extraites
        Map<String, Object> validatedData = new HashMap<>(response.getData());
        
        // Validation et correction du CIN
        String cin = (String) validatedData.get("cin");
        if (cin != null) {
            cin = cin.replaceAll("[^0-9]", ""); // Garder seulement les chiffres
            if (cin.length() == 8) {
                validatedData.put("cin", cin);
            } else {
                validatedData.put("cin", null);
                log.warn("⚠️ CIN invalide après nettoyage: {}", cin);
            }
        }
        
        // Validation du nom
        String nom = (String) validatedData.get("nom");
        if (nom != null) {
            nom = nom.toUpperCase().replaceAll("[^A-Z\\s]", "").trim();
            validatedData.put("nom", nom.isEmpty() ? null : nom);
        }
        
        // Validation du prénom
        String prenom = (String) validatedData.get("prenom");
        if (prenom != null) {
            prenom = prenom.toUpperCase().replaceAll("[^A-Z\\s]", "").trim();
            validatedData.put("prenom", prenom.isEmpty() ? null : prenom);
        }
        
        // Validation de la date
        String date = (String) validatedData.get("date_naissance");
        if (date != null) {
            date = date.replaceAll("[^0-9/]", "");
            if (date.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                validatedData.put("date_naissance", date);
            } else {
                validatedData.put("date_naissance", null);
                log.warn("⚠️ Date invalide après nettoyage: {}", date);
            }
        }
        
        // Créer la réponse validée
        return OCRResponse.builder()
                .success(true)
                .data(validatedData)
                .method(response.getMethod())
                .confidence(response.getConfidence())
                .realText(response.getRealText())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * Valide et corrige une extraction manuelle
     */
    public OCRResponse validateAndCorrectExtraction(Map<String, Object> extractionData) {
        try {
            log.info("🔍 Validation extraction manuelle");
            
            // Appliquer les mêmes validations que pour l'OCR
            Map<String, Object> validatedData = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : extractionData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    String strValue = (String) value;
                    
                    switch (key) {
                        case "cin":
                            strValue = strValue.replaceAll("[^0-9]", "");
                            if (strValue.length() == 8) {
                                validatedData.put(key, strValue);
                            }
                            break;
                        case "nom":
                        case "prenom":
                            strValue = strValue.toUpperCase().replaceAll("[^A-Z\\s]", "").trim();
                            if (!strValue.isEmpty()) {
                                validatedData.put(key, strValue);
                            }
                            break;
                        case "date_naissance":
                        case "date_expiration":
                            strValue = strValue.replaceAll("[^0-9/]", "");
                            if (strValue.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                                validatedData.put(key, strValue);
                            }
                            break;
                        default:
                            validatedData.put(key, strValue);
                    }
                } else {
                    validatedData.put(key, value);
                }
            }
            
            return OCRResponse.success(validatedData, "Manual Validation", 1.0, null);
            
        } catch (Exception e) {
            log.error("❌ Erreur validation extraction: {}", e.getMessage(), e);
            return OCRResponse.error("Erreur validation: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie si le service OCR est disponible
     */
    public boolean isServiceAvailable() {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ocrServiceUrl + "/health"))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
            
            java.net.http.HttpResponse<String> response = httpClient.send(
                    request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            log.warn("⚠️ Service OCR indisponible: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Crée un fichier temporaire
     */
    private Path createTempFile(MultipartFile multipartFile) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".tmp";
        
        Path tempFile = Files.createTempFile("cin_ocr_", extension);
        
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            fos.write(multipartFile.getBytes());
        }
        
        log.debug("📁 Fichier temporaire créé: {}", tempFile);
        return tempFile;
    }
    
    /**
     * Nettoie le fichier temporaire
     */
    private void cleanupTempFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
            log.debug("🗑️ Fichier temporaire supprimé: {}", tempFile);
        } catch (IOException e) {
            log.warn("⚠️ Impossible de supprimer le fichier temporaire: {}", tempFile, e);
        }
    }
    
    /**
     * Détermine le content type selon l'extension
     */
    private String getContentType(String filename) {
        String extension = filename.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (extension.endsWith(".png")) {
            return "image/png";
        } else if (extension.endsWith(".bmp")) {
            return "image/bmp";
        } else {
            return "application/octet-stream";
        }
    }
    
    /**
     * Crée des données vides pour le fallback
     */
    private Map<String, Object> createEmptyData() {
        Map<String, Object> data = new HashMap<>();
        data.put("cin", null);
        data.put("nom", null);
        data.put("prenom", null);
        data.put("date_naissance", null);
        data.put("lieu_naissance", null);
        data.put("sexe", null);
        data.put("date_expiration", null);
        data.put("nationalite", "Tunisienne");
        return data;
    }
}
