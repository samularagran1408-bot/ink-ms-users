package com.inklusport.users.dto;

import lombok.Data;

/**
 * RF27: asignar rol por ID o por nombre (ADMIN, USUARIO, ORGANIZADOR, ENTRENADOR).
 * Debe enviarse al menos uno de los dos campos.
 */
@Data
public class AssignRoleRequest {

    private Long roleId;

    /** Nombre del rol (alternativa a roleId). */
    private String roleName;
}
