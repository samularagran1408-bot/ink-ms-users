package com.inklusport.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttemptResponse {
    private String email;
    private Boolean successful;
    private String ipAddress;
    private LocalDateTime attemptTime;
}
