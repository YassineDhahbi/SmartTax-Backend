package tn.esprit.arabsoftback.dto;

import java.util.Map;

public class FaceVerificationResponseDto {
    private boolean verified;
    private int similarity;
    private int confidence;
    private String message;
    private Map<String, Object> details;

    public FaceVerificationResponseDto() {
    }

    public FaceVerificationResponseDto(boolean verified, int similarity, int confidence, String message, Map<String, Object> details) {
        this.verified = verified;
        this.similarity = similarity;
        this.confidence = confidence;
        this.message = message;
        this.details = details;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public int getSimilarity() {
        return similarity;
    }

    public void setSimilarity(int similarity) {
        this.similarity = similarity;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
