package tn.esprit.arabsoftback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.arabsoftback.entity.Immatriculation;
import tn.esprit.arabsoftback.entity.Role;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.exception.ImmatriculationException;
import tn.esprit.arabsoftback.repository.ImmatriculationRepository;
import tn.esprit.arabsoftback.repository.IUtilisateurRepository;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImmatriculationService {
    
    private final ImmatriculationRepository immatriculationRepository;
    private final TINGeneratorService tinGeneratorService;
    private final EmailService emailService;
    private final NotificationProducerService notificationProducerService;
    private final IUtilisateurRepository utilisateurRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    /**
     * Crée un nouveau dossier d'immatriculation
     */
    public Immatriculation createImmatriculation(Immatriculation immatriculation) {
        // Vérifier les doublons AVANT de créer le dossier
        checkForDuplicatesBeforeCreation(immatriculation);
        
        // Générer un numéro de dossier unique (distinct du matricule fiscal final)
        String dossierNumber = generateDossierNumber();
        immatriculation.setDossierNumber(dossierNumber);
        // Le matricule fiscal doit être généré uniquement lors de la validation Agent/Admin
        immatriculation.setMatriculeFiscal(null);
        
        // Calculer les scores de validation
        calculateValidationScores(immatriculation);
        
        // Vérifier les doublons (pour le scoring)
        checkForDuplicates(immatriculation);
        
        // Sauvegarder
        Immatriculation saved = immatriculationRepository.save(immatriculation);
        log.info("Nouveau dossier d'immatriculation créé: {}", saved.getDossierNumber());

        String dossierLabel = saved.getDossierNumber() != null ? saved.getDossierNumber() : ("#" + saved.getId());
        sendNotificationToAdmins(
                "NEW_IMMATRICULATION",
                "Nouvelle immatriculation",
                "Un nouveau dossier d'immatriculation a ete cree: " + dossierLabel + ".",
                saved.getId()
        );
        
        return saved;
    }
    
    /**
     * Vérifie les doublons avant la création et lève une exception si trouvé
     */
    private void checkForDuplicatesBeforeCreation(Immatriculation immatriculation) {
        // Vérifier le CIN pour personne physique (en excluant EN_COURS_VERIFICATION)
        if (immatriculation.getCin() != null && !immatriculation.getCin().trim().isEmpty()) {
            if (immatriculationRepository.existsByCinExcludingPending(immatriculation.getCin())) {
                log.warn("Tentative de doublon CIN: {}", immatriculation.getCin());
                throw new ImmatriculationException.DuplicateDossierException("CIN", immatriculation.getCin());
            }
        }
        
        // Vérifier l'email (en excluant EN_COURS_VERIFICATION)
        if (immatriculation.getEmail() != null && !immatriculation.getEmail().trim().isEmpty()) {
            if (immatriculationRepository.existsByEmailExcludingPending(immatriculation.getEmail())) {
                log.warn("Tentative de doublon Email: {}", immatriculation.getEmail());
                throw new ImmatriculationException.DuplicateDossierException("email", immatriculation.getEmail());
            }
        }
        
        // Vérifier le registre de commerce pour personne morale (en excluant EN_COURS_VERIFICATION)
        if (immatriculation.getRegistreCommerce() != null && !immatriculation.getRegistreCommerce().trim().isEmpty()) {
            if (immatriculationRepository.existsByRegistreCommerceExcludingPending(immatriculation.getRegistreCommerce())) {
                log.warn("Tentative de doublon Registre de Commerce: {}", immatriculation.getRegistreCommerce());
                throw new ImmatriculationException.DuplicateDossierException("registre de commerce", immatriculation.getRegistreCommerce());
            }
        }
    }
    
    /**
     * Met à jour un dossier d'immatriculation
     */
    public Immatriculation updateImmatriculation(Long id, Immatriculation immatriculation) {
        Immatriculation existing = getImmatriculationById(id);
        
        // Mettre à jour les champs modifiables
        updateFields(existing, immatriculation);
        
        // Recalculer les scores
        calculateValidationScores(existing);
        
        Immatriculation updated = immatriculationRepository.save(existing);
        log.info("Dossier d'immatriculation mis à jour: {}", updated.getDossierNumber());
        
        return updated;
    }
    
    /**
     * Soumet un dossier pour validation
     */
    public Immatriculation submitDossier(Long id) {
        Immatriculation dossier = getImmatriculationById(id);
        
        if (dossier.getStatus() != Immatriculation.DossierStatus.BROUILLON && 
            dossier.getStatus() != Immatriculation.DossierStatus.EN_COURS_VERIFICATION) {
            throw new IllegalStateException("Seuls les dossiers en brouillon ou en cours de vérification peuvent être soumis");
        }
        
        // Si le dossier est déjà soumis, ne pas le soumettre à nouveau
        if (dossier.getStatus() == Immatriculation.DossierStatus.SOUMIS) {
            log.warn("Le dossier {} est déjà soumis", dossier.getDossierNumber());
            return dossier;
        }
        
        dossier.setStatus(Immatriculation.DossierStatus.SOUMIS);
        dossier.setDateSoumission(LocalDateTime.now());
        dossier.setSubmissionMode(Immatriculation.SubmissionMode.SUBMIT);
        
        Immatriculation submitted = immatriculationRepository.save(dossier);
        log.info("Dossier soumis pour validation: {}", submitted.getDossierNumber());
        
        return submitted;
    }
    
    /**
     * Valide un dossier
     */
    public Immatriculation validateDossier(Long id) {
        Immatriculation dossier = getImmatriculationById(id);
        
        if (dossier.getStatus() != Immatriculation.DossierStatus.EN_COURS_VERIFICATION) {
            throw new IllegalStateException("Le dossier doit être en cours de vérification");
        }
        
        dossier.setStatus(Immatriculation.DossierStatus.VALIDE);
        dossier.setDateValidation(LocalDateTime.now());
        
        // Générer le matricule fiscal final lors de la validation si absent ou invalide
        if (shouldGenerateMatriculeFiscal(dossier)) {
            generateMatriculeFiscal(dossier);
        }
        
        Immatriculation validated = immatriculationRepository.save(dossier);
        log.info("Dossier validé: {}", validated.getDossierNumber());

        String registrationPlainCode = issueRegistrationSecurityCode(validated);

        // Envoyer l'email avec le TIN généré et le code de sécurité
        sendTINEmailAfterValidation(validated, registrationPlainCode);
        
        return validated;
    }
    
    /**
     * Rejette un dossier
     */
    public Immatriculation rejectDossier(Long id, String motif) {
        Immatriculation dossier = getImmatriculationById(id);
        
        if (dossier.getStatus() != Immatriculation.DossierStatus.EN_COURS_VERIFICATION) {
            throw new IllegalStateException("Le dossier doit être en cours de vérification");
        }
        
        dossier.setStatus(Immatriculation.DossierStatus.REJETE);
        dossier.setDateRejet(LocalDateTime.now());
        dossier.setMotifRejet(motif);
        
        Immatriculation rejected = immatriculationRepository.save(dossier);
        log.info("Dossier rejeté: {} - Motif: {}", rejected.getDossierNumber(), motif);
        log.info("Vérification motif enregistré en base: {}", rejected.getMotifRejet());

        return rejected;
    }
    
    /**
     * Récupère un dossier par son ID
     */
    public Immatriculation getImmatriculationById(Long id) {
        return immatriculationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé avec l'ID: " + id));
    }
    
    /**
     * Récupère un dossier par son numéro
     */
    public Optional<Immatriculation> getImmatriculationByNumber(String dossierNumber) {
        return immatriculationRepository.findByDossierNumber(dossierNumber);
    }
    
    /**
     * Liste tous les dossiers
     */
    public List<Immatriculation> getAllImmatriculations() {
        return immatriculationRepository.findAll();
    }
    
    /**
     * Liste les dossiers par statut
     */
    public List<Immatriculation> getImmatriculationsByStatus(Immatriculation.DossierStatus status) {
        return immatriculationRepository.findByStatusAndArchivedFalse(status);
    }
    
    /**
     * Recherche des dossiers
     */
    public List<Immatriculation> searchDossiers(String nom, String prenom, String email, 
                                               String cin, Immatriculation.DossierStatus status,
                                               Immatriculation.TypeContribuable typeContribuable) {
        return immatriculationRepository.searchDossiers(nom, prenom, email, cin, status, typeContribuable);
    }

    /**
     * Dossiers actifs du contribuable connecte (email exact, repli TIN).
     */
    public List<Immatriculation> getDossiersForContribuable(String email, String tin) {
        if (email != null && !email.trim().isEmpty()) {
            List<Immatriculation> byEmail = immatriculationRepository
                    .findByEmailIgnoreCaseAndArchivedFalseOrderByDateCreationDesc(email.trim());
            if (!byEmail.isEmpty()) {
                return byEmail;
            }
        }

        if (tin != null && !tin.trim().isEmpty()) {
            String normalizedTin = tin.trim().toUpperCase();
            return immatriculationRepository
                    .findTopByMatriculeFiscalOrMatriculeFiscalExistantOrderByIdDesc(normalizedTin, normalizedTin)
                    .filter(d -> !Boolean.TRUE.equals(d.getArchived()))
                    .map(List::of)
                    .orElse(List.of());
        }

        return List.of();
    }
    
    /**
     * Archive un dossier
     */
    public void archiveDossier(Long id) {
        Immatriculation dossier = getImmatriculationById(id);
        dossier.setArchived(true);
        dossier.setDateArchivage(LocalDateTime.now());
        immatriculationRepository.save(dossier);
        log.info("Dossier archivé: {}", dossier.getDossierNumber());
    }
    
    /**
     * Supprime physiquement un dossier
     */
    public void deleteImmatriculation(Long id) {
        Immatriculation dossier = getImmatriculationById(id);
        
        // Vérifier si le dossier peut être supprimé
        if (dossier.getStatus() == Immatriculation.DossierStatus.VALIDE) {
            throw new ImmatriculationException("Impossible de supprimer un dossier validé. Utilisez l'archivage à la place.");
        }
        
        immatriculationRepository.delete(dossier);
        log.info("Dossier supprimé physiquement: {}", dossier.getDossierNumber());
    }
    
    /**
     * Convertit un fichier en Base64
     */
    public String convertFileToBase64(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        byte[] bytes = file.getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    /**
     * Génère un numéro de dossier unique (format métier dossier, pas TIN)
     */
    private String generateDossierNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        String year = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int number = random.nextInt(999999) + 1;

        String dossierNumber = String.format("DOS-%s-%06d", year, number);

        // S'assurer que le numéro de dossier est unique
        while (immatriculationRepository.findByDossierNumber(dossierNumber).isPresent()) {
            number = random.nextInt(999999) + 1;
            dossierNumber = String.format("DOS-%s-%06d", year, number);
        }

        return dossierNumber;
    }
    
    /**
     * Calcule les scores de validation
     */
    private void calculateValidationScores(Immatriculation dossier) {
        int completedFields = 0;
        int totalFields = 0;
        
        // Compter les champs remplis
        if (dossier.getNom() != null && !dossier.getNom().isEmpty()) completedFields++;
        if (dossier.getPrenom() != null && !dossier.getPrenom().isEmpty()) completedFields++;
        if (dossier.getCin() != null && !dossier.getCin().isEmpty()) completedFields++;
        if (dossier.getDateNaissance() != null) completedFields++;
        if (dossier.getRaisonSociale() != null && !dossier.getRaisonSociale().isEmpty()) completedFields++;
        if (dossier.getRegistreCommerce() != null && !dossier.getRegistreCommerce().isEmpty()) completedFields++;
        if (dossier.getRepresentantLegal() != null && !dossier.getRepresentantLegal().isEmpty()) completedFields++;
        if (dossier.getEmail() != null && !dossier.getEmail().isEmpty()) completedFields++;
        if (dossier.getTelephone() != null && !dossier.getTelephone().isEmpty()) completedFields++;
        if (dossier.getAdresse() != null && !dossier.getAdresse().isEmpty()) completedFields++;
        if (dossier.getTypeActivite() != null && !dossier.getTypeActivite().isEmpty()) completedFields++;
        if (dossier.getSecteur() != null && !dossier.getSecteur().isEmpty()) completedFields++;
        if (dossier.getAdresseProfessionnelle() != null && !dossier.getAdresseProfessionnelle().isEmpty()) completedFields++;
        if (dossier.getDateDebutActivite() != null) completedFields++;
        if (dossier.getDescriptionActivite() != null && !dossier.getDescriptionActivite().isEmpty()) completedFields++;
        
        // Calculer le nombre total de champs selon le type
        if (dossier.getTypeContribuable() == Immatriculation.TypeContribuable.PHYSIQUE) {
            totalFields = 11; // nom, prenom, cin, dateNaissance, email, telephone, adresse, typeActivite, secteur, adresseProfessionnelle, dateDebutActivite, descriptionActivite
        } else {
            totalFields = 10; // raisonSociale, registreCommerce, representantLegal, email, telephone, adresse, typeActivite, secteur, adresseProfessionnelle, dateDebutActivite, descriptionActivite
        }
        
        int completenessScore = totalFields > 0 ? (completedFields * 100) / totalFields : 0;
        dossier.setCompletenessScore(completenessScore);
        
        // Score des documents
        int documentScore = 0;
        if (dossier.getIdentiteFile() != null) documentScore += 33;
        if (dossier.getActiviteFile() != null) documentScore += 33;
        if (dossier.getPhotoFile() != null || dossier.getTypeContribuable() == Immatriculation.TypeContribuable.MORALE) documentScore += 34;
        dossier.setDocumentsScore(documentScore);
        
        // Normaliser le score SWIN (nullable en base pour les anciennes lignes)
        if (dossier.getIdentityValidationScore() == null) {
            dossier.setIdentityValidationScore(0);
        }

        // Score de vérification : aligné sur SWIN (pièce d'identité) si disponible, sinon estimation OCR + visage
        int verificationScore;
        Integer swin = dossier.getIdentityValidationScore();
        if (swin != null && swin > 0) {
            verificationScore = Math.min(100, swin);
        } else {
            int face = dossier.getFaceRecognitionScore() != null ? dossier.getFaceRecognitionScore() : 0;
            verificationScore = (face + 85) / 2; // 85 = score OCR simulé
        }
        dossier.setVerificationScore(verificationScore);
        
        // Score global
        int overallScore = (completenessScore + verificationScore + documentScore) / 3;
        dossier.setOverallScore(overallScore);
    }
    
    /**
     * Vérifie les doublons
     */
    private void checkForDuplicates(Immatriculation dossier) {
        boolean duplicate = false;
        
        if (dossier.getCin() != null && immatriculationRepository.existsByCin(dossier.getCin())) {
            duplicate = true;
        }
        
        if (dossier.getEmail() != null && immatriculationRepository.existsByEmail(dossier.getEmail())) {
            duplicate = true;
        }
        
        if (dossier.getRegistreCommerce() != null && 
            immatriculationRepository.existsByRegistreCommerce(dossier.getRegistreCommerce())) {
            duplicate = true;
        }
        
        dossier.setDuplicateDetected(duplicate);
    }
    
    /**
     * Génère un matricule fiscal (TIN) en utilisant le TINGeneratorService
     */
    private void generateMatriculeFiscal(Immatriculation dossier) {
        try {
            // Générer le TIN avec le nouveau format
            String tin = tinGeneratorService.generateTIN(dossier);
            
            // Stocker le TIN dans le champ matriculeFiscal (nouveau format)
            dossier.setMatriculeFiscal(tin);
            
            // Garder aussi dans matriculeFiscalExistant pour compatibilité
            if (dossier.getTypeContribuable() == Immatriculation.TypeContribuable.MORALE) {
                dossier.setMatriculeFiscalExistant(tin);
            }
            
            log.info("TIN généré pour le dossier {}: {}", dossier.getDossierNumber(), tin);
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération TIN pour le dossier {}: {}", 
                    dossier.getDossierNumber(), e.getMessage(), e);
            // En cas d'erreur, générer un format de secours
            generateFallbackMatriculeFiscal(dossier);
        }
    }
    
    /**
     * Génère un matricule fiscal de secours (ancien format) en cas d'erreur
     */
    private void generateFallbackMatriculeFiscal(Immatriculation dossier) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        String year = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int number = random.nextInt(999999) + 1;
        
        String matricule = String.format("TN-SH-%s-%06d", year, number);
        
        // Toujours renseigner le matricule fiscal final
        dossier.setMatriculeFiscal(matricule);
        // Compatibilité existante
        if (dossier.getTypeContribuable() == Immatriculation.TypeContribuable.MORALE) {
            dossier.setMatriculeFiscalExistant(matricule);
        }
        
        log.warn("Matricule fiscal de secours généré pour le dossier {}: {}", 
                dossier.getDossierNumber(), matricule);
    }

    private boolean shouldGenerateMatriculeFiscal(Immatriculation dossier) {
        String matricule = dossier.getMatriculeFiscal();
        if (matricule == null || matricule.trim().isEmpty()) {
            return true;
        }

        String normalized = matricule.trim();
        if (normalized.equalsIgnoreCase(dossier.getDossierNumber())) {
            return true;
        }

        // Format attendu du TIN généré: 1 chiffre statut + 2 année + 5 séquence + 1 forme + 1 lettre
        return !normalized.matches("^[12]\\d{2}\\d{5}[PC][A-Z]$");
    }
    
    /**
     * Met à jour les champs modifiables
     */
    private void updateFields(Immatriculation existing, Immatriculation updated) {
        // Informations personnelles
        existing.setNom(updated.getNom());
        existing.setPrenom(updated.getPrenom());
        existing.setCin(updated.getCin());
        existing.setDateNaissance(updated.getDateNaissance());
        existing.setRaisonSociale(updated.getRaisonSociale());
        existing.setRegistreCommerce(updated.getRegistreCommerce());
        existing.setRepresentantLegal(updated.getRepresentantLegal());
        
        // Contact
        existing.setEmail(updated.getEmail());
        existing.setTelephone(updated.getTelephone());
        existing.setAdresse(updated.getAdresse());
        
        // Activité
        existing.setTypeActivite(updated.getTypeActivite());
        existing.setSecteur(updated.getSecteur());
        existing.setAdresseProfessionnelle(updated.getAdresseProfessionnelle());
        existing.setDateDebutActivite(updated.getDateDebutActivite());
        existing.setDescriptionActivite(updated.getDescriptionActivite());
        
        // Fichiers
        existing.setIdentiteFile(updated.getIdentiteFile());
        existing.setActiviteFile(updated.getActiviteFile());
        existing.setPhotoFile(updated.getPhotoFile());
        existing.setAutresFiles(updated.getAutresFiles());
        
        // Scores
        existing.setFaceRecognitionScore(updated.getFaceRecognitionScore());
        if (updated.getIdentityValidationScore() != null) {
            existing.setIdentityValidationScore(updated.getIdentityValidationScore());
        }
        existing.setOcrResults(updated.getOcrResults());
        
        // Confirmation
        existing.setConfirmed(updated.getConfirmed());
        
        // Statut (important pour la réactivation)
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
    }
    
    /**
     * Génère un code à 8 chiffres, le stocke en hash pour la création de compte, et retourne le code en clair (email uniquement).
     */
    private String issueRegistrationSecurityCode(Immatriculation dossier) {
        String plain = String.format("%08d", new SecureRandom().nextInt(100_000_000));
        dossier.setRegistrationSecurityCodeHash(passwordEncoder.encode(plain));
        dossier.setRegistrationSecurityCodeConsumed(Boolean.FALSE);
        immatriculationRepository.save(dossier);
        log.info("Code d'inscription contribuable émis pour le dossier {}", dossier.getDossierNumber());
        return plain;
    }

    /**
     * Envoyer un email avec le TIN après validation du dossier
     */
    private void sendTINEmailAfterValidation(Immatriculation dossier, String plainSecurityCode) {
        try {
            String tin = dossier.getMatriculeFiscal();
            String email = dossier.getEmail();
            
            if (tin == null || tin.isEmpty()) {
                log.warn("TIN non disponible pour le dossier {}", dossier.getDossierNumber());
                return;
            }
            
            if (email == null || email.isEmpty()) {
                log.warn("Email non disponible pour le dossier {}", dossier.getDossierNumber());
                return;
            }
            
            // Préparer le contenu de l'email
            String subject = "SmartTax - Votre TIN (Numéro d'Immatriculation Fiscale)";
            String body = generateTINEmailBody(dossier, tin, plainSecurityCode);
            
            // Envoyer l'email avec le TIN
            boolean emailSent = emailService.sendTINEmail(email, subject, body, tin, "http://localhost:4200/register");
            
            if (emailSent) {
                log.info("Email avec TIN {} envoyé avec succès à {}", tin, email);
            } else {
                log.error("Échec de l'envoi de l'email avec TIN {} à {}", tin, email);
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email TIN pour le dossier {}: {}", 
                    dossier.getDossierNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * Générer le corps de l'email avec le TIN et le code de sécurité pour créer le compte
     */
    private String generateTINEmailBody(Immatriculation dossier, String tin, String plainSecurityCode) {
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f8f9fa;\">");
        body.append("<div style=\"background-color: #007bff; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;\">");
        body.append("<h1 style=\"color: white; margin: 0; font-size: 24px;\">TIN (Tax Identification Number)</h1>");
        body.append("<p style=\"color: #d4edda; margin: 10px 0 0 0; font-size: 16px;\">Votre immatriculation a été validée !</p>");
        body.append("</div>");
        body.append("<div style=\"padding: 30px; background-color: white; border-radius: 0 0 8px 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);\">");
        body.append("<h2 style=\"color: #333; margin-bottom: 20px;\">Félicitations !</h2>");
        body.append("<p style=\"color: #666; line-height: 1.6; margin-bottom: 20px;\">");
        body.append("Votre demande d'immatriculation fiscale a été validée avec succès. ");
        body.append("Voici votre TIN (Tax Identification Number) qui servira d'identifiant unique pour toutes vos démarches fiscales.");
        body.append("</p>");
        body.append("<div style=\"background-color: #e9ecef; padding: 20px; border-radius: 5px; text-align: center; margin: 20px 0;\">");
        body.append("<h3 style=\"color: #495057; margin: 0 0 10px 0;\">Votre TIN</h3>");
        body.append("<div style=\"font-size: 24px; font-weight: bold; color: #007bff; letter-spacing: 2px; background-color: white; padding: 15px; border-radius: 5px; border: 2px solid #007bff;\">");
        body.append(tin);
        body.append("</div>");
        body.append("</div>");
        body.append("<div style=\"background-color: #fff3cd; padding: 18px; border-radius: 5px; margin: 20px 0; border: 1px solid #ffc107;\">");
        body.append("<h3 style=\"color: #856404; margin: 0 0 10px 0;\">Code de sécurité — création de compte</h3>");
        body.append("<p style=\"color: #666; line-height: 1.6; margin: 0 0 12px 0;\">");
        body.append("Pour <strong>créer votre compte</strong> sur SmartTax, saisissez ce <strong>code à 8 chiffres</strong> sur la page d'inscription (une seule fois) :");
        body.append("</p>");
        body.append("<div style=\"font-size: 28px; font-weight: bold; color: #212529; letter-spacing: 6px; text-align: center; background: #fff; padding: 14px; border-radius: 6px; border: 2px dashed #856404;\">");
        body.append(plainSecurityCode != null ? plainSecurityCode : "—");
        body.append("</div>");
        body.append("<p style=\"color: #856404; font-size: 13px; margin: 12px 0 0 0;\">Ne partagez ce code avec personne. Il est lié à votre dossier et à votre adresse email.</p>");
        body.append("</div>");
        body.append("<p style=\"color: #666; line-height: 1.6; margin-top: 20px;\">");
        body.append("Conservez précieusement votre TIN et ce code. Ils vous seront demandés lors de la création de votre compte contribuable.");
        body.append("</p>");
        body.append("<div style=\"text-align: center; margin-top: 30px;\">");
        body.append("<a href=\"http://localhost:4200/register\" style=\"background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;\">");
        body.append("Créer mon compte");
        body.append("</a>");
        body.append("</div>");
        body.append("<hr style=\"border: none; border-top: 1px solid #dee2e6; margin: 30px 0;\">");
        body.append("<p style=\"color: #6c757d; font-size: 14px; text-align: center; margin: 0;\">");
        body.append("Cet email a été envoyé automatiquement par le système SmartTax. ");
        body.append("Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet email.");
        body.append("</p>");
        body.append("</div>");
        body.append("</div>");
        
        return body.toString();
    }
    
    /**
     * Statistiques
     */
    public List<Object[]> getStatisticsByStatus() {
        return immatriculationRepository.getStatisticsByStatus();
    }
    
    public long countSinceDate(LocalDateTime date) {
        return immatriculationRepository.countSinceDate(date);
    }
    
    public List<Immatriculation> getLowScoreDossiers() {
        return immatriculationRepository.findLowScoreDossiers();
    }

    private void sendNotificationToAdmins(String eventType, String title, String message, Long immatriculationId) {
        List<Integer> recipientIds = new ArrayList<>();
        recipientIds.addAll(utilisateurRepository.findByRole(Role.ADMIN).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList());
        recipientIds.addAll(utilisateurRepository.findByRole(Role.AGENT).stream()
                .map(Utilisateur::getIdUtilisateur)
                .toList());
        List<Integer> uniqueRecipientIds = recipientIds.stream().distinct().toList();
        // Reuse publicationId field as generic reference id for admin redirection.
        notificationProducerService.send(eventType, title, message, immatriculationId, uniqueRecipientIds);
    }
}
