package tn.esprit.arabsoftback.dto;

public record DemandeInformationStatsDto(
        long total,
        long traitees,
        long nonTraitees,
        long urgentes
) {}
