package tn.esprit.arabsoftback.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Garantit la colonne {@code download_count} sur {@code download_library_document}.
 * Sans Flyway, une base creee avant cette colonne provoquait des erreurs SQL au chargement du catalogue.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DownloadLibraryDownloadCountStartupPatch implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "ALTER TABLE download_library_document "
                            + "ADD COLUMN IF NOT EXISTS download_count BIGINT NOT NULL DEFAULT 0");
            log.debug("Schema download_library_document.download_count verifiee / appliquee.");
        } catch (Exception e) {
            log.warn(
                    "Impossible d'appliquer le patch download_library_document.download_count "
                            + "(table absente ou droits SQL): {}",
                    e.getMessage());
        }
    }
}
