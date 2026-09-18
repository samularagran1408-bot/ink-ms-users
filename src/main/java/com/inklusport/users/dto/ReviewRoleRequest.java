package com.inklusport.users.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRoleRequest {

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    private String notes;
}
