package com.inklusport.users.repository;

import com.inklusport.users.entity.RoleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRequestRepository extends JpaRepository<RoleRequest, String> {

    Page<RoleRequest> findByStatusOrderByRequestedAtDesc(RoleRequest.Status status, Pageable pageable);

    Optional<RoleRequest> findFirstByUserIdAndStatusOrderByRequestedAtDesc(String userId, RoleRequest.Status status);

    boolean existsByUserIdAndRequestedRoleAndStatus(String userId, String requestedRole, RoleRequest.Status status);

    boolean existsByUserIdAndStatus(String userId, RoleRequest.Status status);
}
