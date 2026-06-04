# API de Gestion des Réclamations

## 📋 Overview

Cette API permet de gérer complètement le système de réclamations fiscales, incluant la création, le suivi, la messagerie et la gestion des pièces jointes.

## 🔐 Authentification

Tous les endpoints sont accessibles sans authentification pour le développement. En production, l'authentification JWT sera requise.

## 📊 Base de données

- **Tables** : `reclamation`, `message_reclamation`
- **SGBD** : PostgreSQL
- **Migration** : `V2__Create_reclamation_tables.sql`

## 🚀 Endpoints

### 1. Création de Réclamation

#### `POST /api/reclamation/create`
Crée une nouvelle réclamation avec pièces jointes.

**Request :**
```multipart/form-data
reclamation: {
  "type": { "value": "TECHNIQUE", "label": "Problème Technique" },
  "categorie": "Erreur de connexion",
  "sujet": "Problème connexion portail",
  "description": "Je ne peux pas me connecter...",
  "urgence": { "value": "HAUTE", "label": "Haute" },
  "referenceUser": "REF-123",
  "nomUser": "John Doe",
  "telephoneUser": "21612345678"
}
files: [File, File, ...]
```

**Response :**
```json
{
  "id": 1,
  "reference": "REC-20260317-0001",
  "message": "Réclamation créée avec succès",
  "statut": { "value": "BROUILLON", "label": "Brouillon" }
}
```

#### `POST /api/reclamation/create-with-form`
Alternative avec paramètres de formulaire.

**Request :**
```form-data
type: TECHNIQUE
categorie: Erreur de connexion
sujet: Problème connexion portail
description: Je ne peux pas me connecter...
urgence: HAUTE
reference: REF-123
nom: John Doe
telephone: 21612345678
statut: BROUILLON
files: [File, File, ...]
```

### 2. Lecture des Réclamations

#### `GET /api/reclamation/user`
Récupère toutes les réclamations de l'utilisateur connecté.

**Response :**
```json
[
  {
    "id": 1,
    "reference": "REC-20260317-0001",
    "type": { "value": "TECHNIQUE", "label": "Problème Technique" },
    "categorie": "Erreur de connexion",
    "sujet": "Problème connexion portail",
    "description": "Je ne peux pas me connecter...",
    "urgence": { "value": "HAUTE", "label": "Haute" },
    "statut": { "value": "SOUMIS", "label": "Soumis" },
    "dateCreation": "2026-03-17 14:30:00",
    "dateSoumission": "2026-03-17 14:35:00",
    "emailUser": "user@example.com",
    "piecesJointes": [...],
    "messages": [...]
  }
]
```

#### `GET /api/reclamation/all` 🔒 ADMIN
Récupère toutes les réclamations (pagination).

**Parameters :**
- `page` (default: 0) : Numéro de page
- `size` (default: 10) : Taille de page

#### `GET /api/reclamation/{id}`
Récupère une réclamation spécifique par ID.

#### `GET /api/reclamation/reference/{reference}`
Récupère une réclamation par sa référence.

### 3. Mise à Jour

#### `PUT /api/reclamation/{id}`
Met à jour une réclamation existante.

**Request :**
```multipart/form-data
reclamation: {
  "sujet": "Nouveau sujet",
  "description": "Nouvelle description...",
  "urgence": { "value": "MOYENNE", "label": "Moyenne" }
}
files: [File, File, ...] // Nouveaux fichiers
```

#### `PUT /api/reclamation/{id}/submit`
Soumet un brouillon.

#### `PUT /api/reclamation/{id}/status` 🔒 ADMIN
Change le statut d'une réclamation.

**Parameters :**
- `statut` : NOUVEAU_STATUT
- `motif` (optionnel) : Motif de résolution/rejet

### 4. Suppression

#### `DELETE /api/reclamation/{id}`
Supprime une réclamation (brouillons uniquement).

### 5. Messagerie

#### `GET /api/reclamation/{id}/messages`
Récupère tous les messages d'une réclamation.

**Response :**
```json
[
  {
    "id": 1,
    "reclamationId": 1,
    "contenu": "Bonjour, j'ai un problème...",
    "auteur": { "value": "contribuable", "label": "contribuable" },
    "dateEnvoi": "2026-03-17 14:30:00",
    "lu": true,
    "dateLecture": "2026-03-17 14:35:00",
    "pieceJointe": { ... }
  }
]
```

#### `POST /api/reclamation/{id}/messages`
Envoie un nouveau message.

**Request :**
```multipart/form-data
message: {
  "contenu": "Contenu du message...",
  "auteur": "contribuable"
}
file: File // Optionnel
```

#### `POST /api/reclamation/{id}/messages/simple`
Envoie un message texte simple.

**Parameters :**
- `contenu` : Contenu du message
- `auteur` : "contribuable" ou "agent"

