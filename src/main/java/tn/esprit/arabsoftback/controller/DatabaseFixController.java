package tn.esprit.arabsoftback.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db/fix")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class DatabaseFixController {
    
    private final JdbcTemplate jdbcTemplate;
    
    @PostMapping("/fix-role-constraint")
    public ResponseEntity<Map<String, Object>> fixRoleConstraint() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Début de la correction de la contrainte de rôle...");
            
            // 1. Vérifier la contrainte actuelle
            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                "SELECT conname, consrc FROM pg_constraint WHERE conname = 'utilisateur_role_check'"
            );
            
            response.put("ancienneContrainte", constraints);
            
            // 2. Supprimer l'ancienne contrainte
            jdbcTemplate.execute("ALTER TABLE utilisateur DROP CONSTRAINT IF EXISTS utilisateur_role_check");
            log.info("Ancienne contrainte supprimée");
            
            // 3. Créer la nouvelle contrainte
            jdbcTemplate.execute(
                "ALTER TABLE utilisateur ADD CONSTRAINT utilisateur_role_check " +
                "CHECK (role IN ('ADMIN', 'AGENT', 'CONTRIBUABLE'))"
            );
            log.info("Nouvelle contrainte créée");
            
            // 4. Vérifier la nouvelle contrainte
            List<Map<String, Object>> newConstraints = jdbcTemplate.queryForList(
                "SELECT conname, consrc FROM pg_constraint WHERE conname = 'utilisateur_role_check'"
            );
            
            response.put("nouvelleContrainte", newConstraints);
            
            // 5. Afficher les rôles existants
            List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                "SELECT DISTINCT role FROM utilisateur"
            );
            
            response.put("rolesExistants", roles);
            
            // 6. Créer les comptes par défaut
            try {
                jdbcTemplate.execute(
                    "INSERT INTO utilisateur (first_name, last_name, email, password, role, status, date_inscription, date_naissance, cin_confidence, cin_validation_status) " +
                    "SELECT 'Admin', 'SmartTax', 'admin@smarttax.gov.tn', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 'ACTIVE', CURRENT_DATE, '1990-01-01', 0.0, 'valid' " +
                    "WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'admin@smarttax.gov.tn')"
                );
                
                jdbcTemplate.execute(
                    "INSERT INTO utilisateur (first_name, last_name, email, password, role, status, date_inscription, date_naissance, cin_confidence, cin_validation_status) " +
                    "SELECT 'Agent', 'DGI', 'agent@smarttax.gov.tn', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'AGENT', 'ACTIVE', CURRENT_DATE, '1990-01-01', 0.0, 'valid' " +
                    "WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'agent@smarttax.gov.tn')"
                );
                
                response.put("comptesCreés", true);
                log.info("Comptes par défaut créés");
                
            } catch (Exception e) {
                log.warn("Erreur lors de la création des comptes: {}", e.getMessage());
                response.put("comptesCreés", false);
                response.put("comptesErreur", e.getMessage());
            }
            
            response.put("message", "Contrainte de rôle corrigée avec succès");
            response.put("success", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la correction de la contrainte", e);
            response.put("error", e.getMessage());
            response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/check-constraint")
    public ResponseEntity<Map<String, Object>> checkConstraint() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                "SELECT conname, consrc FROM pg_constraint WHERE conname = 'utilisateur_role_check'"
            );
            
            List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                "SELECT DISTINCT role FROM utilisateur"
            );
            
            response.put("constraint", constraints);
            response.put("roles", roles);
            response.put("message", constraints.isEmpty() ? "Aucune contrainte trouvée" : "Contrainte trouvée");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
