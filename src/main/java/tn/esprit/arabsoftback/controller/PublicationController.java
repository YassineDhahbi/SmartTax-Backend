package tn.esprit.arabsoftback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.entity.Publication;
import tn.esprit.arabsoftback.entity.Publication.PublicationStatus;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.service.PublicationService;
import tn.esprit.arabsoftback.service.UploadStorageService;
import tn.esprit.arabsoftback.service.UtilisateurService;
import tn.esprit.arabsoftback.dto.PublicationDto;
import tn.esprit.arabsoftback.dto.PublicationDto.CreatePublicationRequest;
import tn.esprit.arabsoftback.dto.PublicationDto.PublicationResponse;
import tn.esprit.arabsoftback.dto.PublicationCommentDto;
import tn.esprit.arabsoftback.dto.PublicationReportDto;
import tn.esprit.arabsoftback.exception.ResourceNotFoundException;
import tn.esprit.arabsoftback.mapper.PublicationMapper;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Publications", description = "API pour la gestion des publications fiscales")
@CrossOrigin(origins = "http://localhost:4200")
public class PublicationController {
    
    private final PublicationService publicationService;
    private final ObjectMapper objectMapper;
    private final UtilisateurService utilisateurService;
    private final PublicationMapper publicationMapper;
    private final UploadStorageService uploadStorageService;
    
