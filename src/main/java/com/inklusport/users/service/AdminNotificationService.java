package com.inklusport.users.service;

import com.inklusport.users.client.NotificationServiceClient;
import com.inklusport.users.dto.NotificationRequest;
import com.inklusport.users.entity.UserRole;
import com.inklusport.users.repository.RoleRepository;
import com.inklusport.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Notificaciones administrativas desde el microservicio de usuarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final NotificationServiceClient notificationClient;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Value("${notifications.admin-emails:}")
    private String adminEmailsConfig;

    public void notifyAdminsNewUserRegistered(String email, String fullName) {
        String displayName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : email;
        notifyAdmins(
                "admin_user_registered",
                "Nuevo usuario registrado",
                "El usuario " + displayName + " (" + email + ") se registró por primera vez.",
                null
        );
    }

    public void notifyAdmins(String type, String title, String body, String eventId) {
        Set<String> recipients = resolveAdminEmails();
        if (recipients.isEmpty()) {
            log.debug("Sin destinatarios admin para [{}]", type);
            return;
        }
        for (String adminEmail : recipients) {
            try {
                NotificationRequest request = new NotificationRequest();
                request.setUserId(adminEmail);
                request.setType(type);
                request.setTitle(title);
                request.setBody(body);
                request.setEventId(eventId);
                request.setPriority("medium");
                notificationClient.createNotification(adminEmail, request);
                log.info("Notificación admin enviada a {} [{}]", adminEmail, type);
            } catch (Exception e) {
                log.error("Error notificando admin {}: {}", adminEmail, e.getMessage());
            }
        }
    }

    public List<String> getEmailsByRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return List.of();
        }
        return roleRepository.findByName(roleName.trim().toUpperCase(Locale.ROOT))
                .map(role -> userRoleRepository.findByRoleId(role.getId()).stream()
                        .map(UserRole::getUser)
                        .filter(user -> user != null && user.getEmail() != null)
                        .map(user -> user.getEmail().trim())
                        .filter(email -> email.contains("@"))
                        .distinct()
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    private Set<String> resolveAdminEmails() {
        Set<String> emails = new LinkedHashSet<>();

        if (adminEmailsConfig != null && !adminEmailsConfig.isBlank()) {
            Arrays.stream(adminEmailsConfig.split(","))
                    .map(String::trim)
                    .filter(email -> email.contains("@"))
                    .map(email -> email.toLowerCase(Locale.ROOT))
                    .forEach(emails::add);
        }

        getEmailsByRole("ADMIN").stream()
                .map(email -> email.toLowerCase(Locale.ROOT))
                .forEach(emails::add);

        return emails;
    }
}
