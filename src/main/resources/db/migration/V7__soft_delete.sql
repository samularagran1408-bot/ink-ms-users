-- Eliminación lógica de usuarios (HU08)

ALTER TABLE user_profile
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at DATETIME NULL;
