-- Création de la table immatriculation
CREATE TABLE IF NOT EXISTS immatriculation (
    id BIGINT SERIAL PRIMARY KEY,
    dossier_number VARCHAR(50) NOT NULL UNIQUE,
    type_contribuable VARCHAR(10) NOT NULL CHECK (type_contribuable IN ('PHYSIQUE', 'MORALE')),
    
    -- Informations personne physique
    nom VARCHAR(100),
    prenom VARCHAR(100),
    cin VARCHAR(20),
    date_naissance DATE,
    
    -- Informations personne morale
    raison_sociale VARCHAR(200),
    matricule_fiscal_existant VARCHAR(50),
    registre_commerce VARCHAR(50),
    representant_legal VARCHAR(100),
    
    -- Champs communs
    email VARCHAR(100) NOT NULL,
    telephone VARCHAR(20) NOT NULL,
    adresse TEXT NOT NULL,
    
    -- Informations activité
    type_activite VARCHAR(100) NOT NULL,
    secteur VARCHAR(100) NOT NULL,
    adresse_professionnelle TEXT NOT NULL,
    date_debut_activite DATE NOT NULL,
    description_activite TEXT NOT NULL,
    
    -- Fichiers (stockés en base64 ou chemins)
    identite_file TEXT,
    activite_file TEXT,
    photo_file TEXT,
    autres_files TEXT,
    
    -- Scores de vérification
    overall_score INT NOT NULL DEFAULT 0,
    completeness_score INT NOT NULL DEFAULT 0,
    verification_score INT NOT NULL DEFAULT 0,
    documents_score INT NOT NULL DEFAULT 0,
    face_recognition_score INT NOT NULL DEFAULT 0,
    
    -- Vérifications
    duplicate_detected BOOLEAN NOT NULL DEFAULT FALSE,
    ocr_results TEXT,
    
    -- Workflow
    status VARCHAR(20) NOT NULL DEFAULT 'BROUILLON' CHECK (status IN ('BROUILLON', 'SOUMIS', 'EN_COURS_VERIFICATION', 'VALIDE', 'REJETE')),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_soumission TIMESTAMP,
    date_validation TIMESTAMP,
    date_rejet TIMESTAMP,
    motif_rejet TEXT,
    
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    submission_mode VARCHAR(10) NOT NULL DEFAULT 'SUBMIT' CHECK (submission_mode IN ('DRAFT', 'SUBMIT')),
    
    -- Statut du dossier
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    date_archivage TIMESTAMP,
    
    -- Index
    CREATE INDEX idx_dossier_number ON immatriculation(dossier_number);
    CREATE INDEX idx_email ON immatriculation(email);
    CREATE INDEX idx_cin ON immatriculation(cin);
    CREATE INDEX idx_registre_commerce ON immatriculation(registre_commerce);
    CREATE INDEX idx_status ON immatriculation(status);
    CREATE INDEX idx_type_contribuable ON immatriculation(type_contribuable);
    CREATE INDEX idx_date_creation ON immatriculation(date_creation);
    CREATE INDEX idx_date_soumission ON immatriculation(date_soumission);
    CREATE INDEX idx_archived ON immatriculation(archived);
    
    -- Index composites
    CREATE INDEX idx_status_archived ON immatriculation(status, archived);
    CREATE INDEX idx_type_status ON immatriculation(type_contribuable, status);
);

-- Insertion de données de test
INSERT INTO immatriculation (
    dossier_number, type_contribuable, nom, prenom, cin, date_naissance,
    email, telephone, adresse, type_activite, secteur, adresse_professionnelle,
    date_debut_activite, description_activite, overall_score, completeness_score,
    verification_score, documents_score, status
) VALUES 
(
    'TN-DG-2024-000001', 'PHYSIQUE', 'Mohamed', 'Ben Ali', '12345678', '1990-01-15',
    'mohamed.benali@email.com', '21612345678', 'Tunis, Tunisie', 'Commerce', 'Commerce de détail',
    'Avenue Habib Bourguiba, Tunis', '2024-01-01', 'Vente de produits informatiques',
    85, 90, 80, 85, 'SOUMIS'
),
(
    'TN-DG-2024-000002', 'MORALE', 'Société', 'Tunis Tech', null, null,
    'contact@tunistech.com', '21698765432', 'Sousse, Tunisie', 'Services', 'Technologies',
    'Zone Industrielle, Sousse', '2024-02-01', 'Développement de logiciels',
    92, 95, 90, 92, 'VALIDE'
),
(
    'TN-DG-2024-000003', 'PHYSIQUE', 'Fatma', 'Mansour', '87654321', '1985-05-20',
    'fatma.mansour@email.com', '21655556666', 'Sfax, Tunisie', 'Artisanat', 'Artisanat',
    'Rue de la Médina, Sfax', '2024-03-01', 'Création de bijoux artisanaux',
    78, 80, 75, 80, 'EN_COURS_VERIFICATION'
);

-- Création de la table pour les logs de traitement (optionnel)
CREATE TABLE IF NOT EXISTS immatriculation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dossier_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(100),
    
    FOREIGN KEY (dossier_id) REFERENCES immatriculation(id) ON DELETE CASCADE,
    INDEX idx_dossier_id (dossier_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
);

-- Commentaires sur la table
ALTER TABLE immatriculation COMMENT = 'Table des dossiers d''immatriculation fiscale';
ALTER TABLE immatriculation_logs COMMENT = 'Table des logs de traitement des dossiers';
