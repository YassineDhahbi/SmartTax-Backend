-- Compteur de telechargements par document (fichier API + enregistrement lien externe).

ALTER TABLE download_library_document
ADD COLUMN IF NOT EXISTS download_count BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN download_library_document.download_count IS 'Nombre de telechargements enregistres';
