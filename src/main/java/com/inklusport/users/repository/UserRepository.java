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

    @Query("SELECT u FROM User u WHERE (u.deleted IS NULL OR u.deleted = false)")
    List<User> findAllVisible();

    @Query("SELECT u FROM User u WHERE u.isActive = true AND (u.deleted IS NULL OR u.deleted = false)")
    List<User> findVisibleActive();

    @Query("SELECT u FROM User u WHERE u.isActive = false AND (u.deleted IS NULL OR u.deleted = false)")
    List<User> findVisibleInactive();

    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted IS NULL OR u.deleted = false")
    long countVisible();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true AND (u.deleted IS NULL OR u.deleted = false)")
    long countVisibleActive();

    @Query("""
            SELECT u FROM User u
            WHERE (u.deleted IS NULL OR u.deleted = false)
              AND (:name IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:disability IS NULL OR LOWER(u.disability) LIKE LOWER(CONCAT('%', :disability, '%')))
            ORDER BY u.fullName ASC
            """)
    List<User> searchVisible(@Param("name") String name, @Param("disability") String disability);

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
