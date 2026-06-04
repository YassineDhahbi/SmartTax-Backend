package tn.esprit.arabsoftback.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

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
public class SentimentServiceProcessManager {

    @Value("${sentiment.service.enabled:true}")
    private boolean sentimentEnabled;

    @Value("${sentiment.service.autostart:true}")
    private boolean sentimentAutostart;

    @Value("${sentiment.service.url:http://localhost:8010}")
    private String sentimentServiceUrl;

    @Value("${sentiment.service.python-executable:python}")
    private String pythonExecutable;

    @Value("${sentiment.service.working-dir:sentiment-service}")
    private String sentimentWorkingDir;

    @Value("${sentiment.service.host:0.0.0.0}")
    private String sentimentHost;

    @Value("${sentiment.service.port:8010}")
    private int sentimentPort;

    private Process sentimentProcess;

    @EventListener(ApplicationReadyEvent.class)
    public void startSentimentServiceIfNeeded() {
        if (!sentimentEnabled || !sentimentAutostart) {
            log.info("Sentiment service autostart disabled");
            return;
        }

        if (isSentimentServiceHealthy()) {
            log.info("Sentiment service already running at {}", sentimentServiceUrl);
            return;
        }

        try {
            Path workingDirPath = Paths.get(sentimentWorkingDir);
            if (!workingDirPath.isAbsolute()) {
                workingDirPath = Paths.get(System.getProperty("user.dir")).resolve(sentimentWorkingDir);
            }
            workingDirPath = workingDirPath.normalize();

            List<String> command = new ArrayList<>();
            command.add(pythonExecutable);
            command.add("-m");
            command.add("uvicorn");
            command.add("app:app");
            command.add("--host");
            command.add(sentimentHost);
            command.add("--port");
            command.add(String.valueOf(sentimentPort));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDirPath.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            sentimentProcess = pb.start();

            log.info("Sentiment service started from {} with command {}", workingDirPath, command);
        } catch (IOException ex) {
            log.warn("Failed to autostart sentiment service: {}", ex.getMessage());
        }
    }

    @PreDestroy
    public void stopSentimentServiceIfStartedByBackend() {
        if (sentimentProcess != null && sentimentProcess.isAlive()) {
            sentimentProcess.destroy();
            log.info("Sentiment service process stopped");
        }
    }

    private boolean isSentimentServiceHealthy() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(sentimentServiceUrl + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            return false;
        }
    }
}
