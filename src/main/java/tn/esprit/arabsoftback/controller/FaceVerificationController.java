package tn.esprit.arabsoftback.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.dto.FaceVerificationResponseDto;
import tn.esprit.arabsoftback.service.FaceVerificationService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/face-verification")
public class FaceVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(FaceVerificationController.class);

    private final FaceVerificationService faceVerificationService;

    public FaceVerificationController(FaceVerificationService faceVerificationService) {
        this.faceVerificationService = faceVerificationService;
    }

    @PostMapping
    public ResponseEntity<?> verifyFace(
            @RequestParam("webcamPhoto") MultipartFile webcamPhoto,
            @RequestParam("identityDocument") MultipartFile identityDocument
    ) {
        try {
            FaceVerificationResponseDto response = faceVerificationService.verify(webcamPhoto, identityDocument);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Face verification validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "verified", false,
                    "similarity", 0,
                    "confidence", 0,
                    "message", e.getMessage()
            ));
        } catch (IOException e) {
            logger.error("File handling error during face verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "verified", false,
                    "similarity", 0,
                    "confidence", 0,
                    "message", "Erreur lors du traitement des fichiers de v�rification."
            ));
        } catch (Exception e) {
            logger.error("Unexpected error during face verification", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "verified", false,
                    "similarity", 0,
                    "confidence", 0,
                    "message", e.getMessage() != null ? e.getMessage() : "Service de v�rification faciale indisponible."
            ));
        }
    }
}
