package com.inklusport.users.service;

import com.inklusport.users.client.SubscriptionsServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Asigna el plan gratuito inicial cuando el usuario obtiene el rol ORGANIZADOR.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizerPlanAssignmentService {

    private final SubscriptionsServiceClient subscriptionsServiceClient;

    public void assignFreePlanIfOrganizer(String userId, String roleName) {
        if (userId == null || roleName == null || !"ORGANIZADOR".equalsIgnoreCase(roleName.trim())) {
            return;
        }
        try {
            subscriptionsServiceClient.asignarPlanGratuito(userId);
            log.info("Plan gratuito inicial solicitado para organizador {}", userId);
        } catch (Exception e) {
            log.warn("No se pudo asignar el plan gratuito a {}: {}", userId, e.getMessage());
        }
    }
}
