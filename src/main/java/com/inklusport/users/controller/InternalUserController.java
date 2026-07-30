package com.inklusport.users.controller;

import com.inklusport.users.dto.CreateProfileFromRegisterRequest;
import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.service.RoleService;
import com.inklusport.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints internos para consumo entre microservicios.
 * Flujo:
 * 1) Resolucion de roles por email para autorizacion interna
 * 2) Alta de perfil desde registro (auth → users)
 * 3) Consulta de perfil por ID (AI assistant y otros MS)
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

    // ===== Bloque 2: Perfil desde registro =====
    /**
     * Crea el perfil con discapacidad / acompañante / preferencia de apoyo
     * tras un POST /api/auth/register exitoso.
     */
    @PostMapping("/profile-from-register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse createProfileFromRegister(
            @Valid @RequestBody CreateProfileFromRegisterRequest request) {
        return userService.createProfileFromRegister(request);
    }

    // ===== Bloque 3: Perfil por ID =====
    /**
     * Perfil de usuario por ID para microservicios (p. ej. ink-ms-ai-assistant).
     */
    @GetMapping("/{id}")
    public UserProfileResponse getUserById(@PathVariable String id) {
        return userService.getUserProfileById(id);
    }
}
