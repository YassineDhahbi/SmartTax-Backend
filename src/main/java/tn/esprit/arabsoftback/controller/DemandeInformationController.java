package tn.esprit.arabsoftback.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.arabsoftback.dto.DemandeInformationDto;
import tn.esprit.arabsoftback.dto.DemandeInformationStatsDto;
import tn.esprit.arabsoftback.service.DemandeInformationService;

@RestController
@RequestMapping("/api/demande-information")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class DemandeInformationController {

    private final DemandeInformationService demandeInformationService;

    @PostMapping("/public")
    public ResponseEntity<DemandeInformationDto.CreateResponse> createPublicDemande(
            @Valid @RequestBody DemandeInformationDto.CreateRequest request
    ) {
        DemandeInformationDto.CreateResponse response = demandeInformationService.createPublicRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<DemandeInformationDto.ListResponse> getAllDemandes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String traitement,
            @RequestParam(required = false) String urgence,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) Integer agentId
    ) {
        if (page == null) {
            return ResponseEntity.ok(demandeInformationService.getAllDemandes());
        }
        int s = size != null ? size : 10;
        DemandeInformationDto.ListResponse body = demandeInformationService.getDemandesPaged(
                page,
                s,
                search,
                traitement,
                urgence,
                section,
                agentId
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/stats")
    public ResponseEntity<DemandeInformationStatsDto> getStats() {
        return ResponseEntity.ok(demandeInformationService.getStats());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDemande(@PathVariable Long id) {
        demandeInformationService.deleteDemande(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/traitement-status")
    public ResponseEntity<DemandeInformationDto.ItemResponse> updateTraitementStatus(
            @PathVariable Long id,
            @Valid @RequestBody DemandeInformationDto.UpdateTraitementStatusRequest request
    ) {
        DemandeInformationDto.ItemResponse response =
                demandeInformationService.updateTraitementStatus(id, request.getTraitementStatus());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/assign-agent")
    public ResponseEntity<DemandeInformationDto.ItemResponse> assignAgent(
            @PathVariable Long id,
            @RequestBody DemandeInformationDto.AssignAgentRequest request
    ) {
        DemandeInformationDto.ItemResponse response =
                demandeInformationService.assignAgent(id, request != null ? request.getAgentId() : null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeInformationDto.ItemResponse> getDemandeById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeInformationService.getDemandeById(id));
    }
}

