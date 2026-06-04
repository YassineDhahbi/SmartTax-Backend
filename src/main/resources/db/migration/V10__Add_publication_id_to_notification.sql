ALTER TABLE notification
    ADD COLUMN IF NOT EXISTS publication_id BIGINT NULL;
