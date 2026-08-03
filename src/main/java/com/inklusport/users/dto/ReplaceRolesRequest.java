package com.inklusport.users.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * RF27: reemplaza el conjunto de roles del usuario (admin, usuario, organizador, etc.).
 */
@Data
public class ReplaceRolesRequest {

    /** Nombres de rol (p. ej. ADMIN, USUARIO, ORGANIZADOR). */
    @NotEmpty(message = "Debe indicar al menos un rol")
    private List<String> roleNames;
}
