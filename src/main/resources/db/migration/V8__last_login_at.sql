-- Último acceso del usuario para el panel admin.
ALTER TABLE user_profile
    ADD COLUMN last_login_at DATETIME NULL;
