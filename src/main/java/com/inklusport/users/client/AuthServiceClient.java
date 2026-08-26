package com.inklusport.users.client;

import com.inklusport.users.dto.LastLoginResponse;
import com.inklusport.users.dto.LoginAttemptResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ink-ms-auth", url = "${auth.service.url:http://localhost:3001}")
public interface AuthServiceClient {

    @PostMapping("/api/internal/auth/last-logins")
    List<LastLoginResponse> getLastLogins(@RequestBody List<String> emails);

    @GetMapping("/api/internal/auth/last-login")
    LastLoginResponse getLastLogin(@RequestParam("email") String email);

    @GetMapping("/api/internal/auth/login-history")
    List<LoginAttemptResponse> getLoginHistory(@RequestParam("email") String email);
}
