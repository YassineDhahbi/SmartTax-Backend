CREATE TABLE IF NOT EXISTS publication_report (
    id BIGSERIAL PRIMARY KEY,
    publication_id BIGINT NOT NULL,
    user_id INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_publication_report_publication_id ON publication_report(publication_id);
CREATE INDEX IF NOT EXISTS idx_publication_report_user_id ON publication_report(user_id);

ALTER TABLE publication_report
    ADD CONSTRAINT fk_publication_report_publication
    FOREIGN KEY (publication_id) REFERENCES publication(id) ON DELETE CASCADE;

ALTER TABLE publication_report
    ADD CONSTRAINT fk_publication_report_user
    FOREIGN KEY (user_id) REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE;
