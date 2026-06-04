-- Migration pour ajouter les champs formeJuridique et actionnaire à la table immatriculation
-- Date: 2026-04-14
-- Auteur: SmartTax Team

-- Ajouter le champ formeJuridique pour les personnes morales
ALTER TABLE immatriculation 
ADD COLUMN forme_juridique VARCHAR(50) NULL 
COMMENT 'Forme juridique de l''entreprise (SARL, EURL, SA, etc.)';

-- Ajouter le champ actionnaire pour les personnes morales
ALTER TABLE immatriculation 
ADD COLUMN actionnaire VARCHAR(100) NULL 
COMMENT 'Nom de l''actionnaire principal de l''entreprise';

-- Mettre à jour les enregistrements existants si nécessaire
-- (Optionnel) Décommenter si vous voulez initialiser avec des valeurs par défaut
-- UPDATE immatriculation 
-- SET forme_juridique = 'SARL' 
-- WHERE type_contribuable = 'MORALE' AND forme_juridique IS NULL;

-- Vérifier que les colonnes ont été ajoutées correctement
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'immatriculation' 
  AND COLUMN_NAME IN ('forme_juridique', 'actionnaire')
ORDER BY COLUMN_NAME;
