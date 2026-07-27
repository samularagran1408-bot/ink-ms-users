package com.inklusport.users.controller;

import com.inklusport.users.dto.UpdateProfileRequest;
import com.inklusport.users.dto.UserActivityResponse;
import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.common.dto.response.ErrorResponse;
import com.inklusport.users.service.UserService;
import com.inklusport.users.service.UserActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints del usuario autenticado.
 * Flujo:
 * 1) Gestion del perfil
 * 2) Historial de actividad
 * 3) Consulta puntual por identificador
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserActivityService userActivityService;

    // ===== Bloque 1: Gestion del perfil =====
    /**
     * Crea el perfil base del usuario autenticado.
     * Si llegan campos adicionales, completa los datos en la misma operacion.
     */
    @PostMapping("/perfil")
    public ResponseEntity<?> createMyProfile(@AuthenticationPrincipal String email,
                                              @Valid @RequestBody UpdateProfileRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            UserProfileResponse response = userService.createUserProfile(email, request.getFullName());
            
            // Actualiza campos opcionales si fueron enviados en el request.
            if (request.getPhone() != null || 
                request.getProfilePicture() != null || request.getBio() != null || request.getDisability() != null) {
                response = userService.updateUserProfile(email, request);
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/perfil");
        }
    }

    /**
     * Consulta el perfil del usuario autenticado.
     */
    @GetMapping("/perfil")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal String email) {
        try {
            UserProfileResponse response = userService.getUserProfileByEmail(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/perfil");
        }
    }

    /**
     * Actualiza el perfil del usuario autenticado y registra actividad.
     */
    @PutMapping("/perfil")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal String email,
                                              @Valid @RequestBody UpdateProfileRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            UserProfileResponse response = userService.updateUserProfile(email, request);

            userActivityService.logActivity(email, "UPDATE_PROFILE", 
                "Perfil actualizado", httpRequest.getRemoteAddr());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/perfil");
        }
    }

    // ===== Bloque 2: Historial de actividad =====
    /**
     * Obtiene el historial de actividades del usuario autenticado.
     */
    @GetMapping("/perfil/activities")
    public ResponseEntity<?> getMyActivities(@AuthenticationPrincipal String email) {
        try {
            List<UserActivityResponse> response = userActivityService.getUserActivities(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/perfil/activities");
        }
    }

    // ===== Bloque 3: Consulta puntual =====
    /**
     * Consulta un perfil por identificador.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        try {
            UserProfileResponse response = userService.getUserProfileById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/" + id);
        }
    }


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
}
