package com.inklusport.users.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String fullName;

    @Size(min = 7, max = 20, message = "El teléfono debe tener entre 7 y 20 caracteres")
    private String phone;

    private String profilePicture;

    private String bio;

    private String disability;

    private String companionFullName;

    private String companionPhone;

    private String companionRelationship;

    private String companionEmail;

    private String supportPreference;

    private String supportPreferenceNotes;
}
