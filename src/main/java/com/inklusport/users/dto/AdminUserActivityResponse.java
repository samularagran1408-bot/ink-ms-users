package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminUserActivityResponse {
    private LocalDateTime lastLoginAt;
    private List<AdminUserActivityItem> items;
}
