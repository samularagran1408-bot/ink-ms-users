package com.inklusport.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificationCodeRequest {
    @NotBlank
    private String code;
}
