package com.inklusport.users.config;

import com.inklusport.users.entity.Role;
import com.inklusport.users.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RF27: asegura el catálogo de roles (admin, usuario, organizador, entrenador).
 */
@Component
@RequiredArgsConstructor
@Order(1)
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        ensureRole("ADMIN", "Administrador del sistema");
        ensureRole("USUARIO", "Usuario de la plataforma");
        ensureRole("ORGANIZADOR", "Encargado de los eventos");
        ensureRole("ENTRENADOR", "Especializado en prevención de lesiones y entrenamiento");
    }

    private void ensureRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            return;
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
    }
}
