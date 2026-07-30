-- V3__disability_companion_support.sql
-- Campos de acompañante y preferencia de apoyo en el perfil del usuario.

ALTER TABLE user_profile
    ADD COLUMN companion_full_name VARCHAR(150) NULL,
    ADD COLUMN companion_phone VARCHAR(20) NULL,
    ADD COLUMN companion_relationship VARCHAR(80) NULL,
    ADD COLUMN companion_email VARCHAR(100) NULL,
    ADD COLUMN support_preference VARCHAR(50) NULL,
    ADD COLUMN support_preference_notes VARCHAR(255) NULL;
