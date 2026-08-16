package com.inklusport.users.client;

import com.inklusport.users.dto.FutureRegistrationsCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ink-ms-sports", url = "${sports.service.url:http://localhost:3003}")
public interface SportsServiceClient {

    @GetMapping("/api/internal/registrations/user/{userId}/future")
    FutureRegistrationsCheckResponse getFutureRegistrations(@PathVariable("userId") String userId);
}
