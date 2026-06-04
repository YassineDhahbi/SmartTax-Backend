package tn.esprit.arabsoftback.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.arabsoftback.entity.Immatriculation;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service pour générer les numéros TIN (Tax Identification Number)
 * Format: Status (1) + Year (2) + Sequence (5) + Legal Form (1) + Check Letter (1)
 * Exemple: 22600045PK
 */
@Service
@Slf4j
public class TINGeneratorService {
    
    private final AtomicLong sequenceCounter = new AtomicLong(1);
    
    /**
     * Génère un TIN complet pour une immatriculation
     * 
     * @param immatriculation L'immatriculation pour laquelle générer le TIN
     * @return Le TIN généré (ex: 22600045PK)
     */
    public String generateTIN(Immatriculation immatriculation) {
        try {
            log.info("Génération TIN pour immatriculation: {}", immatriculation.getDossierNumber());
            
            // 1. Status (1 caractère)
            String status = generateStatus(immatriculation);
            
            // 2. Year (2 caractères)
            String year = generateYear();
            
            // 3. Sequence (5 caractères)
            String sequence = generateSequence();
            
            // 4. Legal Form (1 caractère)
            String legalForm = generateLegalForm(immatriculation);
            
            // 5. Check Letter (1 caractère)
            String checkLetter = generateCheckLetter(status + year + sequence + legalForm);
            
            String tin = status + year + sequence + legalForm + checkLetter;
            
            log.info("TIN généré: {} (Status: {}, Year: {}, Sequence: {}, LegalForm: {}, Check: {})", 
                    tin, status, year, sequence, legalForm, checkLetter);
            
            return tin;
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération TIN pour l'immatriculation: {}", 
                    immatriculation.getDossierNumber(), e);
            throw new RuntimeException("Erreur génération TIN", e);
        }
    }
    
    /**
     * Génère le statut (1 caractère)
     * 1 = ancien contribuable
     * 2 = nouveau contribuable
     */
    private String generateStatus(Immatriculation immatriculation) {
        // Logique métier : si c'est une nouvelle immatriculation, on met "2"
        // Si c'est une réimmatriculation ou mise à jour, on met "1"
        // Pour l'instant, toutes les nouvelles immatriculations sont "2"
        return "2"; // Nouveau contribuable
    }
    
    /**
     * Génère l'année (2 caractères)
     * Les deux derniers chiffres de l'année actuelle
     */
    private String generateYear() {
        String currentYear = String.valueOf(Year.now().getValue());
        return currentYear.substring(currentYear.length() - 2);
    }
    
    /**
     * Génère la séquence (5 caractères)
     * Numéro séquentiel complété avec des zéros à gauche
     */
    private String generateSequence() {
        long sequence = sequenceCounter.getAndIncrement();
        return String.format("%05d", sequence);
    }
    
    /**
     * Génère le code forme juridique (1 caractère)
     * P = personne physique
     * C = personne morale
     */
    private String generateLegalForm(Immatriculation immatriculation) {
        if (immatriculation.getTypeContribuable() == Immatriculation.TypeContribuable.PHYSIQUE) {
            return "P";
        } else {
            return "C";
        }
    }
    
    /**
     * Génère la lettre de vérification avec l'algorithme de Luhn
     * 
     * @param base Les 9 premiers caractères du TIN
     * @return La lettre de vérification
     */
    private String generateCheckLetter(String base) {
        try {
            // Convertir chaque caractère en valeur numérique
            int total = 0;
            for (int i = 0; i < base.length(); i++) {
                char c = base.charAt(i);
                int value;
                
                if (Character.isDigit(c)) {
                    value = Character.getNumericValue(c);
                } else {
                    // Pour les lettres (P, C), utiliser des valeurs prédéfinies
                    value = getLetterValue(c);
                }
                
                total += value * (i + 1); // Poids de position
            }
            
            // Calculer le modulo 26
            int remainder = total % 26;
            
            // Convertir en lettre (A=0, B=1, ..., Z=25)
            char checkLetter = (char) ('A' + remainder);
            
            log.debug("Calcul check letter - Base: {}, Total: {}, Remainder: {}, Check: {}", 
                    base, total, remainder, checkLetter);
            
            return String.valueOf(checkLetter);
            
        } catch (Exception e) {
            log.error("Erreur calcul check letter pour base: {}", base, e);
            return "K"; // Valeur par défaut
        }
    }
    
    /**
     * Convertit une lettre en valeur numérique pour le calcul Luhn
     */
    private int getLetterValue(char letter) {
        switch (letter) {
            case 'P':
                return 16; // P = 16ème lettre
            case 'C':
                return 3;  // C = 3ème lettre
            case '1':
            case '2':
                return Character.getNumericValue(letter);
            default:
                return 0;
        }
    }
    
    /**
     * Valide un TIN en utilisant l'algorithme de Luhn
     * 
     * @param tin Le TIN à valider
     * @return true si le TIN est valide
     */
    public boolean validateTIN(String tin) {
        if (tin == null || tin.length() != 11) {
            return false;
        }
        
        try {
            String base = tin.substring(0, 9);
            String checkLetter = tin.substring(9, 10);
            String legalForm = tin.substring(8, 9);
            
            String calculatedCheck = generateCheckLetter(base);
            
            boolean isValid = calculatedCheck.equals(checkLetter);
            
            log.debug("Validation TIN - Input: {}, Base: {}, Expected: {}, Actual: {}, Valid: {}", 
                    tin, base, calculatedCheck, checkLetter, isValid);
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Erreur validation TIN: {}", tin, e);
            return false;
        }
    }
    
    /**
     * Extrait les informations d'un TIN
     * 
     * @param tin Le TIN à analyser
     * @return Objet TINInfo avec les détails
     */
    public TINInfo parseTIN(String tin) {
        if (tin == null || tin.length() != 11) {
            return null;
        }
        
        try {
            TINInfo info = new TINInfo();
            info.setTin(tin);
            info.setStatus(tin.substring(0, 1));
            info.setYear(tin.substring(1, 3));
            info.setSequence(tin.substring(3, 8));
            info.setLegalForm(tin.substring(8, 9));
            info.setCheckLetter(tin.substring(9, 10));
            info.setValid(validateTIN(tin));
            
            // Informations dérivées
            info.setStatusDescription(getStatusDescription(info.getStatus()));
            info.setLegalFormDescription(getLegalFormDescription(info.getLegalForm()));
            info.setFullYear(getFullYear(info.getYear()));
            
            return info;
            
        } catch (Exception e) {
            log.error("Erreur parsing TIN: {}", tin, e);
            return null;
        }
    }
    
    /**
     * Obtient la description du statut
     */
    private String getStatusDescription(String status) {
        switch (status) {
            case "1":
                return "Ancien contribuable";
            case "2":
                return "Nouveau contribuable";
            default:
                return "Inconnu";
        }
    }
    
    /**
     * Obtient la description de la forme juridique
     */
    private String getLegalFormDescription(String legalForm) {
        switch (legalForm) {
            case "P":
                return "Personne physique";
            case "C":
                return "Personne morale";
            default:
                return "Inconnue";
        }
    }
    
    /**
     * Convertit l'année sur 2 chiffres en année complète
     */
    private String getFullYear(String year) {
        int currentYear = Year.now().getValue();
        int currentCentury = currentYear / 100 * 100;
        int twoDigitYear = Integer.parseInt(year);
        
        // Si l'année sur 2 chiffres est supérieure à l'année actuelle sur 2 chiffres,
        // on suppose que c'est le siècle précédent
        if (twoDigitYear > (currentYear % 100)) {
            return String.valueOf(currentCentury - 100 + twoDigitYear);
        } else {
            return String.valueOf(currentCentury + twoDigitYear);
        }
    }
    
    /**
     * Classe pour stocker les informations d'un TIN
     */
    public static class TINInfo {
        private String tin;
        private String status;
        private String year;
        private String sequence;
        private String legalForm;
        private String checkLetter;
        private boolean valid;
        private String statusDescription;
        private String legalFormDescription;
        private String fullYear;
        
        // Getters et setters
        public String getTin() { return tin; }
        public void setTin(String tin) { this.tin = tin; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getYear() { return year; }
        public void setYear(String year) { this.year = year; }
        
        public String getSequence() { return sequence; }
        public void setSequence(String sequence) { this.sequence = sequence; }
        
        public String getLegalForm() { return legalForm; }
        public void setLegalForm(String legalForm) { this.legalForm = legalForm; }
        
        public String getCheckLetter() { return checkLetter; }
        public void setCheckLetter(String checkLetter) { this.checkLetter = checkLetter; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getStatusDescription() { return statusDescription; }
        public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }
        
        public String getLegalFormDescription() { return legalFormDescription; }
        public void setLegalFormDescription(String legalFormDescription) { this.legalFormDescription = legalFormDescription; }
        
        public String getFullYear() { return fullYear; }
        public void setFullYear(String fullYear) { this.fullYear = fullYear; }
        
        @Override
        public String toString() {
            return String.format("TIN: %s (%s) - %s %s - %s - Valid: %s", 
                    tin, statusDescription, legalFormDescription, fullYear, valid);
        }
    }
}
