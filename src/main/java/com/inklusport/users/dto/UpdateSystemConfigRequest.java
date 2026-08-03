package com.inklusport.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSystemConfigRequest {

    @NotBlank(message = "El valor de configuración es obligatorio")
    private String value;

    private String description;
}
