package tn.esprit.arabsoftback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.dto.MessageDto;
import tn.esprit.arabsoftback.dto.ReclamationAgentStatsDto;
import tn.esprit.arabsoftback.dto.ReclamationDto;
import tn.esprit.arabsoftback.entity.Reclamation;
import tn.esprit.arabsoftback.service.ReclamationService;

import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/reclamation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Réclamations", description = "API pour la gestion des réclamations")
@CrossOrigin(origins = "http://localhost:4200")
public class ReclamationController {
    
    private final ReclamationService reclamationService;
    
    // ==================== CRÉATION ====================
    
    @PostMapping("/create")
    @Operation(summary = "Créer une nouvelle réclamation", 
               description = "Crée une nouvelle réclamation avec les informations et fichiers fournis")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Réclamation créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "413", description = "Fichier trop volumineux"),
        @ApiResponse(responseCode = "415", description = "Type de fichier non supporté")
    })
    public ResponseEntity<ReclamationDto.CreateReclamationResponse> createReclamation(
            @RequestPart("reclamation") @Valid ReclamationDto.CreateReclamationDto dto,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        
        String emailUser = getCurrentUserEmail();
        ReclamationDto.CreateReclamationResponse response = reclamationService.createReclamation(dto, emailUser, files);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/create-with-form")
    @Operation(summary = "Créer une réclamation avec formulaire", 
               description = "Crée une réclamation en utilisant des paramètres de formulaire")
    public ResponseEntity<ReclamationDto.CreateReclamationResponse> createReclamationWithForm(
            @RequestParam("type") String type,
            @RequestParam("categorie") String categorie,
            @RequestParam("sujet") String sujet,
            @RequestParam("description") String description,
            @RequestParam("urgence") String urgence,
            @RequestParam(value = "reference", required = false) String reference,
            @RequestParam(value = "nom", required = false) String nom,
            @RequestParam(value = "telephone", required = false) String telephone,
            @RequestParam(value = "emailUser", required = false) String emailUser,
            @RequestParam(value = "statut", defaultValue = "BROUILLON") String statut,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        
        // Construire le DTO
        ReclamationDto.CreateReclamationDto dto = ReclamationDto.CreateReclamationDto.builder()
                .type(ReclamationDto.TypeReclamationDto.builder().value(type).build())
                .categorie(categorie)
                .sujet(sujet)
                .description(description)
                .urgence(ReclamationDto.NiveauUrgenceDto.builder().value(urgence).build())
                .referenceUser(reference)
                .nomUser(nom)
                .telephoneUser(telephone)
                .build();
        
        String authenticatedEmail = getCurrentUserEmail();
        ReclamationDto.CreateReclamationResponse response = reclamationService.createReclamation(
                dto,
                authenticatedEmail,
                files,
                statut,
                emailUser
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // ==================== LECTURE ====================
    
    @GetMapping("/user")
    @Operation(summary = "Récupérer les réclamations de l'utilisateur", 
               description = "Retourne toutes les réclamations de l'utilisateur connecté")
    public ResponseEntity<List<ReclamationDto>> getUserReclamations() {
        String emailUser = getCurrentUserEmail();
        if (emailUser == null || emailUser.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ReclamationDto> reclamations = reclamationService.getReclamationsByUser(emailUser);
        return ResponseEntity.ok(reclamations);
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Récupérer toutes les réclamations", 
               description = "Retourne les réclamations paginées. Filtres optionnels : statut, état de traitement, urgence, recherche texte.")
    public ResponseEntity<Page<ReclamationDto>> getAllReclamations(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filtrer par statut (BROUILLON, SOUMIS, EN_COURS, RESOLU, REJETE)") 
            @RequestParam(required = false) String statut,
            @Parameter(description = "Recherche texte (référence, sujet, email, etc.)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Champ de tri (dateCreation, dateSoumission, sujet, nomUser, emailUser, reference, categorie, type, urgence, statut, etatReclamation)")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Direction: ASC ou DESC")
            @RequestParam(required = false, defaultValue = "DESC") String direction,
            @Parameter(description = "Filtrer par état de traitement (EN_COURS, TRAITE)")
            @RequestParam(required = false) String etat,
            @Parameter(description = "Filtrer par urgence (BASSE, MOYENNE, HAUTE, URGENTE)")
            @RequestParam(required = false) String urgence) {
        
        Reclamation.StatutReclamation statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try {
                statutEnum = Reclamation.StatutReclamation.valueOf(statut.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        Reclamation.EtatReclamation etatEnum = null;
        if (etat != null && !etat.isBlank()) {
            try {
                etatEnum = Reclamation.EtatReclamation.valueOf(etat.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        Reclamation.NiveauUrgence urgenceEnum = null;
        if (urgence != null && !urgence.isBlank()) {
            try {
                urgenceEnum = Reclamation.NiveauUrgence.valueOf(urgence.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        Page<ReclamationDto> reclamations = reclamationService.getAllReclamations(
                page, size, statutEnum, search, sort, direction, etatEnum, urgenceEnum);
        return ResponseEntity.ok(reclamations);
    }

    @GetMapping("/agent-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Statistiques réclamations (dashboard agent)",
               description = "Compteurs agrégés pour un statut donné (par défaut SOUMIS).")
    public ResponseEntity<ReclamationAgentStatsDto> getAgentReclamationStats(
            @Parameter(description = "Statut (ex. SOUMIS)") @RequestParam(defaultValue = "SOUMIS") String statut) {
        try {
            Reclamation.StatutReclamation statutEnum = Reclamation.StatutReclamation.valueOf(statut.trim().toUpperCase());
            return ResponseEntity.ok(reclamationService.getAgentReclamationStats(statutEnum));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une réclamation par ID", 
               description = "Retourne une réclamation spécifique par son ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Réclamation trouvée"),
        @ApiResponse(responseCode = "404", description = "Réclamation non trouvée")
    })
    public ResponseEntity<ReclamationDto> getReclamationById(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id) {
        
        ReclamationDto reclamation = reclamationService.getReclamationDtoById(id);
        return ResponseEntity.ok(reclamation);
    }
    
    @GetMapping("/reference/{reference}")
    @Operation(summary = "Récupérer une réclamation par référence", 
               description = "Retourne une réclamation spécifique par sa référence")
    public ResponseEntity<ReclamationDto> getReclamationByReference(
            @Parameter(description = "Référence de la réclamation") @PathVariable String reference) {
        
        ReclamationDto reclamation = reclamationService.getReclamationByReference(reference);
        return ResponseEntity.ok(reclamation);
    }
    
    // ==================== MISE À JOUR ====================
    
    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une réclamation", 
               description = "Met à jour une réclamation existante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Réclamation mise à jour"),
        @ApiResponse(responseCode = "404", description = "Réclamation non trouvée"),
        @ApiResponse(responseCode = "400", description = "Mise à jour non autorisée")
    })
    public ResponseEntity<ReclamationDto> updateReclamation(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id,
            @RequestPart("reclamation") @Valid ReclamationDto.UpdateReclamationDto dto,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        
        ReclamationDto reclamation = reclamationService.updateReclamation(id, dto, files);
        return ResponseEntity.ok(reclamation);
    }

    @PutMapping("/{id}/update-with-form")
    @Operation(summary = "Mettre à jour une réclamation avec formulaire",
            description = "Met à jour une réclamation existante en utilisant des paramètres de formulaire")
    public ResponseEntity<ReclamationDto> updateReclamationWithForm(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "categorie", required = false) String categorie,
            @RequestParam("sujet") String sujet,
            @RequestParam("description") String description,
            @RequestParam("urgence") String urgence,
            @RequestParam(value = "reference", required = false) String reference,
            @RequestParam(value = "nom", required = false) String nom,
            @RequestParam(value = "telephone", required = false) String telephone,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        ReclamationDto.UpdateReclamationDto dto = ReclamationDto.UpdateReclamationDto.builder()
                .type(type != null ? ReclamationDto.TypeReclamationDto.builder().value(type).build() : null)
                .categorie(categorie)
                .sujet(sujet)
                .description(description)
                .urgence(ReclamationDto.NiveauUrgenceDto.builder().value(urgence).build())
                .referenceUser(reference)
                .nomUser(nom)
                .telephoneUser(telephone)
                .build();

        ReclamationDto reclamation = reclamationService.updateReclamation(id, dto, files);
        return ResponseEntity.ok(reclamation);
    }
    
    @PutMapping("/{id}/submit")
    @Operation(summary = "Soumettre un brouillon", 
               description = "Transforme un brouillon en réclamation soumise")
    public ResponseEntity<ReclamationDto> submitDraft(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id) {
        
        ReclamationDto reclamation = reclamationService.submitDraft(id);
        return ResponseEntity.ok(reclamation);
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Changer le statut d'une réclamation", 
               description = "Change le statut d'une réclamation (admin uniquement)")
    public ResponseEntity<ReclamationDto> changeStatut(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id,
            @RequestParam("statut") Reclamation.StatutReclamation statut,
            @RequestParam(value = "motif", required = false) String motif) {
        
        ReclamationDto reclamation = reclamationService.changeStatut(id, statut, motif);
        return ResponseEntity.ok(reclamation);
    }

    @PutMapping("/{id}/etat-traitement")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Mettre à jour l'état de traitement",
               description = "En cours ou Traité (agent / admin).")
    public ResponseEntity<ReclamationDto> changeEtatTraitement(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id,
            @RequestParam("etat") Reclamation.EtatReclamation etat) {
        ReclamationDto reclamation = reclamationService.changeEtatReclamation(id, etat);
        return ResponseEntity.ok(reclamation);
    }
    
    // ==================== SUPPRESSION ====================
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une réclamation", 
               description = "Supprime une réclamation (brouillons uniquement)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Réclamation supprimée"),
        @ApiResponse(responseCode = "404", description = "Réclamation non trouvée"),
        @ApiResponse(responseCode = "400", description = "Suppression non autorisée")
    })
    public ResponseEntity<Void> deleteReclamation(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id) {
        
        reclamationService.deleteReclamation(id);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== MESSAGERIE ====================
    
    @GetMapping("/{id}/messages")
    @Operation(summary = "Récupérer les messages d'une réclamation", 
               description = "Retourne tous les messages d'une réclamation")
    public ResponseEntity<List<MessageDto>> getMessages(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id) {
        
        List<MessageDto> messages = reclamationService.getMessages(id);
        return ResponseEntity.ok(messages);
    }
    
    @PostMapping("/{id}/messages")
    @Operation(summary = "Envoyer un message", 
               description = "Envoie un nouveau message dans une réclamation")
    public ResponseEntity<MessageDto> sendMessage(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id,
            @RequestPart("message") @Valid MessageDto.CreateMessageDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        
        MessageDto message = reclamationService.sendMessage(id, dto, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
    
    @PostMapping("/{id}/messages/simple")
    @Operation(summary = "Envoyer un message simple", 
               description = "Envoie un message texte simple")
    public ResponseEntity<MessageDto> sendSimpleMessage(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id,
            @RequestParam("contenu") String contenu,
            @RequestParam("auteur") String auteur) {
        
        MessageDto.CreateMessageDto dto = MessageDto.CreateMessageDto.builder()
                .contenu(contenu)
                .auteur(auteur)
                .build();
        
        MessageDto message = reclamationService.sendMessage(id, dto, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
    
    // ==================== RECHERCHE ====================
    
    @GetMapping("/search")
    @Operation(summary = "Rechercher des réclamations", 
               description = "Recherche des réclamations par texte")
    public ResponseEntity<List<ReclamationDto>> searchReclamations(
            @Parameter(description = "Texte de recherche") @RequestParam("q") String query,
            @Parameter(description = "Email utilisateur (optionnel)") @RequestParam(value = "email", required = false) String email) {
        
        List<ReclamationDto> reclamations = reclamationService.searchReclamations(query, email);
        return ResponseEntity.ok(reclamations);
    }
    
    @GetMapping("/filter")
    @Operation(summary = "Filtrer les réclamations", 
               description = "Filtre les réclamations par statut")
    public ResponseEntity<List<ReclamationDto>> filterByStatut(
            @Parameter(description = "Statut à filtrer") @RequestParam("statut") String statut) {
        
        // Implémenter selon les besoins
        return ResponseEntity.ok(List.of());
    }
    
    // ==================== STATISTIQUES ====================
    
    @GetMapping("/statistics")
    @Operation(summary = "Récupérer les statistiques", 
               description = "Retourne les statistiques des réclamations")
    public ResponseEntity<ReclamationDto.ReclamationStatistics> getStatistics(
            @Parameter(description = "Email utilisateur (optionnel)") @RequestParam(value = "email", required = false) String email) {
        
        ReclamationDto.ReclamationStatistics stats = reclamationService.getStatistics(email);
        return ResponseEntity.ok(stats);
    }
    
    // ==================== FICHIERS ====================
    
    @GetMapping("/files/{reference}/{fileName}")
    @Operation(summary = "Télécharger un fichier", 
               description = "Télécharge une pièce jointe d'une réclamation")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Référence de la réclamation") @PathVariable String reference,
            @Parameter(description = "Nom du fichier") @PathVariable String fileName) {
        
        try {
            // Compatibilité: pièces jointes réclamation (racine) + pièces jointes messagerie (sous-dossier messages).
            Path filePath = Paths.get("uploads/reclamations/" + reference + "/" + fileName);
            if (!Files.exists(filePath)) {
                filePath = Paths.get("uploads/reclamations/" + reference + "/messages/" + fileName);
            }
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new ByteArrayResource(Files.readAllBytes(filePath));
            
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du fichier: " + fileName, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}/files/{fileName}")
    @Operation(summary = "Supprimer une pièce jointe", 
               description = "Supprime une pièce jointe d'une réclamation")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "ID de la réclamation") @PathVariable Long id,
            @Parameter(description = "Nom du fichier") @PathVariable String fileName) {
        
        // Implémenter selon les besoins
        return ResponseEntity.noContent().build();
    }
    
    // ==================== UTILITAIRES ====================
    
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String name = authentication.getName();
        if (name == null
                || name.isBlank()
                || "anonymousUser".equalsIgnoreCase(name)) {
            return null;
        }
        return name;
    }
}
