package com.inklusport.users.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "ink-ms-subscriptions", url = "${subscriptions.service.url:http://localhost:3005}")
public interface SubscriptionsServiceClient {

    @PostMapping("/api/internal/suscripciones/organizadores/{organizadorId}/plan-gratuito")
    void asignarPlanGratuito(@PathVariable("organizadorId") String organizadorId);
}
