-- Correction définitive de la contrainte de rôle
-- Version PostgreSQL compatible

-- 1. Supprimer l'ancienne contrainte
ALTER TABLE utilisateur DROP CONSTRAINT IF EXISTS utilisateur_role_check;

-- 2. Ajouter la nouvelle contrainte avec tous les rôles
ALTER TABLE utilisateur 
ADD CONSTRAINT utilisateur_role_check 
CHECK (role IN ('ADMIN', 'AGENT', 'CONTRIBUABLE'));

-- 3. Mettre à jour les enregistrements qui pourraient avoir des valeurs invalides
UPDATE utilisateur 
SET role = 'CONTRIBUABLE' 
WHERE role NOT IN ('ADMIN', 'AGENT', 'CONTRIBUABLE');

-- 4. Vérifier la contrainte
SELECT conname, consrc 
FROM pg_constraint 
WHERE conname = 'utilisateur_role_check';

-- 5. Afficher les rôles actuels dans la base
SELECT DISTINCT role FROM utilisateur;

-- 6. Créer un utilisateur admin de test si nécessaire
INSERT INTO utilisateur (first_name, last_name, email, password, role, status, date_inscription, date_naissance, cin_confidence, cin_validation_status)
SELECT 
    'Admin', 
    'SmartTax', 
    'admin@smarttax.gov.tn', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- admin123
    'ADMIN', 
    'ACTIVE',
    CURRENT_DATE,
    '1990-01-01',
    0.0,
    'valid'
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'admin@smarttax.gov.tn');

-- 7. Créer un utilisateur agent de test si nécessaire
INSERT INTO utilisateur (first_name, last_name, email, password, role, status, date_inscription, date_naissance, cin_confidence, cin_validation_status)
SELECT 
    'Agent', 
    'DGI', 
    'agent@smarttax.gov.tn', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- agent123
    'AGENT', 
    'ACTIVE',
    CURRENT_DATE,
    '1990-01-01',
    0.0,
    'valid'
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'agent@smarttax.gov.tn');

-- 8. Afficher les comptes créés
SELECT 
    email, 
    first_name, 
    last_name, 
    role, 
    status,
    date_inscription
FROM utilisateur 
WHERE email IN ('admin@smarttax.gov.tn', 'agent@smarttax.gov.tn');
