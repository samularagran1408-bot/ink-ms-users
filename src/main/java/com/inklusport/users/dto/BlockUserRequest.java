package com.inklusport.users.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RF28: bloqueo temporal (blockedUntil) o permanente (permanent=true / sin fecha).
 */
@Data
public class BlockUserRequest {

    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    private String reason;

    /** Si es true, el bloqueo no expira automáticamente. */
    private boolean permanent = true;

    /** Fecha/hora de fin del bloqueo temporal. Ignorado si permanent=true. */
    private LocalDateTime blockedUntil;
}
