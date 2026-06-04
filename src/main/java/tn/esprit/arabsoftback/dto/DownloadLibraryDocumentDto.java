package tn.esprit.arabsoftback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadLibraryDocumentDto {
    private Long id;
    /** formulaires | guides | lois | modeles */
    private String categoryId;
    private String title;
    private String description;
    /** ISO-8601 */
    private String updatedAt;
    /**
     * URL absolue pour t�l�chargement / ouverture : lien externe ou endpoint fichier API.
     */
    private String downloadUrl;
    private String originalFileName;
    private String mimeType;
    private Long sizeBytes;
    /** Nombre de t�l�chargements enregistr�s (fichier servi par l�API ou clic lien externe). */
    private Long downloadCount;
}
