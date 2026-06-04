package tn.esprit.arabsoftback.exception;

public class TrashException extends RuntimeException {
    
    public TrashException(String message) {
        super(message);
    }
    
    public TrashException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static class ItemNotFoundException extends TrashException {
        public ItemNotFoundException(String itemType, Object itemId) {
            super(String.format("%s avec l'ID %s non trouvé", itemType, itemId));
        }
    }
    
    public static class ItemAlreadyInTrashException extends TrashException {
        public ItemAlreadyInTrashException(String itemType, Object itemId) {
            super(String.format("%s avec l'ID %s est déjà dans la corbeille", itemType, itemId));
        }
    }
    
    public static class TrashOperationException extends TrashException {
        public TrashOperationException(String message) {
            super(message);
        }
        
        public TrashOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class CannotRestoreException extends TrashException {
        public CannotRestoreException(String reason) {
            super("Impossible de restaurer l'élément: " + reason);
        }
    }
}
