-- Création des tables pour le système de réclamation
-- Version PostgreSQL compatible

-- Création de la table reclamation
CREATE TABLE IF NOT EXISTS reclamation (
    id BIGINT SERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('TECHNIQUE', 'FISCAL', 'COMPTE', 'DOCUMENT', 'PAIEMENT', 'AUTRE')),
    categorie VARCHAR(100) NOT NULL,
    sujet VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    urgence VARCHAR(10) NOT NULL CHECK (urgence IN ('BASSE', 'MOYENNE', 'HAUTE', 'URGENTE')),
    reference_user VARCHAR(100),
    statut VARCHAR(20) NOT NULL CHECK (statut IN ('BROUILLON', 'SOUMIS', 'EN_COURS', 'RESOLU', 'REJETE')),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_soumission TIMESTAMP,
    date_resolution TIMESTAMP,
    motif_resolution TEXT,
    email_user VARCHAR(100) NOT NULL,
    nom_user VARCHAR(100),
    telephone_user VARCHAR(20),
    pieces_jointes TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    date_archivage TIMESTAMP
);

-- Création de la table message_reclamation
CREATE TABLE IF NOT EXISTS message_reclamation (
    id BIGINT SERIAL PRIMARY KEY,
    reclamation_id BIGINT NOT NULL,
    contenu TEXT NOT NULL,
    auteur VARCHAR(15) NOT NULL CHECK (auteur IN ('contribuable', 'agent')),
    date_envoi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lu BOOLEAN NOT NULL DEFAULT FALSE,
    date_lecture TIMESTAMP,
    piece_jointe TEXT,
    FOREIGN KEY (reclamation_id) REFERENCES reclamation(id) ON DELETE CASCADE
);

-- Index pour la table reclamation
CREATE INDEX idx_reclamation_reference ON reclamation(reference);
CREATE INDEX idx_reclamation_email_user ON reclamation(email_user);
CREATE INDEX idx_reclamation_statut ON reclamation(statut);
CREATE INDEX idx_reclamation_type ON reclamation(type);
CREATE INDEX idx_reclamation_urgence ON reclamation(urgence);
CREATE INDEX idx_reclamation_date_creation ON reclamation(date_creation);
CREATE INDEX idx_reclamation_date_soumission ON reclamation(date_soumission);
CREATE INDEX idx_reclamation_archived ON reclamation(archived);

-- Index composites
CREATE INDEX idx_reclamation_statut_date ON reclamation(statut, date_creation);
CREATE INDEX idx_reclamation_user_statut ON reclamation(email_user, statut);
CREATE INDEX idx_reclamation_urgence_statut ON reclamation(urgence, statut);

-- Index pour la recherche textuelle
CREATE INDEX idx_reclamation_search ON reclamation USING gin(to_tsvector('french', sujet || ' ' || description));
CREATE INDEX idx_reclamation_reference_search ON reclamation(reference);

-- Index pour la table message_reclamation
CREATE INDEX idx_message_reclamation_id ON message_reclamation(reclamation_id);
CREATE INDEX idx_message_auteur ON message_reclamation(auteur);
CREATE INDEX idx_message_date_envoi ON message_reclamation(date_envoi);
CREATE INDEX idx_message_lu ON message_reclamation(lu);
CREATE INDEX idx_message_reclamation_auteur ON message_reclamation(reclamation_id, auteur);
CREATE INDEX idx_message_reclamation_lu ON message_reclamation(reclamation_id, lu);

-- Trigger pour mettre à jour automatiquement date_soumission
CREATE OR REPLACE FUNCTION update_date_soumission()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.statut = 'BROUILLON' AND NEW.statut = 'SOUMIS' THEN
        NEW.date_soumission = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_date_soumission
    BEFORE UPDATE ON reclamation
    FOR EACH ROW
    EXECUTE FUNCTION update_date_soumission();

