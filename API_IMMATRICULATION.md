# API d'Immatriculation Fiscale - SmartTax

## 📋 Description

Cette API gère le processus complet d'immatriculation fiscale pour la DGI (Direction Générale des Impôts) Tunisienne. Elle permet aux contribuables de créer leur dossier en ligne, de téléverser les documents requis, et de suivre l'état de leur demande.

## 🏗️ Architecture

### Structure du projet
```
src/main/java/tn/esprit/arabsoftback/
├── controller/
│   ├── ImmatriculationController.java  # API REST
│   └── TestController.java             # Tests de santé
├── service/
│   └── ImmatriculationService.java     # Logique métier
├── repository/
│   └── ImmatriculationRepository.java  # Accès aux données
├── entity/
│   └── Immatriculation.java            # Entité JPA
├── dto/
│   └── ImmatriculationDto.java         # Objets de transfert
├── mapper/
│   └── ImmatriculationMapper.java      # Conversion Entity/DTO
├── exception/
│   ├── ImmatriculationException.java   # Exceptions personnalisées
│   └── GlobalExceptionHandler.java     # Gestionnaire d'exceptions
└── config/
    └── WebConfig.java                  # Configuration CORS
```

### Base de données
- **Table** : `immatriculation`
- **Migration** : `V1__Create_immatriculation_table.sql`

## 🚀 Endpoints

### Gestion des dossiers

#### Créer un dossier
```http
POST /api/immatriculation/create
Content-Type: application/json

{
  "typeContribuable": "PHYSIQUE",
  "nom": "Mohamed",
  "prenom": "Ben Ali",
  "cin": "12345678",
  "dateNaissance": "1990-01-15",
  "email": "mohamed@email.com",
  "telephone": "21612345678",
  "adresse": "Tunis, Tunisie",
  "typeActivite": "Commerce",
  "secteur": "Commerce de détail",
  "adresseProfessionnelle": "Avenue Habib Bourguiba, Tunis",
  "dateDebutActivite": "2024-01-01",
  "descriptionActivite": "Vente de produits informatiques",
  "submissionMode": "SUBMIT",
  "confirmed": true
}
```

#### Créer un dossier avec fichiers
```http
POST /api/immatriculation/create-with-files
Content-Type: multipart/form-data

data: [JSON du dossier]
identiteFile: [Fichier d'identité]
activiteFile: [Fichier d'activité]
photoFile: [Photo]
autresFiles: [Fichiers supplémentaires]
```

#### Récupérer un dossier
```http
GET /api/immatriculation/{id}
```

#### Mettre à jour un dossier
```http
PUT /api/immatriculation/{id}
Content-Type: application/json

{
  "nom": "Mohamed Updated",
  "email": "newemail@email.com"
}
```

### Workflow

#### Soumettre un dossier
```http
POST /api/immatriculation/{id}/submit
```

#### Valider un dossier
```http
POST /api/immatriculation/{id}/validate
```

#### Rejeter un dossier
```http
POST /api/immatriculation/{id}/reject
Content-Type: application/json

{
  "motifRejet": "Documents incomplets"
}
```

#### Archiver un dossier
```http
DELETE /api/immatriculation/{id}/archive
```

### Recherche et consultation

#### Lister tous les dossiers
```http
GET /api/immatriculation
```

#### Rechercher des dossiers
```http
GET /api/immatriculation/search?nom=Mohamed&status=SOUMIS
```

#### Dossiers par statut
```http
GET /api/immatriculation/status/SOUMIS
```

#### Liste paginée
```http
GET /api/immatriculation/paginated?page=0&size=10&sortBy=dateCreation&sortDir=desc
```

### Statistiques et rapports

#### Statistiques générales
```http
GET /api/immatriculation/statistics
```

#### Tableau de bord
```http
GET /api/immatriculation/dashboard
```

#### Télécharger un fichier
```http
GET /api/immatriculation/{id}/download/{fileType}
```

## 📊 Modèles de données

### Entity Immatriculation
```java
public class Immatriculation {
    private Long id;
    private String dossierNumber;
    private TypeContribuable typeContribuable; // PHYSIQUE, MORALE
    
    // Personne Physique
    private String nom, prenom, cin;
    private LocalDate dateNaissance;
    
    // Personne Morale
    private String raisonSociale, matriculeFiscalExistant;
    private String registreCommerce, representantLegal;
    
    // Contact
    private String email, telephone, adresse;
    
    // Activité
    private String typeActivite, secteur, adresseProfessionnelle;
    private LocalDate dateDebutActivite;
    private String descriptionActivite;
    
    // Fichiers (Base64)
    private String identiteFile, activiteFile, photoFile, autresFiles;
    
    // Scores (0-100)
    private Integer overallScore, completenessScore;
    private Integer verificationScore, documentsScore;
    private Integer faceRecognitionScore;
    
    // Workflow
    private DossierStatus status; // BROUILLON, SOUMIS, EN_COURS_VERIFICATION, VALIDE, REJETE
    private LocalDateTime dateCreation, dateSoumission, dateValidation;
    private String motifRejet;
}
```

## 🔧 Configuration

### CORS
L'API est configurée pour accepter les requêtes de `http://localhost:4200` (frontend Angular).

### Validation
- Validation automatique des champs requis
- Vérification des doublons (CIN, email, registre de commerce)
- Calcul automatique des scores de confiance

### Gestion des fichiers
- Les fichiers sont convertis en Base64 pour le stockage
- Support des formats : PDF, JPG, PNG
- Taille maximale recommandée : 5MB par fichier

## 🧪 Tests

### Test de santé
```http
GET /api/test/health
```

### Test de disponibilité
```http
GET /api/test/immatriculation-ready
```

## 📝 Workflow complet

1. **Création** : Le contribuable crée son dossier (statut: BROUILLON)
2. **Soumission** : Le dossier est soumis pour validation (statut: SOUMIS)
3. **Vérification** : Traitement automatique et manuel (statut: EN_COURS_VERIFICATION)
4. **Décision** : Validation ou rejet (statut: VALIDE ou REJETE)
5. **Archivage** : Les dossiers peuvent être archivés logiquement

## 🔐 Sécurité

- Validation des entrées
- Protection contre les injections SQL
- Gestion sécurisée des fichiers
- Logs de traçabilité

## 📈 Monitoring

- Logs détaillés des opérations
- Statistiques en temps réel
- Alertes pour les dossiers à faible score
- Suivi des performances

## 🚨 Codes d'erreur

| Code | Description |
|------|-------------|
| 400 | Données invalides |
| 404 | Dossier non trouvé |
| 409 | Doublon détecté |
| 500 | Erreur interne du serveur |

## 📞 Support

Pour toute question ou problème technique, contacter l'équipe de développement SmartTax.

---

**Version** : 1.0.0  
**Dernière mise à jour** : Mars 2024  
**Auteur** : Équipe SmartTax
