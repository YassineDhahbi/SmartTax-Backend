package tn.esprit.arabsoftback.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.dto.DownloadLibraryDocumentDto;
import tn.esprit.arabsoftback.service.DownloadLibraryDocumentService;
import tn.esprit.arabsoftback.service.PreparedDownload;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/download-documents")
@RequiredArgsConstructor
public class DownloadLibraryDocumentController {

    private final DownloadLibraryDocumentService service;

    @GetMapping
    public ResponseEntity<List<DownloadLibraryDocumentDto>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping("/{id}/record-download")
    public ResponseEntity<Void> recordDownload(@PathVariable Long id) {
        try {
            service.incrementDownloadCount(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            PreparedDownload p = service.prepareDownload(id);
            service.incrementDownloadCount(id);
            String fn = p.fileName() != null && !p.fileName().isBlank() ? p.fileName() : "document";
            String safe = fn.replace("\"", "");
            MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
            if (p.contentType() != null && !p.contentType().isBlank()) {
                try {
                    mt = MediaType.parseMediaType(p.contentType());
                } catch (Exception ignored) {
                    // keep octet-stream
                }
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safe + "\"")
                    .contentType(mt)
                    .body(p.resource());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<?> create(
            @RequestParam("categoryId") String categoryId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "downloadUrl", required = false) String downloadUrl,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            DownloadLibraryDocumentDto dto = service.create(categoryId, title, description, downloadUrl, file);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur lors de l'enregistrement du fichier"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
