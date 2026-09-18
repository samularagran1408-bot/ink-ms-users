package com.inklusport.users.service;

import com.inklusport.users.dto.AssignRoleRequest;
import com.inklusport.users.dto.PageResponse;
import com.inklusport.users.dto.RoleRequestResponse;
import com.inklusport.users.entity.RoleRequest;
import com.inklusport.users.entity.User;
import com.inklusport.users.repository.RoleRequestRepository;
import com.inklusport.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Solicitudes de rol elevado al registrarse (primera vez).
 * USUARIO se asigna siempre; ENTRENADOR/ORGANIZADOR requieren aprobación admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleRequestService {

    private static final Set<String> SELF_REQUESTABLE_ELEVATED = Set.of("ENTRENADOR", "ORGANIZADOR");

    private final RoleRequestRepository roleRequestRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final AdminNotificationService adminNotificationService;
    private final AdminAuditService adminAuditService;

    /**
     * Crea solicitud pendiente si el rol es ENTRENADOR u ORGANIZADOR y no hay otra pendiente.
     * USUARIO (o vacío) no genera solicitud.
     */
    @Transactional
    public Optional<RoleRequest> createIfElevatedRoleRequested(User user, String rawRequestedRole) {
        String role = normalizeSelfRequestableRole(rawRequestedRole);
        if (role == null || "USUARIO".equals(role)) {
            return Optional.empty();
        }

        if (roleRequestRepository.existsByUserIdAndStatus(user.getId(), RoleRequest.Status.PENDING)) {
            return roleRequestRepository.findFirstByUserIdAndStatusOrderByRequestedAtDesc(
                    user.getId(), RoleRequest.Status.PENDING);
        }

        if (roleRequestRepository.existsByUserIdAndRequestedRoleAndStatus(
                user.getId(), role, RoleRequest.Status.APPROVED)) {
            log.info("Usuario {} ya tiene solicitud aprobada para {}; no se crea otra", user.getEmail(), role);
            return Optional.empty();
        }

        RoleRequest request = new RoleRequest();
        request.setUserId(user.getId());
        request.setUserEmail(user.getEmail());
        request.setUserFullName(user.getFullName());
        request.setRequestedRole(role);
        request.setStatus(RoleRequest.Status.PENDING);

        RoleRequest saved = roleRequestRepository.save(request);
        log.info("Solicitud de rol {} creada para {}", role, user.getEmail());

        try {
            adminNotificationService.notifyAdminsRoleRequested(
                    user.getEmail(), user.getFullName(), role);
        } catch (Exception e) {
            log.warn("No se pudo notificar a admins de solicitud de rol {}: {}", user.getEmail(), e.getMessage());
        }

        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public Optional<RoleRequestResponse> findPendingForUser(String userId) {
        return roleRequestRepository
                .findFirstByUserIdAndStatusOrderByRequestedAtDesc(userId, RoleRequest.Status.PENDING)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleRequestResponse> listByStatus(String statusRaw, int page, int size) {
        RoleRequest.Status status = parseStatus(statusRaw);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Page<RoleRequest> result = roleRequestRepository.findByStatusOrderByRequestedAtDesc(
                status, PageRequest.of(safePage, safeSize));
        return PageResponse.of(result, result.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional
    public RoleRequestResponse approve(String requestId, String adminEmail, String notes, String ipAddress) {
        RoleRequest request = getPendingOrThrow(requestId);

        AssignRoleRequest assign = new AssignRoleRequest();
        assign.setRoleName(request.getRequestedRole());
        roleService.assignRoleToUser(request.getUserEmail(), assign, adminEmail, ipAddress);

        request.setStatus(RoleRequest.Status.APPROVED);
        request.setReviewedBy(adminEmail);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNotes(trimToNull(notes));
        RoleRequest saved = roleRequestRepository.save(request);

        adminAuditService.log(adminEmail, "APPROVE_ROLE_REQUEST", request.getUserEmail(),
                request.getUserId(),
                "{\"role\":\"" + request.getRequestedRole() + "\",\"requestId\":\"" + requestId + "\"}",
                ipAddress);

        try {
            adminNotificationService.notifyUserRoleRequestResolved(
                    request.getUserEmail(),
                    request.getRequestedRole(),
                    true);
        } catch (Exception e) {
            log.warn("No se pudo notificar al usuario {} de aprobación de rol: {}",
                    request.getUserEmail(), e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public RoleRequestResponse reject(String requestId, String adminEmail, String notes, String ipAddress) {
        RoleRequest request = getPendingOrThrow(requestId);

        request.setStatus(RoleRequest.Status.REJECTED);
        request.setReviewedBy(adminEmail);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNotes(trimToNull(notes));
        RoleRequest saved = roleRequestRepository.save(request);

        adminAuditService.log(adminEmail, "REJECT_ROLE_REQUEST", request.getUserEmail(),
                request.getUserId(),
                "{\"role\":\"" + request.getRequestedRole() + "\",\"requestId\":\"" + requestId + "\"}",
                ipAddress);

        try {
            adminNotificationService.notifyUserRoleRequestResolved(
                    request.getUserEmail(),
                    request.getRequestedRole(),
                    false);
        } catch (Exception e) {
            log.warn("No se pudo notificar al usuario {} de rechazo de rol: {}",
                    request.getUserEmail(), e.getMessage());
        }

        return toResponse(saved);
    }

    /**
     * Normaliza USUARIO / ENTRENADOR / ORGANIZADOR; null si vacío o inválido.
     */
    public String normalizeSelfRequestableRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("USUARIO".equals(normalized) || "USER".equals(normalized) || "ATLETA".equals(normalized)) {
            return "USUARIO";
        }
        if ("ORGANIZER".equals(normalized) || "ORGANIZADOR".equals(normalized)) {
            return "ORGANIZADOR";
        }
        if ("TRAINER".equals(normalized) || "COACH".equals(normalized) || "ENTRENADOR".equals(normalized)) {
            return "ENTRENADOR";
        }
        if (SELF_REQUESTABLE_ELEVATED.contains(normalized)) {
            return normalized;
        }
        throw new RuntimeException(
                "Rol solicitado inválido: " + raw + ". Valores: USUARIO, ENTRENADOR, ORGANIZADOR");
    }

    private RoleRequest getPendingOrThrow(String requestId) {
        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud de rol no encontrada: " + requestId));
        if (request.getStatus() != RoleRequest.Status.PENDING) {
            throw new RuntimeException("La solicitud ya fue procesada (" + request.getStatus() + ")");
        }
        if (!userRepository.existsById(request.getUserId())) {
            throw new RuntimeException("El usuario de la solicitud ya no existe");
        }
        return request;
    }

    private RoleRequest.Status parseStatus(String statusRaw) {
        if (statusRaw == null || statusRaw.isBlank()) {
            return RoleRequest.Status.PENDING;
        }
        try {
            return RoleRequest.Status.valueOf(statusRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado inválido. Use PENDING, APPROVED o REJECTED");
        }
    }

    private RoleRequestResponse toResponse(RoleRequest request) {
        return RoleRequestResponse.builder()
                .id(request.getId())
                .userEmail(request.getUserEmail())
                .userFullName(request.getUserFullName())
                .requestedRole(request.getRequestedRole())
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .requestedAt(request.getRequestedAt())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .reviewNotes(request.getReviewNotes())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
