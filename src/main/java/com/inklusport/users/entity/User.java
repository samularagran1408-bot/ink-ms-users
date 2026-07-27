package com.inklusport.users.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id = UUID.randomUUID().toString();

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "profile_picture", columnDefinition = "TEXT")
    private String profilePicture;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "disability", length = 100)
    private String disability;

    /**
     * CAMPOS DE VERIFICACIÓN
     */

    /**
     * Verificación básica
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    // Organizador
    @Column(name = "events_attended")
    private int eventsAttended = 0;

    @Column(name = "events_created")
    private int eventsCreated = 0;

    @Column(name = "platform_days")
    private int platformDays = 0;

    @Column(name = "test_event_created")
    private boolean testEventCreated = false;

    @Column(name = "organizer_quiz_score")
    private Double organizerQuizScore;

    @Column(name = "organizer_quiz_passed")
    private boolean organizerQuizPassed = false;

    @Column(name = "organizer_verification_status")
    @Enumerated(EnumType.STRING)
    private VerificationStatus organizerVerificationStatus = VerificationStatus.pending;

    /**
     * Entrenador
     */
    @Column(name = "certification_file")
    private String certificationFile;

    @Column(name = "experience_months")
    private int experienceMonths = 0;

    @Column(name = "events_as_trainer")
    private int eventsAsTrainer = 0;

    @Column(name = "trainer_quiz_score")
    private Double trainerQuizScore;

    @Column(name = "trainer_quiz_passed")
    private boolean trainerQuizPassed = false;

    @Column(name = "identity_document")
    private String identityDocument;

    @Column(name = "trainer_verification_status")
    @Enumerated(EnumType.STRING)
    private VerificationStatus trainerVerificationStatus = VerificationStatus.pending;

    @Column(name = "verified_roles")
    private String verifiedRoles = "";

    /**
     * Campos de auditoría
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Relaciones
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRole> roles = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserActivity> activities = new ArrayList<>();

    /**
     * ENUM INTERNO
     */
    public enum VerificationStatus {
        pending, approved, rejected
    }
}