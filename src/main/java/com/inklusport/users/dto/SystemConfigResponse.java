package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemConfigResponse {
    private String key;
    private String value;
    private String description;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
