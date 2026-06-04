-- Ajouter les champs téléphone, département et adresse pour les agents
-- Script de migration pour la table utilisateur

-- Ajout du champ téléphone
ALTER TABLE utilisateur 
ADD COLUMN telephone VARCHAR(20) NULL 
COMMENT 'Numéro de téléphone de l\'utilisateur';

-- Ajout du champ département  
ALTER TABLE utilisateur 
ADD COLUMN departement VARCHAR(100) NULL 
COMMENT 'Département de l\'agent (ex: Centre des Impôts)';

-- Ajout du champ adresse
ALTER TABLE utilisateur 
ADD COLUMN adresse VARCHAR(255) NULL 
COMMENT 'Adresse de l\'utilisateur';

-- Index pour optimiser les recherches (optionnel)
CREATE INDEX idx_utilisateur_telephone ON utilisateur(telephone);
CREATE INDEX idx_utilisateur_departement ON utilisateur(departement);

-- Commentaire sur la table
ALTER TABLE utilisateur COMMENT 'Table des utilisateurs avec champs supplémentaires pour les agents';
