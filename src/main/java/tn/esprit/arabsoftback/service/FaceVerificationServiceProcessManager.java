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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FaceVerificationServiceProcessManager {

    @Value("${face.verification.service.enabled:true}")
    private boolean faceVerificationEnabled;

    @Value("${face.verification.service.autostart:true}")
    private boolean faceVerificationAutostart;

    @Value("${face.verification.service.url:http://localhost:8005}")
    private String faceVerificationServiceUrl;

    @Value("${face.verification.service.python-executable:python}")
    private String pythonExecutable;

    @Value("${face.verification.service.working-dir:face-verification-service}")
    private String faceVerificationWorkingDir;

    private Process faceVerificationProcess;

    @EventListener(ApplicationReadyEvent.class)
    public void startFaceVerificationServiceIfNeeded() {
        if (!faceVerificationEnabled || !faceVerificationAutostart) {
            log.info("Face verification service autostart disabled");
            return;
        }

        if (isFaceVerificationServiceHealthy()) {
            log.info("Face verification service already running at {}", faceVerificationServiceUrl);
            return;
        }

        try {
            Path workingDirPath = resolveWorkingDirectory();

            List<String> command = new ArrayList<>();
            command.add(pythonExecutable);
            command.add("app.py");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDirPath.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            faceVerificationProcess = pb.start();

            log.info("Face verification service started from {} with command {}", workingDirPath, command);
        } catch (IOException ex) {
            log.warn("Failed to autostart face verification service: {}", ex.getMessage());
        }
    }

    @PreDestroy
    public void stopFaceVerificationServiceIfStartedByBackend() {
        if (faceVerificationProcess != null && faceVerificationProcess.isAlive()) {
            faceVerificationProcess.destroy();
            log.info("Face verification service process stopped");
        }
    }

    private boolean isFaceVerificationServiceHealthy() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(faceVerificationServiceUrl + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            return false;
        }
    }

    private Path resolveWorkingDirectory() {
        Path configuredPath = Paths.get(faceVerificationWorkingDir);
        if (configuredPath.isAbsolute() && configuredPath.toFile().exists()) {
            return configuredPath.normalize();
        }

        Path userDir = Paths.get(System.getProperty("user.dir"));
        Path relativeToUserDir = userDir.resolve(faceVerificationWorkingDir).normalize();
        if (relativeToUserDir.toFile().exists()) {
            return relativeToUserDir;
        }

        Path fromRepoRoot = userDir.resolve("ArabSoftBack").resolve(faceVerificationWorkingDir).normalize();
        if (fromRepoRoot.toFile().exists()) {
            return fromRepoRoot;
        }

        return relativeToUserDir;
    }
}
