package com.inklusport.users.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id = UUID.randomUUID().toString();

    @Column(name = "admin_email", nullable = false, length = 100)
    private String adminEmail;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_email", length = 100)
    private String targetEmail;

    @Column(name = "target_user_id", columnDefinition = "CHAR(36)")
    private String targetUserId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
