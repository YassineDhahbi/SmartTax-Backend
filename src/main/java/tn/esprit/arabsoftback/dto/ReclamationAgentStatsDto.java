package tn.esprit.arabsoftback.dto;

/**
 * Agr�gats pour le tableau de bord agent (r�clamations filtr�es par statut, ex. SOUMIS).
 */
public record ReclamationAgentStatsDto(
        long totalSoumises,
        long etatEnCours,
        long etatTraite,
        long prioriteHaute
) {}
