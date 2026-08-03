package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserProfileResponse {
    
    /**
     * Datos Base
     */
    private String id;
    private String email;
    private String fullName;
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
    private Boolean isActive;
    private String blockReason;
    private LocalDateTime blockedUntil;
    private Boolean blockedPermanently;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> roles;

    /**
     * Verificación Básica
     */
    private Boolean emailVerified;
    private Boolean phoneVerified;

    /**
     * Organizador
     */
    private Integer eventsAttended;
    private Integer eventsCreated;
    private Integer platformDays;
    private Boolean testEventCreated;
    private Double organizerQuizScore;
    private Boolean organizerQuizPassed;
    private String organizerVerificationStatus;

    /**
     * Entrenador
     */
    private String certificationFile;
    private Integer experienceMonths;
    private Integer eventsAsTrainer;
    private Double trainerQuizScore;
    private Boolean trainerQuizPassed;
    private String identityDocument;
    private String trainerVerificationStatus;

    /**
     * Roles verificados
     */
    private String verifiedRoles;
}