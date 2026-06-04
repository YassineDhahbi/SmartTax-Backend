ALTER TABLE utilisateur
    ADD COLUMN IF NOT EXISTS comment_blocked_until TIMESTAMP NULL;

ALTER TABLE utilisateur
    ADD COLUMN IF NOT EXISTS comment_block_reason VARCHAR(500) NULL;
