package tn.esprit.arabsoftback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisClient {

    @Value("${sentiment.service.enabled:true}")
    private boolean sentimentServiceEnabled;

    @Value("${sentiment.service.url:http://localhost:8010}")
    private String sentimentServiceUrl;

    @Value("${sentiment.service.timeout-ms:6000}")
    private int timeoutMs;

    private final RestTemplate restTemplate = new RestTemplate();

    public SentimentResult analyze(String text) {
        if (!sentimentServiceEnabled) {
            return SentimentResult.neutral("disabled");
        }
        if (text == null || text.isBlank()) {
            return SentimentResult.neutral("empty");
        }

        String endpoint = sentimentServiceUrl + "/analyze";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of("text", text, "timeout_ms", timeoutMs), headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Sentiment service responded with non-success status: {}", response.getStatusCode());
                return SentimentResult.neutral("http-status");
            }

            Map<String, Object> body = response.getBody();
            String label = body.get("label") != null ? String.valueOf(body.get("label")) : "NEUTRAL";
            Double score = toDouble(body.get("score"));
            Double confidence = toDouble(body.get("confidence"));
            return new SentimentResult(normalizeLabel(label), score, confidence, "ok");
        } catch (Exception ex) {
            log.warn("Sentiment service unavailable, fallback to NEUTRAL: {}", ex.getMessage());
            return SentimentResult.neutral("fallback");
        }
    }

    private String normalizeLabel(String rawLabel) {
        String value = rawLabel == null ? "" : rawLabel.trim().toUpperCase();
        return switch (value) {
            case "POSITIVE", "NEGATIVE", "NEUTRAL", "MIXED" -> value;
            default -> "NEUTRAL";
        };
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record SentimentResult(
            String label,
            Double score,
            Double confidence,
            String source
    ) {
        public static SentimentResult neutral(String source) {
            return new SentimentResult("NEUTRAL", 0.0, 0.0, source);
        }
    }
}
