-- Compatible con MySQL 8: columnas de bloqueo, auditoría admin y config global.
-- Nota: con ddl-auto=update Hibernate también aplica el esquema en arranque.

ALTER TABLE user_profile
    ADD COLUMN block_reason VARCHAR(500) NULL,
    ADD COLUMN blocked_until DATETIME NULL,
    ADD COLUMN blocked_permanently BOOLEAN DEFAULT FALSE;

ALTER TABLE user_role
    ADD COLUMN assigned_by VARCHAR(100) NULL;

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id CHAR(36) PRIMARY KEY,
    admin_email VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_email VARCHAR(100),
    target_user_id CHAR(36),
    details TEXT,
    ip_address VARCHAR(45),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_audit_admin (admin_email),
    INDEX idx_admin_audit_target (target_email),
    INDEX idx_admin_audit_created (created_at)
);

CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    updated_by VARCHAR(100),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO system_config (config_key, config_value, description, updated_by) VALUES
('max_login_attempts', '5', 'Intentos fallidos de login antes de bloqueo temporal', 'SYSTEM'),
('block_duration_minutes', '15', 'Minutos de bloqueo por fuerza bruta', 'SYSTEM'),
('session_timeout_minutes', '1440', 'Duración máxima de sesión en minutos', 'SYSTEM'),
('password_min_length', '8', 'Longitud mínima de contraseña', 'SYSTEM'),
('registration_enabled', 'true', 'Permite nuevos registros en la plataforma', 'SYSTEM'),
('maintenance_mode', 'false', 'Modo mantenimiento: restringe operaciones no admin', 'SYSTEM'),
('max_events_per_organizer', '50', 'Límite de eventos activos por organizador', 'SYSTEM'),
('default_event_capacity', '30', 'Cupo por defecto al crear un evento', 'SYSTEM'),
('waitlist_enabled', 'true', 'Habilita listas de espera cuando se agotan cupos', 'SYSTEM'),
('waitlist_notification_enabled', 'true', 'Notificar a usuarios en espera cuando hay cupo', 'SYSTEM');
