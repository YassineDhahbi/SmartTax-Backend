package tn.esprit.arabsoftback.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OcrServiceProcessManager {

    @Value("${ocr.service.enabled:true}")
    private boolean ocrEnabled;

    @Value("${ocr.service.autostart:true}")
    private boolean ocrAutostart;

    @Value("${ocr.service.url:http://localhost:8004}")
    private String ocrServiceUrl;

    @Value("${ocr.service.python-executable:python}")
    private String pythonExecutable;

    @Value("${ocr.service.working-dir:../ocr-service}")
    private String ocrWorkingDir;

    @Value("${ocr.service.script:real_cin_reader.py}")
    private String ocrScript;

    @Value("${ocr.service.startup-wait-seconds:45}")
    private int startupWaitSeconds;

    private Process ocrProcess;

    @EventListener(ApplicationReadyEvent.class)
    public void startOcrServiceIfNeeded() {
        if (!ocrEnabled || !ocrAutostart) {
            log.info("OCR service autostart disabled");
            return;
        }

        if (isOcrServiceHealthy()) {
            log.info("OCR service already running at {}", ocrServiceUrl);
            return;
        }

        try {
            Path workingDirPath = resolveWorkingDirectory();
            Path scriptPath = workingDirPath.resolve(ocrScript);
            if (!Files.isRegularFile(scriptPath)) {
                log.warn("OCR script not found at {} � autostart skipped", scriptPath);
                return;
            }

            List<String> command = new ArrayList<>();
            command.add(resolvePythonExecutable(workingDirPath));
            command.add(ocrScript);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDirPath.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            ocrProcess = pb.start();

            log.info("OCR service starting from {} with command {}", workingDirPath, command);
            waitUntilHealthy();
        } catch (IOException ex) {
            log.warn("Failed to autostart OCR service: {}", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("OCR service startup wait interrupted");
        }
    }

    @PreDestroy
    public void stopOcrServiceIfStartedByBackend() {
        if (ocrProcess != null && ocrProcess.isAlive()) {
            ocrProcess.destroy();
            log.info("OCR service process stopped");
        }
    }

    private void waitUntilHealthy() throws InterruptedException {
        for (int second = 0; second < startupWaitSeconds; second++) {
            if (isOcrServiceHealthy()) {
                log.info("OCR service ready at {} (after {}s)", ocrServiceUrl, second);
                return;
            }
            Thread.sleep(1000);
        }
        log.warn("OCR service not responding on {} after {}s � verify Python dependencies in ocr-service",
                ocrServiceUrl, startupWaitSeconds);
    }

    private boolean isOcrServiceHealthy() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(ocrServiceUrl + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            return false;
        }
    }

    private String resolvePythonExecutable(Path workingDirPath) {
        Path configured = Paths.get(pythonExecutable);
        if (configured.isAbsolute() && Files.exists(configured)) {
            return configured.normalize().toString();
        }

        Path venvWindows = workingDirPath.resolve(".venv/Scripts/python.exe");
        if (Files.exists(venvWindows)) {
            return venvWindows.normalize().toString();
        }

        Path venvUnix = workingDirPath.resolve(".venv/bin/python");
        if (Files.exists(venvUnix)) {
            return venvUnix.normalize().toString();
        }

        return pythonExecutable;
    }

    private Path resolveWorkingDirectory() {
        Path configuredPath = Paths.get(ocrWorkingDir);
        if (configuredPath.isAbsolute() && configuredPath.toFile().exists()) {
            return configuredPath.normalize();
        }

        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();

        List<Path> candidates = List.of(
                userDir.resolve(ocrWorkingDir),
                userDir.resolve("ocr-service"),
                userDir.resolve("..").resolve("ocr-service").normalize(),
                userDir.resolve("ArabSoftBack").resolve("..").resolve("ocr-service").normalize()
        );

        for (Path candidate : candidates) {
            if (candidate.toFile().exists() && Files.isRegularFile(candidate.resolve(ocrScript))) {
                return candidate.normalize();
            }
        }

        return userDir.resolve(ocrWorkingDir).normalize();
    }
}
