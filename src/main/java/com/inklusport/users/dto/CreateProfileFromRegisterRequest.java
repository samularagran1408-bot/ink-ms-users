package com.inklusport.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Alta de perfil disparada desde ink-ms-auth al completar el registro.
 */
@Data
public class CreateProfileFromRegisterRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String fullName;

    @Size(max = 100, message = "El tipo de discapacidad no puede superar 100 caracteres")
    private String disability;

    @Size(max = 150)
    private String companionFullName;

    @Size(max = 20)
    private String companionPhone;

    @Size(max = 80)
    private String companionRelationship;

    @Email(message = "El email del acompañante debe ser válido")
    @Size(max = 100)
    private String companionEmail;

    @Size(max = 50)
    private String supportPreference;

    @Size(max = 255)
    private String supportPreferenceNotes;
}
