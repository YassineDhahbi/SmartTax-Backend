package tn.esprit.arabsoftback.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.arabsoftback.service.SMSService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/sms")
@CrossOrigin(origins = "http://localhost:4200")
public class SMSController {
    
    private final SMSService smsService;
    
    public SMSController(SMSService smsService) {
        this.smsService = smsService;
    }
    
    /**
     * Envoyer un SMS de vérification
     */
    @PostMapping("/send-verification")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendVerificationSMS(
            @RequestParam String phoneNumber,
            @RequestParam String verificationCode) {
        
        try {
            System.out.println("SMS Controller - Envoi vers: " + phoneNumber);
            System.out.println("SMS Controller - Code: " + verificationCode);
            
            // Valider le numéro de téléphone
            if (!smsService.isValidPhoneNumber(phoneNumber)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Numéro de téléphone invalide");
                response.put("smsSent", false);
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(response));
            }
            
            // Formater le numéro
            String formattedNumber = smsService.formatPhoneNumber(phoneNumber);
            System.out.println("SMS Controller - Numéro formaté: " + formattedNumber);
            
            // Envoyer le SMS
            return smsService.sendVerificationSMS(formattedNumber, verificationCode)
                    .thenApply(success -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", success);
                        response.put("message", success ? 
                                "SMS envoyé avec succès à " + formattedNumber : 
                                "Échec de l'envoi du SMS");
                        response.put("smsSent", success);
                        response.put("phoneNumber", formattedNumber);
                        
                        return success ? ResponseEntity.ok(response) : 
                                ResponseEntity.status(500).body(response);
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Erreur SMS Controller: " + throwable.getMessage());
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Erreur technique: " + throwable.getMessage());
                        response.put("smsSent", false);
                        return ResponseEntity.status(500).body(response);
                    });
                    
        } catch (Exception e) {
            System.err.println("Erreur SMS Controller: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur technique: " + e.getMessage());
            response.put("smsSent", false);
            
            return CompletableFuture.completedFuture(ResponseEntity.status(500).body(response));
        }
    }
    
    /**
     * Tester l'envoi de SMS
     */
    @PostMapping("/test-send")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> testSendSMS(@RequestParam String phoneNumber) {
        try {
            System.out.println("SMS Controller - Test d'envoi vers: " + phoneNumber);
            
            String testCode = "TEST-123";
            
            return smsService.sendVerificationSMS(phoneNumber, testCode)
                    .thenApply(success -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", success);
                        response.put("message", success ? 
                                "Test SMS envoyé avec succès à " + phoneNumber : 
                                "Échec du test SMS");
                        response.put("smsSent", success);
                        response.put("testCode", testCode);
                        
                        return ResponseEntity.ok(response);
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Erreur test SMS: " + throwable.getMessage());
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Erreur test: " + throwable.getMessage());
                        response.put("smsSent", false);
                        return ResponseEntity.status(500).body(response);
                    });
                    
        } catch (Exception e) {
            System.err.println("Erreur test SMS: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur test: " + e.getMessage());
            response.put("smsSent", false);
            
            return CompletableFuture.completedFuture(ResponseEntity.status(500).body(response));
        }
    }
    
    /**
     * Valider un numéro de téléphone
     */
    @PostMapping("/validate-phone")
    public ResponseEntity<Map<String, Object>> validatePhoneNumber(@RequestParam String phoneNumber) {
        try {
            boolean isValid = smsService.isValidPhoneNumber(phoneNumber);
            String formatted = isValid ? smsService.formatPhoneNumber(phoneNumber) : null;
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("formatted", formatted);
            response.put("original", phoneNumber);
            response.put("message", isValid ? "Numéro valide" : "Numéro invalide");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Erreur validation téléphone: " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("formatted", null);
            response.put("message", "Erreur validation: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Tester la configuration SMS
     */
    @GetMapping("/test")
    public ResponseEntity<String> testSMSConfiguration() {
        try {
            String testResult = smsService.testSMSConfiguration();
            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur de test: " + e.getMessage());
        }
    }
}
