package tn.esprit.arabsoftback.entity;

import java.util.Locale;

/**
 * Rubriques fixes du centre de telechargement (aligne avec le front agent).
 */
public enum DownloadDocumentCategory {
    FORMULAIRES,
    GUIDES,
    LOIS,
    MODELES;

    public static DownloadDocumentCategory fromApiValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Categorie obligatoire");
        }
        return DownloadDocumentCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    /** Identifiant API / front : formulaires, guides, lois, modeles */
    public String toApiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
