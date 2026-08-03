package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserAccessStatusResponse {
    private String email;
    private boolean allowed;
    private boolean active;
    private boolean permanentlyBlocked;
    private LocalDateTime blockedUntil;
    private String blockReason;
    private String message;
}
