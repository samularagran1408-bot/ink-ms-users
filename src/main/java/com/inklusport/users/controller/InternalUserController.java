package com.inklusport.users.controller;

import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.service.RoleService;
import com.inklusport.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints internos para consumo entre microservicios.
 * Flujo:
 * 1) Resolucion de roles por email para autorizacion interna
 * 2) Consulta de perfil por ID (AI assistant y otros MS)
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final RoleService roleService;
    private final UserService userService;

    // ===== Bloque 1: Resolucion de roles =====
    /**
     * Retorna los roles de un usuario por correo para validaciones internas.
     */
    @GetMapping("/roles-by-email")
    public List<String> getUserRoles(@RequestParam String email) {
        return roleService.getUserRoles(email);
    }

    // ===== Bloque 2: Perfil por ID =====
    /**
     * Perfil de usuario por ID para microservicios (p. ej. ink-ms-ai-assistant).
     */
    @GetMapping("/{id}")
    public UserProfileResponse getUserById(@PathVariable String id) {
        return userService.getUserProfileById(id);
    }
}
