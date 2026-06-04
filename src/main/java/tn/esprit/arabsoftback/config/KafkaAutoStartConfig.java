package tn.esprit.arabsoftback.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Component
@Slf4j
public class KafkaAutoStartConfig {

    @Value("${app.kafka.autostart:true}")
    private boolean kafkaAutoStart;

    @Value("${app.kafka.compose-file:docker-compose.kafka.yml}")
    private String composeFile;

    @PostConstruct
    public void startKafkaIfEnabled() {
        if (!kafkaAutoStart) {
            log.info("Kafka autostart disabled (app.kafka.autostart=false)");
            return;
        }

        try {
            String workingDir = System.getProperty("user.dir");
            Path composePath = Path.of(composeFile);
            if (!composePath.isAbsolute()) {
                composePath = Path.of(workingDir).resolve(composePath);
            }

            if (!composePath.toFile().exists()) {
                log.warn("Kafka compose file not found: {}", composePath);
                return;
            }

            List<String> command;
            if (isWindows()) {
                command = List.of(
                        "powershell",
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-Command",
                        "docker compose -f \"" + composePath + "\" up -d"
                );
            } else {
                command = List.of(
                        "sh",
                        "-c",
                        "docker compose -f '" + composePath + "' up -d"
                );
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(workingDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Kafka autostart command executed successfully.");
            } else {
                log.warn("Kafka autostart command failed with exit code {}", exitCode);
            }
        } catch (Exception ex) {
            log.warn("Kafka autostart skipped due to error: {}", ex.getMessage());
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
