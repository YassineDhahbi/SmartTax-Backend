-- Alignement BDD / entit� Reclamation.etatReclamation (dashboard agent : stats + liste filtr�e)
ALTER TABLE reclamation
    ADD COLUMN IF NOT EXISTS etat_reclamation VARCHAR(20) NOT NULL DEFAULT 'EN_COURS';

ALTER TABLE reclamation
    DROP CONSTRAINT IF EXISTS chk_reclamation_etat;

ALTER TABLE reclamation
    ADD CONSTRAINT chk_reclamation_etat CHECK (etat_reclamation IN ('EN_COURS', 'TRAITE'));

CREATE INDEX IF NOT EXISTS idx_reclamation_statut_etat ON reclamation (statut, etat_reclamation);
