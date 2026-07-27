package com.inklusport.users.controller;

import com.inklusport.users.dto.AssignRoleRequest;
import com.inklusport.users.dto.AssignRoleResponse;
import com.inklusport.users.dto.RoleResponse;
import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.common.dto.response.ErrorResponse;
import com.inklusport.users.service.RoleService;
import com.inklusport.users.service.UserService;
import com.inklusport.users.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints administrativos para gestión de usuarios y roles.
 * Requiere rol ADMIN en toda la clase.
 * Flujo:
 * 1. Consulta y estado de usuarios
 * 2. Gestión de roles
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final RoleService roleService;
    private final UserRepository userRepository;

    /**
     * Bloque 1: Consulta y estado de usuarios
     * @return
     */

    /**
     * Lista todos los usuarios registrados.
     * GET /api/admin/users
     */
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Lista solo usuarios activos.
     * GET /api/admin/users/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserProfileResponse>> getActiveUsers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }

    /**
     * Desactiva un usuario por email.
     * POST /api/admin/users/{email}/deactivate
     */
    @PostMapping("/{email}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable String email) {
        try {
            userService.deactivateUser(email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + email + "/deactivate");
        }
    }

    /**
     * Activa un usuario por email.
     * POST /api/admin/users/{email}/activate
     */
    @PostMapping("/{email}/activate")
    public ResponseEntity<?> activateUser(@PathVariable String email) {
        try {
            userService.activateUser(email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + email + "/activate");
        }
    }

    /**
     * Bloque 2: Gestión de roles
     */

    /**
     * Lista los roles disponibles en el sistema.
     * GET /api/admin/users/roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * Cuenta total de usuarios.
     * GET /api/admin/users/count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countUsers() {
        long count = userRepository.count();
        return ResponseEntity.ok(count);
    }

    /**
     * Cuenta usuarios activos.
     * GET /api/admin/users/active/count
     */
    @GetMapping("/active/count")
    public ResponseEntity<Long> countActiveUsers() {
        long count = userRepository.countByIsActiveTrue();
        return ResponseEntity.ok(count);
    }

    /**
     * Asigna un rol a un usuario.
     * POST /api/admin/users/{email}/roles
     */
    @PostMapping("/{email}/roles")
    public ResponseEntity<?> assignRole(
            @PathVariable String email,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal String adminEmail) {
        String targetEmail = decodeEmail(email);
        try {
            AssignRoleResponse response = roleService.assignRoleToUser(targetEmail, request, adminEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + targetEmail + "/roles");
        }
    }

    /**
     * Consulta los roles de un usuario por email.
     * GET /api/admin/users/roles-by-email?email=user@example.com
     */
    @GetMapping("/roles-by-email")
    public ResponseEntity<List<String>> getUserRoles(@RequestParam String email) {
        List<String> roles = roleService.getUserRoles(email);
        return ResponseEntity.ok(roles);
    }

    /**
     * Remueve un rol específico de un usuario.
     * DELETE /api/admin/users/{email}/roles/{roleId}
     */
    @DeleteMapping("/{email}/roles/{roleId}")
    public ResponseEntity<?> removeRole(
            @PathVariable String email,
            @PathVariable Long roleId) {
        try {
            roleService.removeRoleFromUser(email, roleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + email + "/roles/" + roleId);
        }
    }

    /**
     * Verifica si un usuario existe por email.
     * GET /api/admin/users/{email}/exists
     */
    @GetMapping("/{email}/exists")
    public ResponseEntity<Boolean> userExists(@PathVariable String email) {
        return ResponseEntity.ok(userService.userExists(email));
    }

    /**
     * Métodos auxiliares
     * @param e
     * @param path
     * @return
     */

    /**
     * Construye una respuesta de error en formato JSON estandarizado.
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception e, String path) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(e.getMessage())
                .path(path)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Decodifica un email codificado en URL.
     */
    private static String decodeEmail(String email) {
        return URLDecoder.decode(email, StandardCharsets.UTF_8);
    }
}