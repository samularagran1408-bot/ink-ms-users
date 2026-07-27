package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserActivityResponse {
    private String id;
    private String action;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
