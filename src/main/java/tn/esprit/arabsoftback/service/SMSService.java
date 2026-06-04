package tn.esprit.arabsoftback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class SMSService {

    private static final Logger logger = LoggerFactory.getLogger(SMSService.class);

    @Value("${sms.service.enabled:false}")
    private boolean smsEnabled;

    @Value("${sms.service.provider:generic}")
    private String smsProvider;

    @Value("${sms.service.account-sid:}")
    private String accountSid;

    @Value("${sms.service.auth-token:}")
    private String authToken;

    @Value("${sms.service.from-number:}")
    private String fromNumber;

    @Value("${sms.service.url:}")
    private String smsServiceUrl;

    private final RestTemplate restTemplate;

    public SMSService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envoie un SMS via Twilio
     */
    private CompletableFuture<Boolean> sendTwilioSMS(String phoneNumber, String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Formater le numéro de téléphone pour Twilio
                String formattedPhoneNumber = formatPhoneNumberForTwilio(phoneNumber);
                
                // Préparer les headers pour l'authentification Basic
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                
                // Créer les credentials pour l'authentification Basic
                String credentials = accountSid + ":" + authToken;
                String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
                headers.set("Authorization", "Basic " + encodedCredentials);

                // Préparer le corps de la requête
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("To", formattedPhoneNumber);
                body.add("From", fromNumber);
                body.add("Body", buildVerificationMessage(code));

                // Créer l'entité HTTP
                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

                // URL de l'API Twilio
                String twilioUrl = smsServiceUrl + "/" + accountSid + "/Messages.json";

                logger.info("Envoi SMS via Twilio vers {} avec le code {}", formattedPhoneNumber, code);
                logger.info("URL Twilio: {}", twilioUrl);
                logger.info("Account SID: {}", accountSid);
                logger.info("From Number: {}", fromNumber);
                
                // Envoyer la requête
                ResponseEntity<String> response = restTemplate.postForEntity(twilioUrl, request, String.class);

                logger.info("Réponse Twilio - Status: {}", response.getStatusCode());
                logger.info("Réponse Twilio - Body: {}", response.getBody());

                if (response.getStatusCode() == HttpStatus.CREATED) {
                    logger.info("SMS envoyé avec succès via Twilio");
                    return true;
                } else {
                    logger.error("Échec de l'envoi SMS via Twilio - Status: {}", response.getStatusCode());
                    logger.error("Réponse Twilio: {}", response.getBody());
                    return false;
                }

            } catch (Exception e) {
                logger.error("Erreur lors de l'envoi SMS via Twilio: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Envoyer un SMS de vérification
     */
    public CompletableFuture<Boolean> sendVerificationSMS(String phoneNumber, String verificationCode) {
        try {
            if (!smsEnabled) {
                logger.warn("SMS service is disabled");
                return CompletableFuture.completedFuture(false);
            }
            
            logger.info("Envoi SMS de vérification vers {} avec le code {}", phoneNumber, verificationCode);
            return sendTwilioSMS(phoneNumber, verificationCode);
            
        } catch (Exception e) {
            logger.error("Error sending verification SMS: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Formater le numéro pour Twilio
     */
    private String formatPhoneNumberForTwilio(String phoneNumber) {
        String formatted = formatPhoneNumber(phoneNumber);
        // S'assurer que le numéro est au format international pour Twilio
        if (formatted != null && !formatted.startsWith("+")) {
            // Ajouter +216 si c'est un numéro tunisien sans indicatif
            if (formatted.matches("\\d{8}")) {
                formatted = "+216" + formatted;
            }
        }
        return formatted;
    }
    
    /**
     * Construire le message de vérification
     */
    private String buildVerificationMessage(String code) {
        return String.format("Votre code de vérification ArabSoft est: %s", code);
    }
    
    /**
     * Valider le format du numéro de téléphone
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Nettoyer le numéro (enlever espaces, +, etc.)
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // Vérifier si c'est un numéro tunisien (8 chiffres) ou international
        return cleanNumber.matches("^\\d{8}$") || // Tunisie: 8 chiffres
               cleanNumber.matches("^\\d{10,15}$"); // International: 10-15 chiffres
    }
    
    /**
     * Formater le numéro de téléphone
     */
    public String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // Si c'est un numéro tunisien de 8 chiffres, ajouter l'indicatif
        if (cleanNumber.matches("^\\d{8}$")) {
            return "+216" + cleanNumber;
        }
        
        // Si ça commence déjà par 216, ajouter le +
        if (cleanNumber.matches("^216\\d{8}$")) {
            return "+" + cleanNumber;
        }
        
        // Sinon, retourner le numéro tel quel (déjà formaté)
        return phoneNumber;
    }
    
    /**
     * Tester la configuration SMS
     */
    public String testSMSConfiguration() {
        StringBuilder result = new StringBuilder();
        result.append("SMS Service Configuration:\n");
        result.append("  Enabled: ").append(smsEnabled).append("\n");
        result.append("  Provider: ").append(smsProvider).append("\n");
        result.append("  Account SID: ").append(accountSid != null && !accountSid.isEmpty() ? "Configuré" : "Non configuré").append("\n");
        result.append("  Auth Token: ").append(authToken != null && !authToken.isEmpty() ? "Configuré" : "Non configuré").append("\n");
        result.append("  From Number: ").append(fromNumber).append("\n");
        result.append("  Service URL: ").append(smsServiceUrl).append("\n");
        
        if (smsEnabled && accountSid != null && !accountSid.isEmpty() && authToken != null && !authToken.isEmpty()) {
            result.append("  Status: Prêt à envoyer des SMS");
        } else {
            result.append("  Status: Service désactivé ou mal configuré");
        }
        
        return result.toString();
    }
}
