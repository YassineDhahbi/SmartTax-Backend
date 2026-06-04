package tn.esprit.arabsoftback.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.arabsoftback.dto.EmailRequest;
import tn.esprit.arabsoftback.dto.EmailResponse;
import tn.esprit.arabsoftback.service.EmailService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "http://localhost:4200")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Envoyer un email de validation avec code de sécurité
     */
    @PostMapping("/send-validation")
    public ResponseEntity<EmailResponse> sendValidationEmail(@RequestBody EmailRequest emailRequest) {
        try {
            System.out.println("📧 === DÉBUT TRAITEMENT EMAIL ===");
            System.out.println("📧 EmailRequest reçu: " + emailRequest);
            System.out.println("📧 To: " + emailRequest.getTo());
            System.out.println("📧 Subject: " + emailRequest.getSubject());
            System.out.println("📧 Security Code: " + emailRequest.getSecurityCode());
            System.out.println("📧 Registration Link: " + emailRequest.getRegistrationLink());
            System.out.println("📧 Body length: " + (emailRequest.getBody() != null ? emailRequest.getBody().length() : 0));

            // BLOQUER l'envoi d'email avec code de sécurité lors de la validation de dossier
            if (emailRequest.getSubject() != null && 
                emailRequest.getSubject().contains("Votre immatriculation a été validée")) {
                System.out.println("🚫 BLOCAGE: Email de validation de dossier avec code de sécurité bloqué");
                System.out.println("📧 Utiliser le système TIN à la place");
                
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("Email avec code de sécurité bloqué - Utiliser le système TIN");
                return ResponseEntity.badRequest().body(response);
            }

            // Validation des champs requis
            if (emailRequest.getTo() == null || emailRequest.getTo().trim().isEmpty()) {
                System.err.println("❌ Email 'to' manquant ou vide");
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("L'adresse email du destinataire est requise");
                return ResponseEntity.badRequest().body(response);
            }

            if (emailRequest.getSecurityCode() == null || emailRequest.getSecurityCode().trim().isEmpty()) {
                System.err.println("❌ Security code manquant ou vide");
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("Le code de sécurité est requis");
                return ResponseEntity.badRequest().body(response);
            }

            boolean emailSent = emailService.sendValidationEmail(
                emailRequest.getTo(),
                emailRequest.getSubject(),
                emailRequest.getBody(),
                emailRequest.getSecurityCode(),
                emailRequest.getRegistrationLink()
            );

            EmailResponse response = new EmailResponse();
            response.setSuccess(emailSent);
            response.setEmailSent(emailSent);
            
            if (emailSent) {
                response.setMessage("Email envoyé avec succès à " + emailRequest.getTo());
                System.out.println("✅ Email envoyé avec succès");
            } else {
                response.setMessage("Échec de l'envoi de l'email");
                System.out.println("❌ Échec de l'envoi de l'email");
            }

            System.out.println("📧 === FIN TRAITEMENT EMAIL ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
            
            EmailResponse response = new EmailResponse();
            response.setSuccess(false);
            response.setEmailSent(false);
            response.setMessage("Erreur technique: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Tester l'envoi d'email simple
     */
    @PostMapping("/test-send")
    public ResponseEntity<EmailResponse> testSendEmail(@RequestParam String to) {
        try {
            System.out.println("📧 Test d'envoi d'email simple à: " + to);
            
            String securityCode = "TEST-CODE-123";
            String subject = "Test Email - SmartTax";
            String body = "<h1>Test Email</h1><p>Ceci est un test d'envoi d'email depuis SmartTax.</p><p>Code de sécurité: <strong>" + securityCode + "</strong></p>";
            
            boolean emailSent = emailService.sendValidationEmail(to, subject, body, securityCode, "http://localhost:4200/register");
            
            EmailResponse response = new EmailResponse();
            response.setSuccess(emailSent);
            response.setEmailSent(emailSent);
            
            if (emailSent) {
                response.setMessage("Test email envoyé avec succès à " + to);
                System.out.println("✅ Test email envoyé avec succès");
            } else {
                response.setMessage("Échec de l'envoi du test email");
                System.out.println("❌ Échec de l'envoi du test email");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test d'email: " + e.getMessage());
            e.printStackTrace();
            
            EmailResponse response = new EmailResponse();
            response.setSuccess(false);
            response.setEmailSent(false);
            response.setMessage("Erreur technique lors du test: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Endpoint de debug pour tester la désérialisation JSON
     */
    @PostMapping("/debug-request")
    public ResponseEntity<Map<String, Object>> debugRequest(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("🐛 DEBUG: Request reçu: " + request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("received", request);
            response.put("keys", request.keySet());
            response.put("to", request.get("to"));
            response.put("subject", request.get("subject"));
            response.put("securityCode", request.get("securityCode"));
            response.put("body", request.get("body"));
            response.put("registrationLink", request.get("registrationLink"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Erreur debug: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Envoyer un email avec le TIN généré
     */
    @PostMapping("/send-tin-email")
    public ResponseEntity<EmailResponse> sendTINEmail(@RequestBody EmailRequest emailRequest) {
        try {
            System.out.println("📧 === DÉBUT TRAITEMENT EMAIL AVEC TIN ===");
            System.out.println("📧 EmailRequest reçu: " + emailRequest);
            System.out.println("📧 To: " + emailRequest.getTo());
            System.out.println("📧 Subject: " + emailRequest.getSubject());
            System.out.println("📧 Registration Link: " + emailRequest.getRegistrationLink());
            System.out.println("📧 Body length: " + (emailRequest.getBody() != null ? emailRequest.getBody().length() : 0));

            // Validation des champs requis
            if (emailRequest.getTo() == null || emailRequest.getTo().trim().isEmpty()) {
                System.err.println("❌ Email 'to' manquant ou vide");
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("L'adresse email du destinataire est requise");
                return ResponseEntity.badRequest().body(response);
            }

            // Envoyer l'email avec le TIN
            boolean emailSent = emailService.sendTINEmail(
                emailRequest.getTo(),
                emailRequest.getSubject(),
                emailRequest.getBody(),
                "", // Le TIN sera extrait du body
                emailRequest.getRegistrationLink()
            );

            EmailResponse response = new EmailResponse();
            response.setSuccess(emailSent);
            response.setEmailSent(emailSent);
            response.setMessage(emailSent ? "Email avec TIN envoyé avec succès" : "Échec de l'envoi de l'email");

            System.out.println("📧 Email avec TIN envoyé: " + emailSent);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email avec TIN: " + e.getMessage());
            e.printStackTrace();
            
            EmailResponse response = new EmailResponse();
            response.setSuccess(false);
            response.setEmailSent(false);
            response.setMessage("Erreur lors de l'envoi: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Envoyer un email de rejet
     */
    @PostMapping("/send-rejection")
    public ResponseEntity<EmailResponse> sendRejectionEmail(@RequestBody EmailRequest emailRequest) {
        try {
            System.out.println("📧 === DÉBUT TRAITEMENT EMAIL DE REJET ===");
            System.out.println("📧 EmailRequest reçu: " + emailRequest);
            System.out.println("📧 To: " + emailRequest.getTo());
            System.out.println("📧 Subject: " + emailRequest.getSubject());
            System.out.println("📧 Registration Link: " + emailRequest.getRegistrationLink());
            System.out.println("📧 Body length: " + (emailRequest.getBody() != null ? emailRequest.getBody().length() : 0));

            // Validation des champs requis
            if (emailRequest.getTo() == null || emailRequest.getTo().trim().isEmpty()) {
                System.err.println("❌ Email 'to' manquant ou vide");
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("L'adresse email du destinataire est requise");
                return ResponseEntity.badRequest().body(response);
            }

            if (emailRequest.getBody() == null || emailRequest.getBody().trim().isEmpty()) {
                System.err.println("❌ Corps de l'email manquant ou vide");
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("Le corps de l'email est requis");
                return ResponseEntity.badRequest().body(response);
            }

            // Envoyer l'email de rejet
            boolean emailSent = emailService.sendEmail(
                emailRequest.getTo(),
                emailRequest.getSubject(),
                emailRequest.getBody()
            );

            EmailResponse response = new EmailResponse();
            response.setSuccess(emailSent);
            response.setEmailSent(emailSent);
            
            if (emailSent) {
                response.setMessage("Email de rejet envoyé avec succès à " + emailRequest.getTo());
                System.out.println("✅ Email de rejet envoyé avec succès");
            } else {
                response.setMessage("Échec de l'envoi de l'email de rejet");
                System.out.println("❌ Échec de l'envoi de l'email de rejet");
            }

            System.out.println("📧 === FIN TRAITEMENT EMAIL DE REJET ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email de rejet: " + e.getMessage());
            e.printStackTrace();
            
            EmailResponse response = new EmailResponse();
            response.setSuccess(false);
            response.setEmailSent(false);
            response.setMessage("Erreur technique lors de l'envoi de l'email de rejet: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Envoyer un email simple (réponse agent, notification, etc.)
     */
    @PostMapping("/send-simple")
    public ResponseEntity<EmailResponse> sendSimpleEmail(@RequestBody EmailRequest emailRequest) {
        try {
            if (emailRequest.getTo() == null || emailRequest.getTo().trim().isEmpty()) {
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("L'adresse email du destinataire est requise");
                return ResponseEntity.badRequest().body(response);
            }

            if (emailRequest.getSubject() == null || emailRequest.getSubject().trim().isEmpty()) {
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("Le sujet de l'email est requis");
                return ResponseEntity.badRequest().body(response);
            }

            if (emailRequest.getBody() == null || emailRequest.getBody().trim().isEmpty()) {
                EmailResponse response = new EmailResponse();
                response.setSuccess(false);
                response.setEmailSent(false);
                response.setMessage("Le corps de l'email est requis");
                return ResponseEntity.badRequest().body(response);
            }

            boolean emailSent = emailService.sendEmail(
                    emailRequest.getTo(),
                    emailRequest.getSubject(),
                    emailRequest.getBody()
            );

            EmailResponse response = new EmailResponse();
            response.setSuccess(emailSent);
            response.setEmailSent(emailSent);
            response.setMessage(emailSent
                    ? "Email envoyé avec succès à " + emailRequest.getTo()
                    : "Échec de l'envoi de l'email");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            EmailResponse response = new EmailResponse();
            response.setSuccess(false);
            response.setEmailSent(false);
            response.setMessage("Erreur technique lors de l'envoi de l'email: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Tester la configuration email
     */
    @GetMapping("/test")
    public ResponseEntity<String> testEmailConfiguration() {
        try {
            String testResult = emailService.testEmailConfiguration();
            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur de test: " + e.getMessage());
        }
    }
}
