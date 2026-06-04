package tn.esprit.arabsoftback.exception;

public class ImmatriculationException extends RuntimeException {
    
    public ImmatriculationException(String message) {
        super(message);
    }
    
    public ImmatriculationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // Exceptions spécifiques
    public static class DossierNotFoundException extends ImmatriculationException {
        public DossierNotFoundException(String id) {
            super("Dossier d'immatriculation non trouvé avec l'ID: " + id);
        }
    }
    
    public static class DuplicateDossierException extends ImmatriculationException {
        public DuplicateDossierException(String field, String value) {
            super("Un dossier existe déjà avec ce " + field + ": " + value);
        }
    }
    
    public static class InvalidStatusException extends ImmatriculationException {
        public InvalidStatusException(String currentStatus, String requiredStatus) {
            super("Statut invalide. Actuel: " + currentStatus + ", Requis: " + requiredStatus);
        }
    }
    
    public static class FileProcessingException extends ImmatriculationException {
        public FileProcessingException(String message) {
            super("Erreur lors du traitement du fichier: " + message);
        }
    }
    
    public static class ValidationException extends ImmatriculationException {
        public ValidationException(String message) {
            super("Erreur de validation: " + message);
        }
    }
}
