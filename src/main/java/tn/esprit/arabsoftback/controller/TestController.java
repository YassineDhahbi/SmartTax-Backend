package tn.esprit.arabsoftback.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Backend SmartTax is running");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/immatriculation-ready")
    public ResponseEntity<Map<String, Object>> immatriculationReady() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "READY");
        response.put("message", "Immatriculation API is ready");
        response.put("endpoints", Map.of(
                "POST /api/immatriculation/create", "Créer un dossier",
                "GET /api/immatriculation/{id}", "Récupérer un dossier",
                "POST /api/immatriculation/{id}/submit", "Soumettre un dossier",
                "GET /api/immatriculation/search", "Rechercher des dossiers",
                "GET /api/immatriculation/statistics", "Statistiques"
        ));
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
