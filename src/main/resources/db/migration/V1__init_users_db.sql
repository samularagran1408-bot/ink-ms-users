-- V1__init_users_db.sql

CREATE DATABASE user_ms;
USE user_ms;

-- Perfil completo del usuario
CREATE TABLE user_profile (
    id CHAR(36) PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100) UNIQUE NOT NULL,
    profile_picture TEXT,
    bio TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Roles
CREATE TABLE role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- Usuario - Rol
CREATE TABLE user_role (
    user_id CHAR(36),
    role_id INT,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES user_profile(id),
    FOREIGN KEY (role_id) REFERENCES role(id)
);

-- Historial de actividad del usuario
CREATE TABLE user_activity (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_profile(id)
);

alter table user_profile add disability VARCHAR(100);

describe user_profile;

select * from user_profile;
select * from role;
select * from user_role;

INSERT INTO role (id, name, description) VALUES 
(3, 'ENTRENADOR', 'Especializado en ejercicios y prevención de lesiones'),
(4, 'ORGANIZADOR', 'Encargado de asignar eventos');

INSERT INTO user_role(user_id, role_id) VALUES ('b9d66506-a5a4-4bba-8213-6fe591994a84', 1);
INSERT INTO user_role(user_id, role_id) VALUES ('e0e37e7b-e00b-43d9-b8fa-19bb38849247', 1);
INSERT INTO user_role(user_id, role_id) VALUES ('8dab4173-7997-4074-9125-16e7371dfd29', 1);
INSERT INTO user_role(user_id, role_id) VALUES ('7ac705cf-2186-4554-8ad3-f01497580c9a', 1);