package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminAuditLogResponse {
    private String id;
    private String adminEmail;
    private String action;
    private String targetEmail;
    private String targetUserId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
