package tn.esprit.arabsoftback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import tn.esprit.arabsoftback.dto.FaceVerificationPythonResponseDto;
import tn.esprit.arabsoftback.dto.FaceVerificationResponseDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class FaceVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(FaceVerificationService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Value("${face.verification.service.url}")
    private String faceServiceUrl;

    @Value("${face.verification.timeout-ms:60000}")
    private long timeoutMs;

    @Value("${face.verification.threshold:75}")
    private int threshold;

    public FaceVerificationService() {}

    public FaceVerificationResponseDto verify(MultipartFile webcamPhoto, MultipartFile identityDocument) throws IOException {
        validateFile(webcamPhoto, "webcamPhoto");
        validateFile(identityDocument, "identityDocument");

        Path webcamTemp = Files.createTempFile("webcam_", ".jpg");
        Path documentTemp = Files.createTempFile("identity_", ".jpg");

        try {
            webcamPhoto.transferTo(webcamTemp);
            identityDocument.transferTo(documentTemp);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("webcam_photo", new FileSystemResource(webcamTemp));
            body.add("identity_document", new FileSystemResource(documentTemp));

            logger.info("Face verification started. webcam={}, identity={}", webcamPhoto.getOriginalFilename(), identityDocument.getOriginalFilename());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = buildRestTemplate();

            FaceVerificationPythonResponseDto pythonResponse;
            try {
                pythonResponse = restTemplate.postForObject(
                        faceServiceUrl + "/verify",
                        requestEntity,
                        FaceVerificationPythonResponseDto.class
                );
            } catch (Exception ex) {
                logger.error("Error calling Flask face service: {}", ex.getMessage(), ex);
                throw new RuntimeException("Service de verification faciale indisponible");
            }

            if (pythonResponse == null) {
                throw new RuntimeException("Reponse vide du service de verification faciale");
            }

            double distance = pythonResponse.getDistance() == null ? 1.0 : pythonResponse.getDistance();
            int similarity = Math.max(0, Math.min(100, (int) Math.round((1 - distance) * 100)));
            boolean verified = similarity >= threshold;

            Map<String, Object> details = new HashMap<>();
            details.put("distance", distance);
            details.put("multiFaceDetected", pythonResponse.getMultiFaceDetected());
            details.put("detectedFacesDocument", pythonResponse.getFacesInDocument());
            details.put("detectedFacesWebcam", pythonResponse.getFacesInWebcam());

            String message = pythonResponse.getMessage() != null ? pythonResponse.getMessage() :
                    (verified ? "Identite verifiee avec succes." : "Visage non verifie.");

            logger.info("Face verification completed. verified={}, similarity={}", verified, similarity);
            return new FaceVerificationResponseDto(verified, similarity, similarity, message, details);

        } finally {
            Files.deleteIfExists(webcamTemp);
            Files.deleteIfExists(documentTemp);
        }
    }

    private void validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier requis manquant: " + fieldName);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Le fichier " + fieldName + " depasse 10MB");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!(contentType.contains("jpeg") || contentType.contains("jpg") || contentType.contains("png"))) {
            throw new IllegalArgumentException("Format invalide pour " + fieldName + ". Formats autorises: JPG, JPEG, PNG");
        }
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeoutMs);
        requestFactory.setReadTimeout((int) timeoutMs);
        return new RestTemplate(requestFactory);
    }
}
