package com.inklusport.users.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Solicitud de rol elevado (ENTRENADOR u ORGANIZADOR) al registrarse por primera vez.
 */
@Entity
@Table(name = "role_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "user_email", nullable = false, length = 100)
    private String userEmail;

    @Column(name = "user_full_name", nullable = false, length = 150)
    private String userFullName;

    @Column(name = "requested_role", nullable = false, length = 50)
    private String requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}
