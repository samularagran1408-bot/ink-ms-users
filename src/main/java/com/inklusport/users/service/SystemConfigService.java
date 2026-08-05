package com.inklusport.users.service;

import com.inklusport.users.dto.SystemConfigResponse;
import com.inklusport.users.dto.UpdateSystemConfigRequest;
import com.inklusport.users.entity.SystemConfig;
import com.inklusport.users.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * parámetros globales del sistema (políticas, límites, ajustes de operación).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class SystemConfigService implements ApplicationRunner {

    private final SystemConfigRepository systemConfigRepository;
    private final AdminAuditService adminAuditService;

    private static final Map<String, String[]> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("max_login_attempts", new String[]{"5", "Intentos fallidos de login antes de bloqueo temporal"});
        DEFAULTS.put("block_duration_minutes", new String[]{"15", "Minutos de bloqueo por fuerza bruta"});
        DEFAULTS.put("session_timeout_minutes", new String[]{"1440", "Duración máxima de sesión en minutos"});
        DEFAULTS.put("password_min_length", new String[]{"8", "Longitud mínima de contraseña"});
        DEFAULTS.put("registration_enabled", new String[]{"true", "Permite nuevos registros en la plataforma"});
        DEFAULTS.put("maintenance_mode", new String[]{"false", "Modo mantenimiento: restringe operaciones no admin"});
        DEFAULTS.put("max_events_per_organizer", new String[]{"50", "Límite de eventos activos por organizador"});
        DEFAULTS.put("default_event_capacity", new String[]{"30", "Cupo por defecto al crear un evento"});
        DEFAULTS.put("waitlist_enabled", new String[]{"true", "Habilita listas de espera cuando se agotan cupos"});
        DEFAULTS.put("waitlist_notification_enabled", new String[]{"true", "Notificar a usuarios en espera cuando hay cupo"});
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDefaults();
    }

    @Transactional
    public void seedDefaults() {
        DEFAULTS.forEach((key, meta) -> {
            if (!systemConfigRepository.existsById(key)) {
                SystemConfig cfg = new SystemConfig();
                cfg.setKey(key);
                cfg.setValue(meta[0]);
                cfg.setDescription(meta[1]);
                cfg.setUpdatedBy("SYSTEM");
                systemConfigRepository.save(cfg);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<SystemConfigResponse> getAll() {
        return systemConfigRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SystemConfigResponse getByKey(String key) {
        SystemConfig cfg = systemConfigRepository.findById(key)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada: " + key));
        return toResponse(cfg);
    }

    @Transactional
    public SystemConfigResponse update(String key, UpdateSystemConfigRequest request,
                                       String adminEmail, String ipAddress) {
        SystemConfig cfg = systemConfigRepository.findById(key).orElseGet(() -> {
            SystemConfig created = new SystemConfig();
            created.setKey(key);
            return created;
        });

        String previous = cfg.getValue();
        cfg.setValue(request.getValue().trim());
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            cfg.setDescription(request.getDescription().trim());
        }
        cfg.setUpdatedBy(adminEmail);
        SystemConfig saved = systemConfigRepository.save(cfg);

        adminAuditService.log(
                adminEmail,
                "UPDATE_SYSTEM_CONFIG",
                null,
                null,
                "{\"key\":\"" + key + "\",\"from\":\"" + previous + "\",\"to\":\"" + saved.getValue() + "\"}",
                ipAddress
        );

        return toResponse(saved);
    }

    private SystemConfigResponse toResponse(SystemConfig cfg) {
        return SystemConfigResponse.builder()
                .key(cfg.getKey())
                .value(cfg.getValue())
                .description(cfg.getDescription())
                .updatedBy(cfg.getUpdatedBy())
                .updatedAt(cfg.getUpdatedAt())
                .build();
    }
}
