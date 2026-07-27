package com.inklusport.users.repository;

import com.inklusport.users.entity.UserRole;
import com.inklusport.users.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(String userId);

    @Query("SELECT ur.role.id FROM UserRole ur WHERE ur.user.id = :userId")
    List<Long> findRoleIdsByUserId(@Param("userId") String userId);

    @Query("SELECT ur.role.name FROM UserRole ur WHERE ur.user.id = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndRoleId(String userId, Long roleId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserRole ur WHERE ur.user.id = :userId")
    void deleteByUserId(@Param("userId") String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.id = :roleId")
    void deleteByUserIdAndRoleId(@Param("userId") String userId, @Param("roleId") Long roleId);
}