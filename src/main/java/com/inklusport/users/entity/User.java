package com.inklusport.users.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
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

    /**
     * Wrapper (no primitivo): en BD antiguas la columna puede ser NULL y Hibernate
     * no puede asignar null a boolean. Los accessors is/set tratan null como default.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /** motivo del bloqueo/desactivación. */
    @Column(name = "block_reason", length = 500)
    private String blockReason;

    /** fin del bloqueo temporal; null + isActive=false + blockedPermanently = permanente. */
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "blocked_permanently")
    private Boolean blockedPermanently = false;

    /** Eliminación lógica: el perfil deja de listarse pero se conserva. */
    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "profile_picture", columnDefinition = "TEXT")
    private String profilePicture;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "disability", length = 100)
    private String disability;

    @Column(name = "companion_full_name", length = 150)
    private String companionFullName;

    @Column(name = "companion_phone", length = 20)
    private String companionPhone;

    @Column(name = "companion_relationship", length = 80)
    private String companionRelationship;

    @Column(name = "companion_email", length = 100)
    private String companionEmail;

    @Column(name = "support_preference", length = 50)
    private String supportPreference;

    @Column(name = "support_preference_notes", length = 255)
    private String supportPreferenceNotes;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    @Column(name = "events_attended")
    private Integer eventsAttended = 0;

    @Column(name = "events_created")
    private Integer eventsCreated = 0;

    @Column(name = "platform_days")
    private Integer platformDays = 0;

    @Column(name = "test_event_created")
    private Boolean testEventCreated = false;

    @Column(name = "organizer_quiz_score")
    private Double organizerQuizScore;

    @Column(name = "organizer_quiz_passed")
    private Boolean organizerQuizPassed = false;

    @Column(name = "organizer_verification_status")
    @Enumerated(EnumType.STRING)
    private VerificationStatus organizerVerificationStatus = VerificationStatus.pending;

    @Column(name = "certification_file")
    private String certificationFile;

    @Column(name = "experience_months")
    private Integer experienceMonths = 0;

    /**
     * Sport IDs del catálogo, separados por coma (disciplinas del quiz).
     */
    @Column(name = "quiz_disciplines", length = 500)
    private String quizDisciplines;

    @Column(name = "events_as_trainer")
    private Integer eventsAsTrainer = 0;

    @Column(name = "trainer_quiz_score")
    private Double trainerQuizScore;

    @Column(name = "trainer_quiz_passed")
    private Boolean trainerQuizPassed = false;

    /**
     * Intentos fallidos del quiz de entrenador (máximo configurado en UserService).
     */
    @Column(name = "trainer_quiz_attempts")
    private Integer trainerQuizAttempts = 0;

    /**
     * Intentos fallidos del quiz de organizador (máximo configurado en UserService).
     */
    @Column(name = "organizer_quiz_attempts")
    private Integer organizerQuizAttempts = 0;

    @Column(name = "identity_document")
    private String identityDocument;

    @Column(name = "trainer_verification_status")
    @Enumerated(EnumType.STRING)
    private VerificationStatus trainerVerificationStatus = VerificationStatus.pending;

    @Column(name = "verified_roles")
    private String verifiedRoles = "";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRole> roles = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserActivity> activities = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void normalizeNullFlags() {
        if (isActive == null) {
            isActive = true;
        }
        if (blockedPermanently == null) {
            blockedPermanently = false;
        }
        if (deleted == null) {
            deleted = false;
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
        if (phoneVerified == null) {
            phoneVerified = false;
        }
        if (testEventCreated == null) {
            testEventCreated = false;
        }
        if (organizerQuizPassed == null) {
            organizerQuizPassed = false;
        }
        if (trainerQuizPassed == null) {
            trainerQuizPassed = false;
        }
        if (eventsAttended == null) {
            eventsAttended = 0;
        }
        if (eventsCreated == null) {
            eventsCreated = 0;
        }
        if (platformDays == null) {
            platformDays = 0;
        }
        if (experienceMonths == null) {
            experienceMonths = 0;
        }
        if (eventsAsTrainer == null) {
            eventsAsTrainer = 0;
        }
        if (trainerQuizAttempts == null) {
            trainerQuizAttempts = 0;
        }
        if (organizerQuizAttempts == null) {
            organizerQuizAttempts = 0;
        }
    }

    @PostLoad
    private void coerceNullFlagsAfterLoad() {
        normalizeNullFlags();
    }

    /** Compatibilidad: el código llama isActive()/setActive() como si fuera primitivo. */
    public boolean isActive() {
        return !Boolean.FALSE.equals(isActive);
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isBlockedPermanently() {
        return Boolean.TRUE.equals(blockedPermanently);
    }

    public void setBlockedPermanently(boolean blockedPermanently) {
        this.blockedPermanently = blockedPermanently;
    }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isPhoneVerified() {
        return Boolean.TRUE.equals(phoneVerified);
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public boolean isTestEventCreated() {
        return Boolean.TRUE.equals(testEventCreated);
    }

    public void setTestEventCreated(boolean testEventCreated) {
        this.testEventCreated = testEventCreated;
    }

    public boolean isOrganizerQuizPassed() {
        return Boolean.TRUE.equals(organizerQuizPassed);
    }

    public void setOrganizerQuizPassed(boolean organizerQuizPassed) {
        this.organizerQuizPassed = organizerQuizPassed;
    }

    public boolean isTrainerQuizPassed() {
        return Boolean.TRUE.equals(trainerQuizPassed);
    }

    public void setTrainerQuizPassed(boolean trainerQuizPassed) {
        this.trainerQuizPassed = trainerQuizPassed;
    }

    public int getEventsAttended() {
        return eventsAttended == null ? 0 : eventsAttended;
    }

    public void setEventsAttended(int eventsAttended) {
        this.eventsAttended = eventsAttended;
    }

    public int getEventsCreated() {
        return eventsCreated == null ? 0 : eventsCreated;
    }

    public void setEventsCreated(int eventsCreated) {
        this.eventsCreated = eventsCreated;
    }

    public int getPlatformDays() {
        return platformDays == null ? 0 : platformDays;
    }

    public void setPlatformDays(int platformDays) {
        this.platformDays = platformDays;
    }

    public int getExperienceMonths() {
        return experienceMonths == null ? 0 : experienceMonths;
    }

    public void setExperienceMonths(int experienceMonths) {
        this.experienceMonths = experienceMonths;
    }

    public int getEventsAsTrainer() {
        return eventsAsTrainer == null ? 0 : eventsAsTrainer;
    }

    public void setEventsAsTrainer(int eventsAsTrainer) {
        this.eventsAsTrainer = eventsAsTrainer;
    }

    public int getTrainerQuizAttempts() {
        return trainerQuizAttempts == null ? 0 : trainerQuizAttempts;
    }

    public void setTrainerQuizAttempts(int trainerQuizAttempts) {
        this.trainerQuizAttempts = trainerQuizAttempts;
    }

    public int getOrganizerQuizAttempts() {
        return organizerQuizAttempts == null ? 0 : organizerQuizAttempts;
    }

    public void setOrganizerQuizAttempts(int organizerQuizAttempts) {
        this.organizerQuizAttempts = organizerQuizAttempts;
    }

    public enum VerificationStatus {
        pending, approved, rejected
    }
}
