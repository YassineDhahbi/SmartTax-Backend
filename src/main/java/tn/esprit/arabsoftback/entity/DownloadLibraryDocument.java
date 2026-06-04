package tn.esprit.arabsoftback.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "download_library_document", indexes = {
        @Index(name = "idx_download_doc_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DownloadLibraryDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DownloadDocumentCategory category;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Lien externe (HTTPS) si pas de fichier stock� */
    @Column(name = "external_url", length = 2000)
    private String externalUrl;

    /** Nom du fichier sur disque (sous {@code app.download-documents.storage-dir}), sans chemin absolu */
    @Column(name = "stored_file_name", length = 500)
    private String storedFileName;

    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "download_count", nullable = false)
    private long downloadCount = 0;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasStoredFile() {
        return storedFileName != null && !storedFileName.isBlank();
    }
}
