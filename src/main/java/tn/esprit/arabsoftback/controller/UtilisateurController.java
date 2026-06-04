package tn.esprit.arabsoftback.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.service.IUtilisateurService;
import tn.esprit.arabsoftback.service.UploadStorageService;
import tn.esprit.arabsoftback.service.UserPresenceService;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowCredentials = "true")
public class UtilisateurController {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurController.class);

    @Autowired
    private IUtilisateurService utilisateurService;

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private UploadStorageService uploadStorageService;

    @GetMapping("/me")
    public ResponseEntity<?> getUserDetails(Authentication authentication) {
        try {
            String email = authentication.getName();
            logger.info("Fetching details for user with email: {}", email);
            Optional<Utilisateur> utilisateur = utilisateurService.getUtilisateurByEmail(email);
            if (utilisateur.isPresent()) {
                return ResponseEntity.ok(utilisateur.get());
            } else {
                logger.warn("No user found with email: {}", email);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Utilisateur non trouvé");
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error fetching user details: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération des détails: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/updateuser")
    public ResponseEntity<?> updateUserDetails(Authentication authentication, @RequestBody Utilisateur utilisateurDetails) {
        try {
            String email = authentication.getName();
            logger.info("Updating user with email: {}", email);
            Utilisateur updatedUtilisateur = utilisateurService.updateUtilisateur(email, utilisateurDetails);
            return ResponseEntity.ok(updatedUtilisateur);
        } catch (RuntimeException e) {
            logger.error("Error updating user: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    @DeleteMapping("/deleteuser")
    public ResponseEntity<?> deleteUser(Authentication authentication) {
        try {
            String email = authentication.getName();
            logger.info("Deleting user with email: {}", email);
            utilisateurService.deleteUtilisateur(email);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting user: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la suppression: " + e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserById(@PathVariable Integer id, Authentication authentication) {
        try {
            logger.info("Deleting user with id: {}", id);
            Optional<Utilisateur> utilisateur = utilisateurService.getUtilisateurById(id);
            if (utilisateur.isPresent()) {
                // Vérifier si l'utilisateur connecté est un admin ou le propriétaire
                String currentEmail = authentication.getName();
                if (!currentEmail.equals(utilisateur.get().getEmail()) && !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                    logger.warn("Unauthorized attempt to delete user with id: {}", id);
                    return ResponseEntity.status(403).body("Vous n'êtes pas autorisé à supprimer cet utilisateur.");
                }
                utilisateurService.deleteUtilisateurById(id); // Utiliser la nouvelle méthode
                return ResponseEntity.ok().build();
            } else {
                logger.warn("No user found with id: {}", id);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Utilisateur non trouvé");
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error deleting user by id: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la suppression: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<Map<String, Object>> uploadPhoto(Authentication authentication, @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = authentication.getName();
            logger.info("Uploading photo for user with email: {}", email);
            Optional<Utilisateur> optionalUtilisateur = utilisateurService.getUtilisateurByEmail(email);
            if (!optionalUtilisateur.isPresent()) {
                logger.warn("No user found with email: {}", email);
                response.put("success", false);
                response.put("error", "Utilisateur non trouvé");
                return ResponseEntity.status(404).body(response);
            }

            Utilisateur utilisateur = optionalUtilisateur.get();

            if (!file.getContentType().startsWith("image/")) {
                logger.warn("Invalid file type for upload: {}", file.getContentType());
                response.put("success", false);
                response.put("error", "Seuls les fichiers image sont autorisés");
                return ResponseEntity.status(400).body(response);
            }

            if (utilisateur.getPhoto() != null && !utilisateur.getPhoto().isEmpty()) {
                uploadStorageService.deleteIfManaged(utilisateur.getPhoto());
            }

            String photoUrl = uploadStorageService.storeUserPhoto(file, email);
            utilisateur.setPhoto(photoUrl);
            utilisateurService.updateUtilisateur(email, utilisateur);

            response.put("success", true);
            response.put("photoPath", photoUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error uploading photo: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "Erreur lors du téléchargement de l'image: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        try {
            String email = request.get("email");
            logger.info("Received forgot password request for email: {}", email);
            Optional<Utilisateur> optionalUtilisateur = utilisateurService.getUtilisateurByEmail(email);
            if (!optionalUtilisateur.isPresent()) {
                logger.warn("No user found with email: {}", email);
                response.put("error", "Aucun utilisateur trouvé avec cet e-mail");
                return ResponseEntity.status(404).body(response);
            }

            Utilisateur utilisateur = optionalUtilisateur.get();
            String token = UUID.randomUUID().toString();
            utilisateurService.createPasswordResetTokenForUser(utilisateur, token);
            utilisateurService.sendPasswordResetEmail(email, token);
            logger.info("Password reset email sent to: {}", email);
            response.put("success", "Un lien de réinitialisation a été envoyé à votre e-mail");
            return ResponseEntity.ok(response);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email: {}", e.getMessage());
            response.put("error", "Erreur lors de l'envoi de l'e-mail: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Endpoint reset-password supprimé - utiliser /api/auth/reset-password à la place

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserById(@PathVariable Integer id, @RequestBody Utilisateur utilisateurDetails, Authentication authentication) {
        try {
            logger.info("Updating user with id: {}", id);
            
            // Vérifier si l'utilisateur existe
            Optional<Utilisateur> existingUser = utilisateurService.getUtilisateurById(id);
            if (!existingUser.isPresent()) {
                logger.warn("No user found with id: {}", id);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Utilisateur non trouvé");
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            // Vérifier si l'utilisateur connecté est un admin
            if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                logger.warn("Unauthorized attempt to update user with id: {}", id);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Vous n'êtes pas autorisé à modifier cet utilisateur");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            // Mettre à jour l'utilisateur
            Utilisateur userToUpdate = existingUser.get();
            
            // Mettre à jour uniquement les champs non nuls
            if (utilisateurDetails.getFirstName() != null) {
                userToUpdate.setFirstName(utilisateurDetails.getFirstName());
            }
            if (utilisateurDetails.getLastName() != null) {
                userToUpdate.setLastName(utilisateurDetails.getLastName());
            }
            if (utilisateurDetails.getEmail() != null) {
                userToUpdate.setEmail(utilisateurDetails.getEmail());
            }
            if (utilisateurDetails.getRole() != null) {
                userToUpdate.setRole(utilisateurDetails.getRole());
            }
            if (utilisateurDetails.getStatus() != null) {
                userToUpdate.setStatus(utilisateurDetails.getStatus());
            }
            
            Utilisateur updatedUser = utilisateurService.updateUtilisateurById(id, userToUpdate);
            logger.info("Successfully updated user with id: {}", id);
            return ResponseEntity.ok(updatedUser);
            
        } catch (Exception e) {
            logger.error("Error updating user by id: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication, @RequestBody Map<String, String> passwordData) {
        Map<String, String> response = new HashMap<>();
        try {
            String email = authentication.getName();
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");
            
            logger.info("Changing password for user with email: {}", email);
            
            if (currentPassword == null || newPassword == null || currentPassword.isEmpty() || newPassword.isEmpty()) {
                response.put("error", "Les mots de passe actuel et nouveau sont requis");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<Utilisateur> optionalUtilisateur = utilisateurService.getUtilisateurByEmail(email);
            if (!optionalUtilisateur.isPresent()) {
                logger.warn("No user found with email: {}", email);
                response.put("error", "Utilisateur non trouvé");
                return ResponseEntity.status(404).body(response);
            }
            
            Utilisateur utilisateur = optionalUtilisateur.get();
            
            // Vérifier le mot de passe actuel
            if (!utilisateurService.checkPassword(currentPassword, utilisateur.getPassword())) {
                logger.warn("Invalid current password for user: {}", email);
                response.put("error", "Le mot de passe actuel est incorrect");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Mettre à jour le mot de passe
            utilisateurService.changePassword(email, newPassword);
            logger.info("Password successfully changed for user: {}", email);
            response.put("success", "Mot de passe changé avec succès");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error changing password: {}", e.getMessage());
            response.put("error", "Erreur lors du changement de mot de passe: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUtilisateurById(@PathVariable Integer id) {
        try {
            logger.info("Fetching user with id: {}", id);
            Optional<Utilisateur> utilisateur = utilisateurService.getUtilisateurById(id);
            if (utilisateur.isPresent()) {
                return ResponseEntity.ok(utilisateur.get());
            } else {
                logger.warn("No user found with id: {}", id);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Utilisateur non trouvé");
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error fetching user by id: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Utilisateur>> getAllUtilisateurs() {
        logger.info("Fetching all users");
        List<Utilisateur> utilisateurs = utilisateurService.getAllUtilisateurs();
        return ResponseEntity.ok(utilisateurs);
    }

    @PostMapping("/presence/heartbeat")
    public ResponseEntity<Void> presenceHeartbeat(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        utilisateurService.getUtilisateurByEmail(authentication.getName())
                .ifPresent(u -> userPresenceService.markPresent(u.getIdUtilisateur()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/online")
    public ResponseEntity<?> getOnlineUsers(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Accès réservé aux administrateurs");
            return ResponseEntity.status(403).body(error);
        }
        Set<Integer> onlineUserIds = userPresenceService.getOnlineUserIds();
        Map<String, Object> body = new HashMap<>();
        body.put("onlineUserIds", onlineUserIds);
        body.put("count", onlineUserIds.size());
        body.put("thresholdMinutes", UserPresenceService.ONLINE_THRESHOLD.toMinutes());
        return ResponseEntity.ok(body);
    }
}