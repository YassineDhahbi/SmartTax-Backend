package tn.esprit.arabsoftback.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tn.esprit.arabsoftback.entity.Role;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.entity.Immatriculation;
import tn.esprit.arabsoftback.repository.IUtilisateurRepository;
import tn.esprit.arabsoftback.repository.ImmatriculationRepository;
import tn.esprit.arabsoftback.service.EmailService;
import tn.esprit.arabsoftback.service.NotificationProducerService;
import tn.esprit.arabsoftback.service.UserPresenceService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
        allowCredentials = "true")
public class AuthController {

    @Autowired
    private IUtilisateurRepository utilisateurRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ImmatriculationRepository immatriculationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationProducerService notificationProducerService;

    @Autowired
    private UserPresenceService userPresenceService;

    private final String SECRET_KEY = "8d4f7b2a9e3c1d5f6a8b0c2e4f7a9b1d3e5f7a9b2c4e6f8a0b1c2d3e4f5a6b789abcdef";
    private final long EXPIRATION_TIME = 864_000_000; // 10 jours

    @Value("${recaptcha.secret.key}")
    private String recaptchaSecretKey;

    private String lastRecaptchaResponse = "";

    // ====================== REGISTER ======================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Utilisateur utilisateur) {
        try {
            // Debug: Vérifier que le matricule est bien reçu
            System.out.println("=== DEBUG REGISTER ===");
            System.out.println("Matricule reçu: " + utilisateur.getMatricule());
            System.out.println("Tous les champs: " + utilisateur.toString());
            System.out.println("====================");

            String matriculeTin = utilisateur.getMatricule() != null
                    ? utilisateur.getMatricule().trim().toUpperCase()
                    : null;

            if (utilisateur.getRole() == Role.CONTRIBUABLE && matriculeTin != null && !matriculeTin.isEmpty()) {
                String securityCode = utilisateur.getSecurityCode();
                if (securityCode == null || securityCode.trim().isEmpty()) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Le code de sécurité est obligatoire (8 chiffres reçus par email avec votre TIN).");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                Immatriculation dossier = immatriculationRepository
                        .findTopByMatriculeFiscalOrMatriculeFiscalExistantOrderByIdDesc(matriculeTin, matriculeTin)
                        .orElse(null);
                if (dossier == null) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "TIN inconnu. Vérifiez votre numéro d'immatriculation fiscale.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                if (dossier.getStatus() != Immatriculation.DossierStatus.VALIDE) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Votre dossier n'est pas encore validé. Vous ne pouvez pas créer de compte pour ce TIN.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                if (Boolean.TRUE.equals(dossier.getRegistrationSecurityCodeConsumed())) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Ce code de sécurité a déjà été utilisé. Contactez le support en cas de problème.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                String storedHash = dossier.getRegistrationSecurityCodeHash();
                if (storedHash == null || storedHash.isEmpty()) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Aucun code d'activation n'est associé à ce TIN. Demandez une régénération à l'administration.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                if (!passwordEncoder.matches(securityCode.trim(), storedHash)) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Code de sécurité incorrect. Vérifiez les 8 chiffres reçus par email.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                if (dossier.getEmail() == null
                        || !dossier.getEmail().trim().equalsIgnoreCase(utilisateur.getEmail().trim())) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "L'adresse email doit être identique à celle du dossier d'immatriculation.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            if (utilisateurRepository.findByEmail(utilisateur.getEmail()).isPresent()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email déjà utilisé");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
            utilisateur.setSecurityCode(null);

