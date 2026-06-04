package tn.esprit.arabsoftback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.dto.DownloadLibraryDocumentDto;
import tn.esprit.arabsoftback.entity.DownloadDocumentCategory;
import tn.esprit.arabsoftback.entity.DownloadLibraryDocument;
import tn.esprit.arabsoftback.repository.DownloadLibraryDocumentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadLibraryDocumentService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DownloadLibraryDocumentRepository repository;

    @Value("${app.download-documents.storage-dir:uploads/download-documents}")
    private String storageDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional(readOnly = true)
    public List<DownloadLibraryDocumentDto> listAll() {
        return repository.findAllByOrderByCategoryAscUpdatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DownloadLibraryDocument getEntityOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable"));
    }

    @Transactional
    public void incrementDownloadCount(long id) {
        int updated = repository.incrementDownloadCount(id);
        if (updated == 0) {
            throw new IllegalArgumentException("Document introuvable");
        }
    }

    @Transactional(readOnly = true)
    public PreparedDownload prepareDownload(long id) throws IOException {
        DownloadLibraryDocument doc = getEntityOrThrow(id);
        if (!doc.hasStoredFile()) {
            throw new IllegalArgumentException("Pas de fichier stocke");
        }
        Path path = Paths.get(storageDir).toAbsolutePath().normalize().resolve(doc.getStoredFileName());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IOException("Fichier manquant sur le disque");
        }
        PreparedDownload pd = PreparedDownload.of(path, doc.getOriginalFileName(), doc.getContentType());
        if (!pd.resource().exists() || !pd.resource().isReadable()) {
            throw new IOException("Fichier illisible");
        }
        return pd;
    }

    @Transactional
    public DownloadLibraryDocumentDto create(
            String categoryId,
            String title,
            String description,
            String downloadUrl,
            MultipartFile file
    ) throws IOException {
        DownloadDocumentCategory category = DownloadDocumentCategory.fromApiValue(categoryId);
        String t = title != null ? title.trim() : "";
        if (!StringUtils.hasText(t)) {
            throw new IllegalArgumentException("Titre obligatoire");
        }
        String extUrl = downloadUrl != null ? downloadUrl.trim() : "";
        boolean hasFile = file != null && !file.isEmpty();
        if (!StringUtils.hasText(extUrl) && !hasFile) {
            throw new IllegalArgumentException("Fournir un fichier ou un lien HTTPS");
        }
        if (StringUtils.hasText(extUrl) && hasFile) {
            throw new IllegalArgumentException("Choisir soit un fichier soit un lien, pas les deux");
        }

        DownloadLibraryDocument entity = new DownloadLibraryDocument();
        entity.setCategory(category);
        entity.setTitle(t);
        entity.setDescription(description != null ? description.trim() : null);

        if (hasFile) {
            assertPdfUpload(file);
            String stored = saveUploadedFile(file);
            entity.setStoredFileName(stored);
            entity.setOriginalFileName(file.getOriginalFilename());
            entity.setContentType(file.getContentType());
            entity.setFileSizeBytes(file.getSize());
            entity.setExternalUrl(null);
        } else {
            if (!extUrl.startsWith("http://") && !extUrl.startsWith("https://")) {
                throw new IllegalArgumentException("Le lien doit commencer par http:// ou https://");
            }
            entity.setExternalUrl(extUrl);
            entity.setStoredFileName(null);
            entity.setOriginalFileName(null);
            entity.setContentType(null);
            entity.setFileSizeBytes(null);
        }

        DownloadLibraryDocument saved = repository.save(entity);
        log.info("Document telechargement cree id={} categorie={}", saved.getId(), category);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        DownloadLibraryDocument doc = getEntityOrThrow(id);
        if (doc.hasStoredFile()) {
            Path path = Paths.get(storageDir).toAbsolutePath().normalize().resolve(doc.getStoredFileName());
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Impossible de supprimer le fichier disque {} : {}", path, e.getMessage());
            }
        }
        repository.delete(doc);
        log.info("Document telechargement supprime id={}", id);
    }

    private String saveUploadedFile(MultipartFile file) throws IOException {
        Path base = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(base);
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String stored = UUID.randomUUID() + "_" + safe;
        Path target = base.resolve(stored);
        file.transferTo(target.toFile());
        return stored;
    }

    private static void assertPdfUpload(MultipartFile file) {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!original.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Seuls les fichiers PDF (.pdf) sont acceptes.");
        }
        String ct = file.getContentType();
        if (!StringUtils.hasText(ct)) {
            return;
        }
        String c = ct.toLowerCase(Locale.ROOT);
        if (c.contains("pdf") || "application/octet-stream".equals(c)) {
            return;
        }
        throw new IllegalArgumentException("Le fichier doit etre au format PDF.");
    }

    private DownloadLibraryDocumentDto toDto(DownloadLibraryDocument e) {
        String downloadUrl;
        if (StringUtils.hasText(e.getExternalUrl())) {
            downloadUrl = e.getExternalUrl();
        } else if (e.hasStoredFile()) {
            downloadUrl = baseUrl.replaceAll("/$", "") + "/api/download-documents/" + e.getId() + "/file";
        } else {
            downloadUrl = null;
        }
        return DownloadLibraryDocumentDto.builder()
                .id(e.getId())
                .categoryId(e.getCategory().toApiValue())
                .title(e.getTitle())
                .description(e.getDescription())
                .updatedAt(e.getUpdatedAt() != null ? ISO.format(e.getUpdatedAt()) : null)
                .downloadUrl(downloadUrl)
                .originalFileName(e.getOriginalFileName())
                .mimeType(e.getContentType())
                .sizeBytes(e.getFileSizeBytes())
                .downloadCount(e.getDownloadCount())
                .build();
    }
}
