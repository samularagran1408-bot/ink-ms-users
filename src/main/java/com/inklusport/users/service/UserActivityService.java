package com.inklusport.users.service;

import com.inklusport.users.client.AuthServiceClient;
import com.inklusport.users.dto.AdminUserActivityItem;
import com.inklusport.users.dto.AdminUserActivityResponse;
import com.inklusport.users.dto.LoginAttemptResponse;
import com.inklusport.users.dto.RecordActivityRequest;
import com.inklusport.users.dto.UserActivityResponse;
import com.inklusport.users.entity.User;
import com.inklusport.users.entity.UserActivity;
import com.inklusport.users.repository.UserActivityRepository;
import com.inklusport.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de auditoría funcional del usuario (acciones sobre su perfil).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;
    private final AuthServiceClient authServiceClient;

    /**
     * Registra una actividad del usuario con acción, detalle e IP.
     */
    @Transactional
    public void logActivity(String email, String action, String details, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UserActivity activity = new UserActivity();
        activity.setUser(user);
        activity.setAction(action);
        activity.setDetails(toValidJsonDetails(details));
        activity.setIpAddress(ipAddress);

        userActivityRepository.save(activity);
        log.debug("Actividad registrada: {} - {}", email, action);
    }

    /**
     * Registra actividad sin propagar fallos (p. ej. tras actualizar perfil).
     */
    public void logActivityQuietly(String email, String action, String details, String ipAddress) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                log.warn("No se registró actividad {}: usuario {} no encontrado", action, email);
                return;
            }

            UserActivity activity = new UserActivity();
            activity.setUser(user);
            activity.setAction(action);
            activity.setDetails(toValidJsonDetails(details));
            activity.setIpAddress(ipAddress);
            userActivityRepository.saveAndFlush(activity);
        } catch (Exception ex) {
            log.warn("No se pudo registrar actividad {} para {}: {}", action, email, ex.getMessage());
        }
    }

    /**
     * Normaliza el detalle a JSON válido; envuelve texto plano si hace falta.
     */
    private String toValidJsonDetails(String details) {
        if (details == null || details.isBlank()) {
            return "{}";
        }
        String trimmed = details.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        String escaped = trimmed
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "{\"message\":\"" + escaped + "\"}";
    }

    /**
     * Lista las actividades de perfil del usuario, de la más reciente a la más antigua.
     */
    @Transactional(readOnly = true)
    public List<UserActivityResponse> getUserActivities(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return userActivityRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una actividad persistida a su DTO de respuesta.
     */
    private UserActivityResponse convertToResponse(UserActivity activity) {
        return UserActivityResponse.builder()
                .id(activity.getId())
                .action(activity.getAction())
                .details(activity.getDetails())
                .ipAddress(activity.getIpAddress())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    /**
     * Registra actividad interna (p. ej. login) y actualiza lastLoginAt si aplica.
     */
    @Transactional
    public void recordFromInternal(RecordActivityRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return;
        }
        String action = request.getAction() == null || request.getAction().isBlank()
                ? "LOGIN"
                : request.getAction().trim().toUpperCase();
        logActivityQuietly(request.getEmail(), action, request.getDetails(), request.getIpAddress());
        if ("LOGIN".equals(action)) {
            userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
            });
        }
    }

    /**
     * Combina actividad de perfil e historial de login para la vista admin.
     */
    @Transactional(readOnly = true)
    public AdminUserActivityResponse getAdminActivity(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<AdminUserActivityItem> items = new ArrayList<>();
        for (UserActivityResponse activity : getUserActivities(email)) {
            items.add(AdminUserActivityItem.builder()
                    .action(activity.getAction())
                    .details(activity.getDetails())
                    .ipAddress(activity.getIpAddress())
                    .createdAt(activity.getCreatedAt())
                    .source("PROFILE")
                    .build());
        }

        try {
            List<LoginAttemptResponse> logins = authServiceClient.getLoginHistory(email);
            if (logins != null) {
                for (LoginAttemptResponse login : logins) {
                    boolean ok = Boolean.TRUE.equals(login.getSuccessful());
                    items.add(AdminUserActivityItem.builder()
                            .action(ok ? "LOGIN" : "LOGIN_FAILED")
                            .details(ok ? "{\"message\":\"Ingreso al sistema\"}" : "{\"message\":\"Intento de ingreso fallido\"}")
                            .ipAddress(login.getIpAddress())
                            .createdAt(login.getAttemptTime())
                            .source("LOGIN")
                            .build());
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo obtener historial de login para {}: {}", email, ex.getMessage());
        }

        items.sort(Comparator.comparing(AdminUserActivityItem::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        LocalDateTime lastLogin = user.getLastLoginAt();
        try {
            var fromAuth = authServiceClient.getLastLogin(email);
            if (fromAuth != null && fromAuth.getLastLogin() != null) {
                lastLogin = fromAuth.getLastLogin();
            }
        } catch (Exception ex) {
            log.warn("No se pudo consultar último login de {}: {}", email, ex.getMessage());
        }

        return AdminUserActivityResponse.builder()
                .lastLoginAt(lastLogin)
                .items(items.stream().limit(50).toList())
                .build();
    }
}