### 6. Recherche et Filtres

#### `GET /api/reclamation/search`
Recherche des réclamations par texte.

**Parameters :**
- `q` : Texte de recherche
- `email` (optionnel) : Email utilisateur

#### `GET /api/reclamation/filter`
Filtre les réclamations par statut.

**Parameters :**
- `statut` : Statut à filtrer

#### `GET /api/reclamation/statistics`
Récupère les statistiques des réclamations.

**Parameters :**
- `email` (optionnel) : Email utilisateur pour stats personnelles

**Response :**
```json
{
  "total": 150,
  "brouillons": 25,
  "soumis": 50,
  "enCours": 35,
  "resolus": 30,
  "rejetes": 10,
  "nonLus": 5
}
```

### 7. Gestion des Fichiers

#### `GET /api/reclamation/files/{reference}/{fileName}`
Télécharge une pièce jointe.

#### `DELETE /api/reclamation/{id}/files/{fileName}`
Supprime une pièce jointe.

## 📝 Types de Données

### TypeRéclamation
- `TECHNIQUE` : Problème Technique
- `FISCAL` : Question Fiscale
- `COMPTE` : Problème de Compte
- `DOCUMENT` : Document Manquant
- `PAIEMENT` : Problème de Paiement
- `AUTRE` : Autre

### NiveauUrgence
- `BASSE` : Basse
- `MOYENNE` : Moyenne
- `HAUTE` : Haute
- `URGENTE` : Urgente

### StatutReclamation
- `BROUILLON` : Brouillon
- `SOUMIS` : Soumis
- `EN_COURS` : En cours de traitement
- `RESOLU` : Résolu
- `REJETE` : Rejeté

### AuteurMessage
- `contribuable` : Contribuable
- `agent` : Agent DGI

## 📁 Gestion des Fichiers

- **Taille maximale** : 5MB par fichier
- **Formats acceptés** : JPG, PNG, PDF, DOC, DOCX
- **Stockage** : `uploads/reclamations/{reference}/`
- **Accès** : Via endpoint `/files/{reference}/{fileName}`

## 🔧 Validation

### Création de Réclamation
- `type` : Obligatoire
- `categorie` : Obligatoire
- `sujet` : 5-100 caractères
- `description` : 20-1000 caractères
- `urgence` : Obligatoire
- `emailUser` : Obligatoire, format email valide

### Message
- `contenu` : 5-500 caractères
- `auteur` : "contribuable" ou "agent"

## ⚠️ Erreurs

### Codes d'erreur
- `400` : Données invalides
- `401` : Non authentifié
- `403` : Accès refusé
- `404` : Ressource non trouvée
- `409` : Conflit (doublon)
- `413` : Fichier trop volumineux
- `415` : Type de fichier non supporté
- `422` : Données invalides
- `500` : Erreur serveur

### Messages d'erreur spécifiques
```json
{
  "timestamp": "2026-03-17T14:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Le sujet doit contenir entre 5 et 100 caractères",
  "path": "/api/reclamation/create"
}
```

## 🔄 Workflow

1. **Création** : `BROUILLON` → Référence générée automatiquement
2. **Soumission** : `BROUILLON` → `SOUMIS` (dateSoumission automatique)
3. **Traitement** : `SOUMIS` → `EN_COURS` (agent)
4. **Résolution** : `EN_COURS` → `RESOLU` (dateResolution automatique)
5. **Rejet** : `SOUMIS`/`EN_COURS` → `REJETE`

## 🧪 Tests

### Créer une réclamation
```bash
curl -X POST http://localhost:8080/api/reclamation/create-with-form \
  -F "type=TECHNIQUE" \
  -F "categorie=Erreur de connexion" \
  -F "sujet=Test réclamation" \
  -F "description=Ceci est une réclamation de test..." \
  -F "urgence=MOYENNE" \
  -F "nom=Test User" \
  -F "telephone=21612345678"
```

### Récupérer les réclamations
```bash
curl -X GET http://localhost:8080/api/reclamation/user
```

### Envoyer un message
```bash
curl -X POST http://localhost:8080/api/reclamation/1/messages/simple \
  -F "contenu=Bonjour, j'ai une question..." \
  -F "auteur=contribuable"
```

## 📊 Performance

- **Index optimisés** pour les requêtes fréquentes
- **Pagination** pour les grandes listes
- **Recherche textuelle** avec PostgreSQL GIN
- **Cache** recommandé pour les statistiques

## 🔒 Sécurité

- **Validation stricte** des entrées
- **Contrôle des types** de fichiers
- **Limitation de taille** des fichiers
- **Gestion des permissions** par rôle
- **Audit trail** avec timestamps

---

**Cette API offre une gestion complète et sécurisée des réclamations fiscales !** 🚀
