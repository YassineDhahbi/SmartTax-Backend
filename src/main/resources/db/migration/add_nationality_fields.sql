-- Ajouter les champs ville, autreVille et nationalite à la table immatriculation
-- Migration pour la gestion de la nationalité et des villes

ALTER TABLE immatriculation 
ADD COLUMN IF NOT EXISTS ville VARCHAR(100),
ADD COLUMN IF NOT EXISTS autreVille VARCHAR(100),
ADD COLUMN IF NOT EXISTS nationalite VARCHAR(50) DEFAULT 'tunisienne';

-- Ajouter des commentaires pour les nouveaux champs
COMMENT ON COLUMN immatriculation.ville IS 'Ville ou délégation du contribuable';
COMMENT ON COLUMN immatriculation.autreVille IS 'Ville personnalisée si non présente dans la liste';
COMMENT ON COLUMN immatriculation.nationalite IS 'Nationalité du contribuable (tunisienne, française, etc.)';

-- Créer un index pour optimiser les recherches par nationalité
CREATE INDEX IF NOT EXISTS idx_immatriculation_nationalite ON immatriculation(nationalite);
CREATE INDEX IF NOT EXISTS idx_immatriculation_ville ON immatriculation(ville);

-- Mettre à jour les enregistrements existants avec la nationalité par défaut
UPDATE immatriculation 
SET nationalite = 'tunisienne' 
WHERE nationalite IS NULL OR nationalite = '';