            if (utilisateur.getRole() == null) {
                utilisateur.setRole(Role.CONTRIBUABLE);
            } else if (utilisateur.getRole() != Role.CONTRIBUABLE &&
                    utilisateur.getRole() != Role.AGENT &&
                    utilisateur.getRole() != Role.ADMIN) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Rôle invalide. Rôles autorisés : CONTRIBUABLE, AGENT, ADMIN");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Utilisateur savedUser = utilisateurRepository.save(utilisateur);
            if (utilisateur.getRole() == Role.CONTRIBUABLE && matriculeTin != null && !matriculeTin.isEmpty()) {
                immatriculationRepository.findTopByMatriculeFiscalOrMatriculeFiscalExistantOrderByIdDesc(matriculeTin, matriculeTin)
                        .ifPresent(d -> {
                            if (d.getRegistrationSecurityCodeHash() != null
                                    && !Boolean.TRUE.equals(d.getRegistrationSecurityCodeConsumed())) {
                                d.setRegistrationSecurityCodeConsumed(true);
                                immatriculationRepository.save(d);
                            }
                        });
            }
            sendNotificationToAdmins(
                    "NEW_USER",
                    "Nouvel utilisateur",
                    "Un nouvel utilisateur a ete cree: " + savedUser.getEmail() + ".",
                    Long.valueOf(savedUser.getIdUtilisateur())
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Utilisateur créé avec succès");
            response.put("utilisateur", savedUser);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de l'inscription: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // ====================== CREATE TEST AGENT ======================
    @PostMapping("/create-test-agent")
    public ResponseEntity<?> createTestAgent() {
        try {
            String email = "agent@test.com";
            if (utilisateurRepository.findByEmail(email).isPresent()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "L'utilisateur agent de test existe déjà");
                return ResponseEntity.ok(response);
            }

            Utilisateur agent = new Utilisateur();
            agent.setFirstName("Agent");
            agent.setLastName("DGI");
            agent.setEmail(email);
            agent.setPassword(passwordEncoder.encode("agent123"));
            agent.setRole(Role.AGENT);
            agent.setStatus("ACTIVE");

            Utilisateur saved = utilisateurRepository.save(agent);
            sendNotificationToAdmins(
                    "NEW_USER",
                    "Nouvel utilisateur",
                    "Un nouvel utilisateur a ete cree: " + saved.getEmail() + ".",
                    Long.valueOf(saved.getIdUtilisateur())
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Utilisateur agent de test créé avec succès");
            response.put("email", saved.getEmail());
            response.put("role", saved.getRole().toString());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la création de l'agent de test: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // ====================== LOGIN ======================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");

            Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!passwordEncoder.matches(password, utilisateur.getPassword())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Mot de passe incorrect");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // Vérifier si le compte est actif
            if (!"actif".equalsIgnoreCase(utilisateur.getStatus())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Votre compte est inactif. Veuillez contacter l'administrateur.");
                return ResponseEntity.status(403).body(errorResponse);
            }

            String token = Jwts.builder()
                    .setSubject(email)
                    .claim("role", utilisateur.getRole().toString())
                    .claim("userId", utilisateur.getIdUtilisateur())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), Jwts.SIG.HS512)
                    .compact();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Connexion réussie");
            response.put("idUtilisateur", utilisateur.getIdUtilisateur());
            response.put("role", utilisateur.getRole().toString());
            response.put("token", token);
            response.put("email", utilisateur.getEmail());
            response.put("firstName", utilisateur.getFirstName());
            response.put("lastName", utilisateur.getLastName());
            response.put("telephone", utilisateur.getTelephone());
            response.put("matricule", utilisateur.getMatricule());
            response.put("tin", utilisateur.getMatricule());

            userPresenceService.markPresent(utilisateur.getIdUtilisateur());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la connexion: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // ====================== RECAPTCHA ======================
    private boolean validateRecaptcha(String recaptchaToken) {
        if (recaptchaToken == null || recaptchaToken.isEmpty()) {
            System.out.println("Recaptcha token is null or empty");
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://www.google.com/recaptcha/api/siteverify";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("secret", recaptchaSecretKey);
            requestBody.put("response", recaptchaToken);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
            Map<String, Object> responseBody = response.getBody();

            lastRecaptchaResponse = responseBody != null ? responseBody.toString() : "No response";

            if (responseBody != null && responseBody.containsKey("success") && (Boolean) responseBody.get("success")) {
                return true;
            }

            System.out.println("reCAPTCHA validation failed: " + responseBody);
            return false;

        } catch (Exception e) {
            System.out.println("Recaptcha validation error: " + e.getMessage());
            lastRecaptchaResponse = "Error: " + e.getMessage();
            return false;
        }
    }

    private String getLastRecaptchaResponse() {
        return lastRecaptchaResponse;
    }

    private void sendNotificationToAdmins(String eventType, String title, String message, Long userIdReference) {
        List<Integer> adminIds = utilisateurRepository.findByRole(Role.ADMIN).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList();
        // publicationId field is reused as a generic reference id for frontend navigation.
        notificationProducerService.send(eventType, title, message, userIdReference, adminIds);
    }

    // ====================== FORGOT PASSWORD ======================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            System.out.println("Email reçu pour forgot-password: '" + email + "'");

            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email est requis");
                return ResponseEntity.badRequest().body(response);
            }

            Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);

            if (utilisateur == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Si cet email existe dans notre système, un lien de réinitialisation a été envoyé.");
                return ResponseEntity.ok(response);
            }

            String resetToken = Jwts.builder()
                    .setSubject(email)
                    .claim("type", "password-reset")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 heure
                    .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), Jwts.SIG.HS512)
                    .compact();

            String resetLink = "http://localhost:4200/reset-password?token=" + resetToken + "&email=" + email;

            try {
                String emailBody = generatePasswordResetEmailBody(utilisateur, resetLink);
                boolean emailSent = emailService.sendEmail(email, "SmartTax - Réinitialisation de votre mot de passe", emailBody);

                if (emailSent) {
                    System.out.println("Email de réinitialisation envoyé à: " + email);
                }
            } catch (Exception e) {
                System.err.println("Erreur envoi email reset password: " + e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Si cet email existe dans notre système, un lien de réinitialisation a été envoyé.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la demande de réinitialisation: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private String generatePasswordResetEmailBody(Utilisateur utilisateur, String resetLink) {
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f8f9fa;\">");
        body.append("<div style=\"background-color: #007bff; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;\">");
        body.append("<h1 style=\"color: white; margin: 0; font-size: 24px;\">Réinitialisation du mot de passe</h1>");
        body.append("<p style=\"color: #d4edda; margin: 10px 0 0 0; font-size: 16px;\">SmartTax</p>");
        body.append("</div>");
        body.append("<div style=\"padding: 30px; background-color: white; border-radius: 0 0 8px 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);\">");
        body.append("<h2 style=\"color: #333; margin-bottom: 20px;\">Bonjour ").append(utilisateur.getFirstName()).append(" ").append(utilisateur.getLastName()).append("!</h2>");
        body.append("<p style=\"color: #666; line-height: 1.6; margin-bottom: 20px;\">");
        body.append("Vous avez demandé la réinitialisation de votre mot de passe. ");
        body.append("Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe.");
        body.append("</p>");
        body.append("<div style=\"text-align: center; margin: 30px 0;\">");
        body.append("<a href=\"").append(resetLink).append("\" style=\"background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;\">");
        body.append("Réinitialiser mon mot de passe");
        body.append("</a>");
        body.append("</div>");
        body.append("<p style=\"color: #666; font-size: 14px; margin-top: 30px;\">");
        body.append("Ce lien expirera dans 1 heure. Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.");
        body.append("</p>");
        body.append("</div>");
        body.append("</div>");

        return body.toString();
    }

    // ====================== RESET PASSWORD ======================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            String email = request.get("email");

            System.out.println("Reset password request - Email: " + email + ", Token: " + 
                    (token != null ? token.substring(0, Math.min(token.length(), 50)) + "..." : "null"));

            if (token == null || token.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Token et nouveau mot de passe sont requis");
                return ResponseEntity.badRequest().body(response);
            }

            try {
                // === CORRECTION ICI ===
                Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Vérifier que c'est bien un token de réinitialisation
                String tokenType = claims.get("type", String.class);
                if (!"password-reset".equals(tokenType)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Token invalide");
                    return ResponseEntity.badRequest().body(response);
                }

                String tokenEmail = claims.getSubject();
                if (email != null && !email.equals(tokenEmail)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Email ne correspond pas au token");
                    return ResponseEntity.badRequest().body(response);
                }

                Utilisateur utilisateur = utilisateurRepository.findByEmail(tokenEmail)
                        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

                utilisateur.setPassword(passwordEncoder.encode(newPassword));
                utilisateurRepository.save(utilisateur);

                System.out.println("Mot de passe réinitialisé avec succès pour: " + tokenEmail);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Mot de passe réinitialisé avec succès");
                return ResponseEntity.ok(response);

            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                System.out.println("Token expiré: " + e.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Token expiré. Veuillez demander une nouvelle réinitialisation.");
                return ResponseEntity.badRequest().body(response);
            } catch (io.jsonwebtoken.security.SignatureException e) {   // Note : dans 0.12+ c'est souvent dans le package security
                System.out.println("Signature invalide: " + e.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Token invalide. Veuillez demander une nouvelle réinitialisation.");
                return ResponseEntity.badRequest().body(response);
            } catch (Exception e) {
                System.out.println("Erreur token: " + e.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Token invalide ou expiré");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la réinitialisation du mot de passe: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la réinitialisation: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ====================== VERIFY TIN ======================
    @PostMapping("/verify-tin")
    public ResponseEntity<?> verifyTIN(@RequestBody Map<String, String> request) {
        try {
            String tin = request.get("tin");
            if (tin != null) {
                tin = tin.trim().toUpperCase();
            }
            System.out.println("TIN reçu du frontend: '" + tin + "'");

            if (tin == null || tin.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "TIN est requis");
                return ResponseEntity.badRequest().body(response);
            }

            boolean formatValid = tin.matches("^[12]\\d{7}[A-Z]{2}$");
            System.out.println("Format TIN valide : " + formatValid);

            if (!formatValid) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Format TIN invalide. Exemple: 22600001PU");
                return ResponseEntity.badRequest().body(response);
            }

            Immatriculation immatriculation = immatriculationRepository
                    .findTopByMatriculeFiscalOrMatriculeFiscalExistantOrderByIdDesc(tin, tin)
                    .orElse(null);

            if (immatriculation == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("exists", false);
                response.put("message", "TIN non trouvé. Veuillez vérifier votre numéro.");
                return ResponseEntity.ok(response);
            }

            if (immatriculation.getStatus() != Immatriculation.DossierStatus.VALIDE) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("exists", true);
                response.put("message", "TIN trouvé mais dossier non encore validé. Veuillez attendre la validation.");
                return ResponseEntity.ok(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exists", true);
            response.put("message", "TIN valide !");
            response.put("email", immatriculation.getEmail());
            response.put("nom", immatriculation.getNom());
            response.put("prenom", immatriculation.getPrenom());
            response.put("telephone", immatriculation.getTelephone());
            response.put("cin", immatriculation.getCin());
            response.put("adresse", immatriculation.getAdresse());
            response.put("dateNaissance", immatriculation.getDateNaissance() != null ? immatriculation.getDateNaissance().toString() : "");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la vérification du TIN: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}