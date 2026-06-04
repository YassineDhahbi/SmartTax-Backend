package tn.esprit.arabsoftback.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublicationSchemaFixConfig {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensurePublicationStatusConstraintIncludesScheduled() {
        try {
            // PostgreSQL-specific schema fix: include SCHEDULED in publication status check constraint.
            jdbcTemplate.execute(
                "DO $$ " +
                "BEGIN " +
                "  IF EXISTS ( " +
                "    SELECT 1 FROM pg_constraint c " +
                "    JOIN pg_class t ON c.conrelid = t.oid " +
                "    WHERE t.relname = 'publication' AND c.conname = 'publication_status_check' " +
                "  ) THEN " +
                "    ALTER TABLE publication DROP CONSTRAINT publication_status_check; " +
                "  END IF; " +
                "  ALTER TABLE publication " +
                "    ADD CONSTRAINT publication_status_check " +
                "    CHECK (status IN ('DRAFT', 'PENDING', 'VALIDATED', 'PUBLISHED', 'SCHEDULED', 'REJECTED', 'ARCHIVED', 'DELETED')); " +
                "END $$;"
            );
            log.info("? Contrainte publication_status_check mise � jour avec le statut SCHEDULED");
        } catch (Exception e) {
            log.error("? Erreur lors de la mise � jour de publication_status_check", e);
        }
    }
}
