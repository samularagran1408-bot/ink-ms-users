package com.inklusport.users.repository;

import com.inklusport.users.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, String> {

    List<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AdminAuditLog> findByTargetEmailOrderByCreatedAtDesc(String targetEmail);

    List<AdminAuditLog> findByAdminEmailOrderByCreatedAtDesc(String adminEmail);
}
