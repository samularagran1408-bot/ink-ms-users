package com.inklusport.users.config;

import com.inklusport.users.entity.Role;
import com.inklusport.users.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (roleRepository.count() > 0) {
            return;
        }
        roleRepository.save(role("ADMIN", "Administrador del sistema"));
        roleRepository.save(role("USUARIO", "Usuario de la plataforma"));
    }

    private static Role role(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        return role;
    }
}

