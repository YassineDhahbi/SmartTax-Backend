ALTER TABLE notification
    ADD COLUMN IF NOT EXISTS reclamation_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_notification_reclamation_id ON notification (reclamation_id);
