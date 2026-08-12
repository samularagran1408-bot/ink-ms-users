package com.inklusport.users.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BulkEmailsRequest {

    @NotEmpty(message = "Debes indicar al menos un email")
    private List<@Size(max = 100) String> emails;
}
