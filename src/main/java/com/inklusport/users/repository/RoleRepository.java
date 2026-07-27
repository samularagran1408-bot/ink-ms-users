package com.inklusport.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inklusport.users.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional <Role> findByName(String name);

    boolean existsByName(String name);
}
