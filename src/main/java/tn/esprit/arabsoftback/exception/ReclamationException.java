package tn.esprit.arabsoftback.exception;

public class ReclamationException extends RuntimeException {
    
    public ReclamationException(String message) {
        super(message);
    }
    
    public ReclamationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // Exception pour réclamation non trouvée
    public static class ReclamationNotFoundException extends ReclamationException {
        public ReclamationNotFoundException(String reference) {
            super("Réclamation non trouvée avec la référence: " + reference);
        }
        
        public ReclamationNotFoundException(Long id) {
            super("Réclamation non trouvée avec l'ID: " + id);
        }
    }
    
    // Exception pour statut invalide
    public static class InvalidStatusException extends ReclamationException {
        public InvalidStatusException(String currentStatus, String targetStatus) {
            super("Impossible de changer le statut de '" + currentStatus + "' vers '" + targetStatus + "'");
        }
    }

    public static class InvalidReclamationDataException extends ReclamationException {
        public InvalidReclamationDataException(String message) {
            super(message);
        }
    }
    
    // Exception pour accès non autorisé
    public static class UnauthorizedAccessException extends ReclamationException {
        public UnauthorizedAccessException(String email) {
            super("Accès non autorisé pour l'utilisateur: " + email);
        }
    }
    
    // Exception pour fichier invalide
    public static class InvalidFileException extends ReclamationException {
        public InvalidFileException(String message) {
            super("Fichier invalide: " + message);
        }
    }
    
    // Exception pour taille de fichier dépassée
    public static class FileSizeExceededException extends ReclamationException {
        public FileSizeExceededException(long maxSize) {
            super("La taille du fichier dépasse la limite maximale de " + maxSize + " bytes");
        }
    }
    
    // Exception pour type de fichier non supporté
    public static class UnsupportedFileTypeException extends ReclamationException {
        public UnsupportedFileTypeException(String fileType) {
            super("Type de fichier non supporté: " + fileType);
        }
    }
    
    // Exception pour réclamation déjà résolue
    public static class ReclamationAlreadyResolvedException extends ReclamationException {
        public ReclamationAlreadyResolvedException(String reference) {
            super("La réclamation " + reference + " est déjà résolue");
        }
    }
    
    // Exception pour opération non permise sur un brouillon
    public static class DraftOperationException extends ReclamationException {
        public DraftOperationException(String operation) {
            super("Opération '" + operation + "' non permise sur un brouillon");
        }
    }
    
    // Exception pour message non trouvé
    public static class MessageNotFoundException extends ReclamationException {
        public MessageNotFoundException(Long messageId) {
            super("Message non trouvé avec l'ID: " + messageId);
        }
    }

    /** Le contribuable ne peut pas écrire tant qu'aucun message de l'agent n'existe. */
    public static class AgentMustInitiateChatException extends ReclamationException {
        public AgentMustInitiateChatException() {
            super("Un agent doit d'abord vous contacter avant que vous puissiez envoyer un message.");
        }
    }

    /** Messagerie indisponible pour ce statut de réclamation (ex. brouillon, rejet). */
    public static class MessagerieUnavailableException extends ReclamationException {
        public MessagerieUnavailableException() {
            super("La messagerie n'est pas disponible pour un brouillon ou une réclamation rejetée.");
        }
    }
    
    // Exception pour pièce jointe non trouvée
    public static class AttachmentNotFoundException extends ReclamationException {
        public AttachmentNotFoundException(String fileName) {
            super("Pièce jointe non trouvée: " + fileName);
        }
    }
}
