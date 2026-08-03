package com.inklusport.users.repository;

import com.inklusport.users.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, String> {

    List<AdminAuditLog> findAllByOrderByCreatedAtDesc();

    List<AdminAuditLog> findByTargetEmailOrderByCreatedAtDesc(String targetEmail);

    List<AdminAuditLog> findByAdminEmailOrderByCreatedAtDesc(String adminEmail);
}
