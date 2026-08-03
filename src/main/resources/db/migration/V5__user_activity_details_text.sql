-- La columna details era JSON y rechazaba texto plano al registrar actividad de perfil.
ALTER TABLE user_activity
    MODIFY COLUMN details TEXT NULL;
