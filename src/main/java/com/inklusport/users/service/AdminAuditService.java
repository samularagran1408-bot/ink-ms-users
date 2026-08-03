package com.inklusport.users.service;

import com.inklusport.users.dto.AdminAuditLogResponse;
import com.inklusport.users.entity.AdminAuditLog;
import com.inklusport.users.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RF29: historial de acciones administrativas (eliminaciones, ediciones, accesos, bloqueos, roles).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional
    public void log(String adminEmail, String action, String targetEmail, String targetUserId,
                    String details, String ipAddress) {
        AdminAuditLog entry = new AdminAuditLog();
        entry.setAdminEmail(adminEmail != null ? adminEmail : "SYSTEM");
        entry.setAction(action);
        entry.setTargetEmail(targetEmail);
        entry.setTargetUserId(targetUserId);
        entry.setDetails(details != null ? details : "{}");
        entry.setIpAddress(ipAddress);
        adminAuditLogRepository.save(entry);
        log.info("Auditoría admin: {} → {} ({})", entry.getAdminEmail(), action, targetEmail);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> getAll() {
        return adminAuditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> getByTargetEmail(String email) {
        return adminAuditLogRepository.findByTargetEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> getByAdminEmail(String adminEmail) {
        return adminAuditLogRepository.findByAdminEmailOrderByCreatedAtDesc(adminEmail).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog log) {
        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .adminEmail(log.getAdminEmail())
                .action(log.getAction())
                .targetEmail(log.getTargetEmail())
                .targetUserId(log.getTargetUserId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
