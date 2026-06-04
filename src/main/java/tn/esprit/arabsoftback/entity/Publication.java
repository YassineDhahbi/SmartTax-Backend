package tn.esprit.arabsoftback.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "publication")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Publication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(unique = true, nullable = false)
    private String slug;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;
    
    private String imageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationStatus status = PublicationStatus.DRAFT;
    
    @Column(nullable = false)
    private Boolean isPinned = false;
    
    @Column(nullable = false)
    private Integer viewsCount = 0;
    
    @Column(nullable = false)
    private Integer likesCount = 0;
    
    @Column(nullable = false)
    private Integer dislikesCount = 0;
    
    @Column(nullable = false)
    private Integer favoritesCount = 0;
    
    @Column(nullable = false)
    private Integer reportsCount = 0;
    
    @ElementCollection
    @CollectionTable(name = "publication_tags", joinColumns = @JoinColumn(name = "publication_id"))
    @Column(name = "tag")
    private List<String> aiGeneratedTags;
    
    @Column(nullable = false)
    private Double sentimentScore = 0.0;
    
    @Column(nullable = false)
    private String language = "fr";
    
    @Column(nullable = false)
    private Boolean isArchived = false;
    
    @Column(nullable = false)
    private Boolean isDeleted = false;
    
    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnore
    private Utilisateur createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    @JsonIgnore
    private Utilisateur validatedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @JsonIgnore
    private Utilisateur updatedBy;
    
    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // Additional fields
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Generate slug from title if not provided
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(title);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private String generateSlug(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "publication-" + System.currentTimeMillis();
        }
        
        return title.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                + "-" + System.currentTimeMillis();
    }
    
    public enum PublicationStatus {
        DRAFT,
        PENDING,
        VALIDATED,
        PUBLISHED,
        SCHEDULED,
        REJECTED,
        ARCHIVED,
        DELETED
    }
}
