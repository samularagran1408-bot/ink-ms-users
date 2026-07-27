package com.inklusport.users.repository;

import com.inklusport.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByIsActiveTrue();

    List<User> findByIsActiveFalse();

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isActive = true WHERE u.email = :email")
    void activateUser(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isActive = false WHERE u.email = :email")
    void deactivateUser(@Param("email") String email);

    long countByIsActiveTrue();
}
