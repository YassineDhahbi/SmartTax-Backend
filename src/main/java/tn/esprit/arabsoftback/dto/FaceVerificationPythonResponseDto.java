package tn.esprit.arabsoftback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FaceVerificationPythonResponseDto {
    private boolean verified;
    private Double distance;
    private String message;

    @JsonProperty("multi_face_detected")
    private Boolean multiFaceDetected;

    @JsonProperty("faces_in_document")
    private Integer facesInDocument;

    @JsonProperty("faces_in_webcam")
    private Integer facesInWebcam;

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getMultiFaceDetected() {
        return multiFaceDetected;
    }

    public void setMultiFaceDetected(Boolean multiFaceDetected) {
        this.multiFaceDetected = multiFaceDetected;
    }

    public Integer getFacesInDocument() {
        return facesInDocument;
    }

    public void setFacesInDocument(Integer facesInDocument) {
        this.facesInDocument = facesInDocument;
    }

    public Integer getFacesInWebcam() {
        return facesInWebcam;
    }

    public void setFacesInWebcam(Integer facesInWebcam) {
        this.facesInWebcam = facesInWebcam;
    }
}
