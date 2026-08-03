package com.inklusport.users.service;

import com.inklusport.users.dto.UserActivityResponse;
import com.inklusport.users.entity.User;
import com.inklusport.users.entity.UserActivity;
import com.inklusport.users.repository.UserActivityRepository;
import com.inklusport.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<UserActivityResponse> getUserActivities(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return userActivityRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private UserActivityResponse convertToResponse(UserActivity activity) {
        return UserActivityResponse.builder()
                .id(activity.getId())
                .action(activity.getAction())
                .details(activity.getDetails())
                .ipAddress(activity.getIpAddress())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