    @Operation(summary = "Créer une nouvelle publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Publication créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<PublicationDto> createPublication(
            @Parameter(description = "Données de la publication") @Valid @RequestPart("publication") CreatePublicationRequest publicationRequest,
            @Parameter(description = "Image de la publication") @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {
        
        log.info("=== DÉBUT CRÉATION PUBLICATION ===");
        log.info("Authentication object: {}", authentication);
        log.info("Authentication principal: {}", authentication != null ? authentication.getPrincipal() : "null");
        log.info("Authentication authorities: {}", authentication != null ? authentication.getAuthorities() : "null");
        
        // Récupérer l'utilisateur authentifié depuis SecurityContext
        Utilisateur authenticatedUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            log.info("Email extrait de l'authentification: {}", email);
            authenticatedUser = utilisateurService.getUtilisateurByEmail(email).orElse(null);
            log.info("Utilisateur récupéré depuis BDD: {}", authenticatedUser != null ? "ID=" + authenticatedUser.getIdUtilisateur() + ", Email=" + authenticatedUser.getEmail() : "null");
        } else {
            log.warn("Authentification échouée - authentication est null ou non authentifié");
        }

        if (authenticatedUser == null) {
            log.warn("Création publication refusée: utilisateur authentifié introuvable");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Logs de débogage pour vérifier les données reçues
        log.info("PublicationRequest reçu: title={}, status={}, scheduledAt={}", 
                publicationRequest.getTitle(), 
                publicationRequest.getStatus(), 
                publicationRequest.getScheduledAt());
        
        // Logs spécifiques pour les tags
        log.info("Tags reçus: {}", publicationRequest.getAiGeneratedTags());
        log.info("Nombre de tags: {}", 
                publicationRequest.getAiGeneratedTags() != null ? publicationRequest.getAiGeneratedTags().size() : 0);
        
        // Log pour déboguer le JSON brut reçu
        try {
            // Essayer de convertir le DTO en JSON pour débogage
            String jsonContent = objectMapper.writeValueAsString(publicationRequest);
            log.info("JSON reçu du DTO: {}", jsonContent);
        } catch (Exception e) {
            log.warn("Impossible de convertir le DTO en JSON: {}", e.getMessage());
        }
        
        // Convertir DTO en entité Publication
        Publication publication = convertToPublication(publicationRequest, authenticatedUser);
        
        log.info("Publication convertie: status={}", publication.getStatus());
        log.info("Tags dans la publication convertie: {}", publication.getAiGeneratedTags());
        log.info("Nombre de tags dans la publication convertie: {}", 
                publication.getAiGeneratedTags() != null ? publication.getAiGeneratedTags().size() : 0);
        
        // Gérer l'image si fournie
        if (image != null && !image.isEmpty()) {
            String imageUrl = saveImage(image);
            publication.setImageUrl(imageUrl);
        }
        
        // Définir la date de programmation si fournie
        if (publicationRequest.getScheduledAt() != null) {
            publication.setScheduledAt(publicationRequest.getScheduledAt());
            // Si le statut n'est pas déjà défini, utiliser SCHEDULED
            if (publication.getStatus() == null) {
                publication.setStatus(PublicationStatus.SCHEDULED);
            }
            log.info("Publication après programmation: status={}", publication.getStatus());
        }
        
        // Log final avant sauvegarde
        log.info("Publication finale avant sauvegarde: status={}", publication.getStatus());
        
        Publication created = publicationService.createPublication(publication, authenticatedUser);
        PublicationDto createdDto = publicationMapper.toDto(created);
        createdDto.setCommentsCount((int) publicationService.getCommentsCount(created.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
    }
    
    @Operation(summary = "Récupérer toutes les publications avec pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des publications récupérée avec succès")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPublications(
            @Parameter(description = "Page (défaut: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (défaut: 10)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Tri par (défaut: createdAt)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Direction du tri (défaut: desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        
        Page<Publication> publications = publicationService.getAllPublications(page, size, sortBy, sortDir);
        
        // Convertir les entités en DTOs pour éviter les problèmes de sérialisation
        List<PublicationDto> publicationDtos = publicationMapper.toDtoList(publications.getContent());
        publicationDtos.forEach(dto -> dto.setCommentsCount((int) publicationService.getCommentsCount(dto.getId())));
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", publicationDtos);
        response.put("pagination", Map.of(
            "current_page", publications.getNumber(),
            "total_pages", publications.getTotalPages(),
            "total_items", publications.getTotalElements(),
            "items_per_page", publications.getSize()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Récupérer les publications filtrées")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publications filtrées récupérées avec succès")
    })
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> getFilteredPublications(
            @Parameter(description = "Statut de la publication") @RequestParam(required = false) PublicationStatus status,
            @Parameter(description = "Langue de la publication") @RequestParam(required = false) String language,
            @Parameter(description = "Publication épinglée") @RequestParam(required = false) Boolean isPinned,
            @Parameter(description = "ID du créateur") @RequestParam(required = false) Long createdBy,
            @Parameter(description = "Date de début") @RequestParam(required = false) String dateFrom,
            @Parameter(description = "Date de fin") @RequestParam(required = false) String dateTo,
            @Parameter(description = "Terme de recherche") @RequestParam(required = false) String search,
            @Parameter(description = "Page (défaut: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (défaut: 10)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Tri par (défaut: createdAt)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Direction du tri (défaut: desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        
        // Convertir les dates si fournies
        LocalDateTime fromDate = dateFrom != null ? parseDateTime(dateFrom) : null;
        LocalDateTime toDate = dateTo != null ? parseDateTime(dateTo) : null;
        
        Page<Publication> publications = publicationService.getFilteredPublications(
                status, language, isPinned, createdBy, fromDate, toDate, search, page, size, sortBy, sortDir);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", publications.getContent());
        response.put("pagination", Map.of(
            "current_page", publications.getNumber(),
            "total_pages", publications.getTotalPages(),
            "total_items", publications.getTotalElements(),
            "items_per_page", publications.getSize()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Récupérer une publication par son ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publication récupérée avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PublicationDto> getPublicationById(
            @Parameter(description = "ID de la publication") @PathVariable Long id) {
        
        Publication publication = publicationService.getPublicationById(id);
        PublicationDto publicationDto = publicationMapper.toDto(publication);
        publicationDto.setCommentsCount((int) publicationService.getCommentsCount(id));
        return ResponseEntity.ok(publicationDto);
    }
    
    @Operation(summary = "Récupérer une publication par son slug")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publication récupérée avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée")
    })
    @GetMapping("/slug/{slug}")
    public ResponseEntity<PublicationDto> getPublicationBySlug(
            @Parameter(description = "Slug de la publication") @PathVariable String slug) {
        
        Publication publication = publicationService.getPublicationBySlug(slug);
        PublicationDto publicationDto = publicationMapper.toDto(publication);
        publicationDto.setCommentsCount((int) publicationService.getCommentsCount(publication.getId()));
        return ResponseEntity.ok(publicationDto);
    }
    
    @Operation(summary = "Mettre à jour une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publication mise à jour avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<Publication> updatePublication(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Valid @RequestBody Publication publicationDetails,
            Authentication authentication) {
        
        Utilisateur currentUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            currentUser = utilisateurService.getUtilisateurByEmail(email).orElse(null);
        }
        
        Publication updated = publicationService.updatePublication(id, publicationDetails, currentUser);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Mettre à jour une publication avec image")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publication mise à jour avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<Publication> updatePublicationWithImage(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Parameter(description = "Données de la publication") @RequestPart("publication") String publicationJson,
            @Parameter(description = "Image de la publication") @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        Utilisateur currentUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            currentUser = utilisateurService.getUtilisateurByEmail(email).orElse(null);
        }

        Publication publicationDetails = parsePublicationFromJson(publicationJson);
        if (image != null && !image.isEmpty()) {
            String imageUrl = saveImage(image);
            publicationDetails.setImageUrl(imageUrl);
        }

        Publication updated = publicationService.updatePublication(id, publicationDetails, currentUser);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Mettre à jour le statut d'une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut de la publication mis à jour avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<Publication> updatePublicationStatus(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Utilisateur currentUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            currentUser = utilisateurService.getUtilisateurByEmail(email).orElse(null);
        }

        String statusValue = request.get("status");
        PublicationStatus status = PublicationStatus.valueOf(statusValue.toUpperCase());
        String rejectionReason = request.get("rejection_reason");

        Publication updated = publicationService.updatePublicationStatus(id, status, rejectionReason, currentUser);
        return ResponseEntity.ok(updated);
    }
    
    @Operation(summary = "Valider une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publication validée avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PutMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Publication> validatePublication(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur currentUser) {
        
        Publication validated = publicationService.validatePublication(id, currentUser);
        return ResponseEntity.ok(validated);
    }
    
    @Operation(summary = "Rejeter une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publication rejetée avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Publication> rejectPublication(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Parameter(description = "Raison du rejet") @RequestParam String rejectionReason,
            @AuthenticationPrincipal Utilisateur currentUser) {
        
        Publication rejected = publicationService.rejectPublication(id, rejectionReason, currentUser);
        return ResponseEntity.ok(rejected);
    }
    
    @Operation(summary = "Basculer le statut épinglé d'une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut épinglé modifié avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PutMapping("/{id}/pin")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<Publication> togglePinPublication(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur currentUser) {
        
        Publication updated = publicationService.togglePinPublication(id);
        return ResponseEntity.ok(updated);
    }
    
    @Operation(summary = "Archiver une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publication archivée avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Publication> archivePublication(
            @Parameter(description = "ID de la publication") @PathVariable Long id) {
        
        Publication archived = publicationService.archivePublication(id);
        return ResponseEntity.ok(archived);
    }
    
    @Operation(summary = "Supprimer une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Publication supprimée avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<Void> deletePublication(
            @Parameter(description = "ID de la publication") @PathVariable Long id) {
        
        publicationService.deletePublication(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Interagir avec une publication (like/dislike/favorite)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Interaction enregistrée avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée")
    })
    @PostMapping("/{id}/interact")
    public ResponseEntity<Publication> interactWithPublication(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Parameter(description = "Type d'interaction") @RequestParam String interactionType) {
        
        Publication updated = publicationService.handleInteraction(id, interactionType);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Recuperer les commentaires d'une publication")
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<PublicationCommentDto>> getPublicationComments(
            @Parameter(description = "ID de la publication") @PathVariable Long id) {
        return ResponseEntity.ok(publicationService.getCommentsByPublication(id));
    }

    @Operation(summary = "Ajouter un commentaire a une publication")
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addPublicationComment(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Valid @RequestBody PublicationCommentDto.CreateCommentRequest request) {
        try {
            PublicationCommentDto created = publicationService.addComment(id, request.getContent());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    @Operation(summary = "Modifier son propre commentaire")
    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<?> updatePublicationComment(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Parameter(description = "ID du commentaire") @PathVariable Long commentId,
            @Valid @RequestBody PublicationCommentDto.UpdateCommentRequest request) {
        try {
            PublicationCommentDto updated = publicationService.updateComment(id, commentId, request.getContent());
            return ResponseEntity.ok(updated);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    @Operation(summary = "Supprimer son propre commentaire")
    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deletePublicationComment(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Parameter(description = "ID du commentaire") @PathVariable Long commentId) {
        try {
            publicationService.deleteComment(id, commentId);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Signaler un commentaire")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalement du commentaire enregistré avec succès"),
        @ApiResponse(responseCode = "403", description = "Non autorisé"),
        @ApiResponse(responseCode = "404", description = "Publication ou commentaire non trouvé")
    })
    @PostMapping("/{id}/comments/{commentId}/report")
    public ResponseEntity<?> reportPublicationComment(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Parameter(description = "ID du commentaire") @PathVariable Long commentId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.getOrDefault("reason", "") : "";
            publicationService.reportComment(id, commentId, reason);
            return ResponseEntity.ok(Map.of("message", "Signalement du commentaire enregistré"));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Erreur lors du signalement du commentaire {} de la publication {}: {}", commentId, id, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur interne lors du signalement du commentaire"));
        }
    }

    @Operation(summary = "Bloquer l'auteur d'un commentaire")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur bloque avec succes"),
        @ApiResponse(responseCode = "400", description = "Duree invalide"),
        @ApiResponse(responseCode = "403", description = "Non autorise"),
        @ApiResponse(responseCode = "404", description = "Publication ou commentaire non trouve")
    })
    @PostMapping("/{id}/comments/{commentId}/block-author")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> blockPublicationCommentAuthor(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @Parameter(description = "ID du commentaire") @PathVariable Long commentId,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            int durationHours = 24;
            if (request != null && request.get("durationHours") != null) {
                Object raw = request.get("durationHours");
                if (raw instanceof Number number) {
                    durationHours = number.intValue();
                } else {
                    durationHours = Integer.parseInt(String.valueOf(raw));
                }
            }
            String reason = request != null ? String.valueOf(request.getOrDefault("reason", "")) : "";
            publicationService.blockCommentAuthor(id, commentId, durationHours, reason);
            return ResponseEntity.ok(Map.of("message", "Utilisateur bloque avec succes"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Erreur lors du blocage de l'auteur du commentaire {} de la publication {}: {}", commentId, id, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur interne lors du blocage"));
        }
    }

    @Operation(summary = "Debloquer un utilisateur pour les commentaires")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur debloque avec succes"),
        @ApiResponse(responseCode = "403", description = "Non autorise"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouve")
    })
    @PostMapping("/users/{userId}/unblock-comments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unblockUserComments(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Integer userId) {
        try {
            publicationService.unblockUserComments(userId);
            return ResponseEntity.ok(Map.of("message", "Utilisateur debloque avec succes"));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Erreur lors du deblocage de l'utilisateur {}: {}", userId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur interne lors du deblocage"));
        }
    }
    
    @Operation(summary = "Incrémenter le compteur de vues")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vue enregistrée avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée")
    })
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementViews(
            @Parameter(description = "ID de la publication") @PathVariable Long id) {
        
        publicationService.incrementViewsCount(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Signaler une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalement enregistré avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "403", description = "Non autorisé")
    })
    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportPublication(
            @Parameter(description = "ID de la publication") @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.getOrDefault("reason", "") : "";
            publicationService.reportPublication(id, reason);
            return ResponseEntity.ok(Map.of("message", "Signalement enregistré"));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Erreur lors du signalement de la publication {}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur interne lors du signalement"));
        }
    }

    @Operation(summary = "Récupérer les signalements d'une publication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalements récupérés avec succès"),
        @ApiResponse(responseCode = "404", description = "Publication non trouvée")
    })
    @GetMapping("/{id}/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPublicationReports(
            @Parameter(description = "ID de la publication") @PathVariable Long id) {
        try {
            List<PublicationReportDto> reports = publicationService.getReportsByPublication(id);
            List<Map<String, Object>> commentReports = publicationService.getCommentReportsByPublication(id);
            return ResponseEntity.ok(Map.of(
                    "publicationReports", reports,
                    "commentReports", commentReports,
                    "totalReports", reports.size() + commentReports.size()
            ));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Erreur lors du chargement des signalements publication {}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du chargement des signalements"));
        }
    }
    
    @Operation(summary = "Récupérer les publications épinglées")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publications épinglées récupérées avec succès")
    })
    @GetMapping("/pinned")
    public ResponseEntity<List<Publication>> getPinnedPublications() {
        
        List<Publication> publications = publicationService.getPinnedPublications();
        return ResponseEntity.ok(publications);
    }
    
    @Operation(summary = "Récupérer les publications récentes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publications récentes récupérées avec succès")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<Publication>> getRecentPublications(
            @Parameter(description = "Limite (défaut: 10)") @RequestParam(defaultValue = "10") int limit) {
        
        List<Publication> publications = publicationService.getRecentPublications(limit);
        return ResponseEntity.ok(publications);
    }
    
    @Operation(summary = "Récupérer les publications populaires")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publications populaires récupérées avec succès")
    })
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularPublications(
            @Parameter(description = "Page (défaut: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (défaut: 10)") @RequestParam(defaultValue = "10") int size) {
        
        Page<Publication> publications = publicationService.getPopularPublications(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", publications.getContent());
        response.put("pagination", Map.of(
            "current_page", publications.getNumber(),
            "total_pages", publications.getTotalPages(),
            "total_items", publications.getTotalElements(),
            "items_per_page", publications.getSize()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Récupérer les publications en attente de validation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publications en attente récupérées avec succès"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @GetMapping("/pending-validation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Publication>> getPendingValidationPublications() {
        
        List<Publication> publications = publicationService.getPendingValidationPublications();
        return ResponseEntity.ok(publications);
    }
    
    @Operation(summary = "Récupérer les statistiques des publications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Object[]>> getPublicationsStatistics() {
        
        List<Object[]> statistics = publicationService.getPublicationsStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    @Operation(summary = "Rechercher des publications par tag")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publications trouvées avec succès")
    })
    @GetMapping("/search/tag")
    public ResponseEntity<List<Publication>> searchPublicationsByTag(
            @Parameter(description = "Tag de recherche") @RequestParam String tag) {
        
        List<Publication> publications = publicationService.searchPublicationsByTag(tag);
        return ResponseEntity.ok(publications);
    }
    
    @Operation(summary = "Récupérer les publications créées par un utilisateur")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publications récupérées avec succès")
    })
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<Map<String, Object>> getPublicationsByCreator(
            @Parameter(description = "ID du créateur") @PathVariable Long creatorId,
            @Parameter(description = "Page (défaut: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (défaut: 10)") @RequestParam(defaultValue = "10") int size) {
        
        Page<Publication> publications = publicationService.getPublicationsByCreator(creatorId, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", publications.getContent());
        response.put("pagination", Map.of(
            "current_page", publications.getNumber(),
            "total_pages", publications.getTotalPages(),
            "total_items", publications.getTotalElements(),
            "items_per_page", publications.getSize()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    // Méthodes utilitaires privées
    private Publication convertToPublication(CreatePublicationRequest request, Utilisateur currentUser) {
        Publication publication = new Publication();
        publication.setTitle(request.getTitle());
        publication.setSummary(request.getSummary());
        publication.setContent(request.getContent());
        publication.setImageUrl(request.getImageUrl());
        publication.setLanguage(request.getLanguage());
        publication.setIsPinned(request.getIsPinned());
        publication.setAiGeneratedTags(request.getAiGeneratedTags());
        
        // Définir le statut si fourni dans la requête
        if (request.getStatus() != null) {
            try {
                PublicationStatus status = PublicationStatus.valueOf(request.getStatus().toUpperCase());
                publication.setStatus(status);
            } catch (IllegalArgumentException e) {
                // Si le statut n'est pas valide, utiliser DRAFT par défaut
                publication.setStatus(PublicationStatus.DRAFT);
            }
        }
        
        // Utiliser l'utilisateur authentifié ou null
        publication.setCreatedBy(currentUser);
        
        return publication;
    }
    
    private Publication parsePublicationFromJson(String json) {
        try {
            return objectMapper.readValue(json, Publication.class);
        } catch (Exception e) {
            log.error("Erreur lors du parsing JSON de la publication: {}", e.getMessage());
            throw new RuntimeException("Format JSON invalide pour la publication", e);
        }
    }
    
    private String saveImage(MultipartFile image) {
        try {
            String imageUrl = uploadStorageService.storePublicationImage(image);
            log.info("Image publication sauvegardee: {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de l'image: {}", e.getMessage());
            return null;
        }
    }
    
    private LocalDateTime parseDateTime(String dateTimeString) {
        try {
            return LocalDateTime.parse(dateTimeString);
        } catch (Exception e) {
            return null;
        }
    }
}
