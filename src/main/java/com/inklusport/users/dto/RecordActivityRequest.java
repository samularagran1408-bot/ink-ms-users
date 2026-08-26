package com.inklusport.users.dto;

import lombok.Data;

@Data
public class RecordActivityRequest {
    private String email;
    private String action;
    private String details;
    private String ipAddress;
}
