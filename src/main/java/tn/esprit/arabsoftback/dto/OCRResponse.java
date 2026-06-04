package tn.esprit.arabsoftback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OCRResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("data")
    private Map<String, Object> data;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("confidence")
    private double confidence;
    
    @JsonProperty("real_text")
    private String realText;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    // Constructeur pour les réponses d'erreur
    public OCRResponse(String message) {
        this.success = false;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Constructeur pour les réponses de succès
    public OCRResponse(boolean success, Map<String, Object> data, String method, double confidence, String realText) {
        this.success = success;
        this.data = data;
        this.method = method;
        this.confidence = confidence;
        this.realText = realText;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Méthode statique pour créer une réponse d'erreur
    public static OCRResponse error(String message) {
        return new OCRResponse(message);
    }
    
    // Méthode statique pour créer une réponse de succès
    public static OCRResponse success(Map<String, Object> data, String method, double confidence, String realText) {
        return new OCRResponse(true, data, method, confidence, realText);
    }
    
    // Getters pour les champs spécifiques de la CIN
    @JsonProperty("cin")
    public String getCin() {
        return data != null ? (String) data.get("cin") : null;
    }
    
    @JsonProperty("nom")
    public String getNom() {
        return data != null ? (String) data.get("nom") : null;
    }
    
    @JsonProperty("prenom")
    public String getPrenom() {
        return data != null ? (String) data.get("prenom") : null;
    }
    
    @JsonProperty("date_naissance")
    public String getDateNaissance() {
        return data != null ? (String) data.get("date_naissance") : null;
    }
    
    @JsonProperty("lieu_naissance")
    public String getLieuNaissance() {
        return data != null ? (String) data.get("lieu_naissance") : null;
    }
    
    @JsonProperty("sexe")
    public String getSexe() {
        return data != null ? (String) data.get("sexe") : null;
    }
    
    @JsonProperty("date_expiration")
    public String getDateExpiration() {
        return data != null ? (String) data.get("date_expiration") : null;
    }
    
    @JsonProperty("nationalite")
    public String getNationalite() {
        return data != null ? (String) data.get("nationalite") : null;
    }
    
    // Validation des données extraites
    public boolean hasValidCIN() {
        String cin = getCin();
        return cin != null && cin.matches("^\\d{8}$");
    }
    
    public boolean hasValidName() {
        String nom = getNom();
        String prenom = getPrenom();
        return (nom != null && !nom.trim().isEmpty()) || 
               (prenom != null && !prenom.trim().isEmpty());
    }
    
    public boolean hasValidDate() {
        String date = getDateNaissance();
        return date != null && date.matches("^\\d{2}/\\d{2}/\\d{4}$");
    }
    
    // Score de qualité global
    public int getQualityScore() {
        int score = 0;
        if (hasValidCIN()) score += 30;
        if (hasValidName()) score += 30;
        if (hasValidDate()) score += 25;
        if (getLieuNaissance() != null) score += 10;
        if (getSexe() != null) score += 5;
        return score;
    }
    
    // Message de qualité
    public String getQualityMessage() {
        int score = getQualityScore();
        if (score >= 90) return "Excellente qualité";
        if (score >= 70) return "Bonne qualité";
        if (score >= 50) return "Qualité moyenne";
        if (score >= 30) return "Qualité faible";
        return "Très faible qualité";
    }
}
