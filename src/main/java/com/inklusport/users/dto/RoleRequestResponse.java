package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoleRequestResponse {
    private String id;
    private String userEmail;
    private String userFullName;
    private String requestedRole;
    private String status;
    private LocalDateTime requestedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
}