-- Trigger pour mettre à jour automatiquement date_resolution
CREATE OR REPLACE FUNCTION update_date_resolution()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.statut != 'RESOLU' AND NEW.statut = 'RESOLU' THEN
        NEW.date_resolution = CURRENT_TIMESTAMP;
    ELSIF OLD.statut = 'RESOLU' AND NEW.statut != 'RESOLU' THEN
        NEW.date_resolution = NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_date_resolution
    BEFORE UPDATE ON reclamation
    FOR EACH ROW
    EXECUTE FUNCTION update_date_resolution();

-- Trigger pour mettre à jour date_lecture des messages
CREATE OR REPLACE FUNCTION update_date_lecture()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.lu = FALSE AND NEW.lu = TRUE THEN
        NEW.date_lecture = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_date_lecture
    BEFORE UPDATE ON message_reclamation
    FOR EACH ROW
    EXECUTE FUNCTION update_date_lecture();

-- Insertion de données de test
INSERT INTO reclamation (
    reference, type, categorie, sujet, description, urgence, 
    statut, email_user, nom_user, telephone_user
) VALUES 
(
    'REC-20260317-0001',
    'TECHNIQUE',
    'Erreur de connexion',
    'Problème de connexion au portail',
    'Je ne peux pas me connecter à mon espace contribuable depuis hier. Le message d''erreur indique "Identifiants invalides" alors que je suis certain de mes identifiants.',
    'HAUTE',
    'SOUMIS',
    'test@example.com',
    'Test User',
    '21612345678'
),
(
    'REC-20260317-0002',
    'FISCAL',
    'Impôt sur le revenu',
    'Question sur le calcul de l''impôt',
    'Je souhaiterais obtenir des éclaircissements sur le calcul de mon impôt sur le revenu pour l''année 2023. Le montant me semble élevé par rapport à mes revenus.',
    'MOYENNE',
    'EN_COURS',
    'user2@example.com',
    'User Two',
    '21698765432'
),
(
    'REC-20260317-0003',
    'DOCUMENT',
    'Facture manquante',
    'Recherche de facture perdue',
    'J''ai égaré ma facture de paiement pour l''année 2022 et j''en ai besoin pour ma comptabilité. Pouvez-vous me fournir une copie ?',
    'BASSE',
    'RESOLU',
    'user3@example.com',
    'User Three',
    '21655555555'
);

-- Insertion de messages de test
INSERT INTO message_reclamation (
    reclamation_id, contenu, auteur, lu
) VALUES 
(
    1,
    'Bonjour, je ne peux toujours pas me connecter malgré la réinitialisation de mon mot de passe.',
    'contribuable',
    TRUE
),
(
    1,
    'Bonjour, nous avons bien reçu votre demande. Notre équipe technique examine actuellement le problème. Pouvez-vous nous confirmer que vous utilisez bien le bon URL : https://smarttax.gov.tn ?',
    'agent',
    TRUE
),
(
    1,
    'Oui j''utilise bien ce URL. Le problème persiste depuis hier matin.',
    'contribuable',
    FALSE
),
(
    2,
    'Bonjour, je vous remercie pour votre demande. Pourriez-vous nous transmettre votre dernier avis d''imposition afin que nous puissions vérifier le calcul ?',
    'agent',
    FALSE
),
(
    3,
    'Votre facture a été envoyée à votre adresse email. Merci de vérifier votre boîte de réception.',
    'agent',
    TRUE
),
(
    3,
    'Merci beaucoup, j''ai bien reçu la facture.',
    'contribuable',
    TRUE
);

-- Création du répertoire pour les pièces jointes (note: cette commande doit être exécutée au niveau système)
-- mkdir -p uploads/reclamations

-- Commentaire sur la structure :
-- La table reclamation contient toutes les informations sur les réclamations
-- La table message_reclamation contient les échanges entre les contributeurs et les agents
-- Les pièces jointes sont stockées sous forme JSON dans les champs pieces_jointes et piece_jointe
-- Les triggers automatiques gèrent les mises à jour des dates importantes
-- Les index optimisent les performances des requêtes les plus fréquentes
