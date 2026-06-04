-- Documents du centre de telechargement (categories fixes en colonne enum).
-- Hibernate peut aussi creer la table (ddl-auto=update) ; cette migration aligne la base si Flyway est active.

CREATE TABLE IF NOT EXISTS download_library_document (
    id                  BIGSERIAL PRIMARY KEY,
    category            VARCHAR(32)  NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    external_url        VARCHAR(2000),
    stored_file_name    VARCHAR(500),
    original_file_name  VARCHAR(500),
    content_type        VARCHAR(255),
    file_size_bytes     BIGINT,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_download_doc_category ON download_library_document (category);

COMMENT ON TABLE download_library_document IS 'Fichiers et liens du centre de telechargement (Formulaires, Guides, Lois, Modeles)';
