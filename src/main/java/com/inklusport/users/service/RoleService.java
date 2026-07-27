package com.inklusport.users.service;

import com.inklusport.users.dto.AssignRoleRequest;
import com.inklusport.users.dto.AssignRoleResponse;
import com.inklusport.users.dto.RoleResponse;
import com.inklusport.users.entity.Role;
import com.inklusport.users.entity.User;
import com.inklusport.users.entity.UserRole;
import com.inklusport.users.entity.UserRoleId;
import com.inklusport.users.repository.RoleRepository;
import com.inklusport.users.repository.UserRepository;
import com.inklusport.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para administración de roles y relaciones usuario-rol.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    /**
     * Inyecta dependencias importadas de los repositorios
     */
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Lista catálogo de roles disponibles.
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Asigna un rol a un usuario existente.
     */
    @Transactional
    public AssignRoleResponse assignRoleToUser(String userEmail, AssignRoleRequest request, String assignedByAdminEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado. Debe existir un perfil creado (POST /api/users/perfil) para: " + userEmail));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado. Consulte los IDs en GET /api/admin/users/roles"));

        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), request.getRoleId())) {
            throw new RuntimeException("El usuario ya tiene el rol " + role.getName());
        }

        UserRoleId id = new UserRoleId(user.getId(), request.getRoleId());
        UserRole userRole = new UserRole();
        userRole.setId(id);
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedBy(assignedByAdminEmail);

        userRoleRepository.save(userRole);
        log.info("Rol {} asignado a {} por admin {}", role.getName(), userEmail, assignedByAdminEmail);

        return AssignRoleResponse.builder()
                .email(userEmail)
                .roleId(role.getId())
                .roleName(role.getName())
                .message("Rol asignado correctamente")
                .build();
    }

    /**
     * Elimina un rol puntual de un usuario.
     */
    @Transactional
    public void removeRoleFromUser(String userEmail, Long roleId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), roleId)) {
            throw new RuntimeException("El usuario no tiene este rol asignado");
        }

        userRoleRepository.deleteByUserIdAndRoleId(user.getId(), roleId);
        log.info("Rol {} removido de usuario {}", roleId, userEmail);
    }

    /**
     * Obtiene nombres de rol por email de usuario.
     */
    @Transactional(readOnly = true)
    public List<String> getUserRoles(String email) {
        return userRepository.findByEmail(email)
                .map(user -> userRoleRepository.findRoleNamesByUserId(user.getId()))
                .orElse(List.of());
    }

    /**
     * Mapea entidad Role a DTO de salida.
     */
    private RoleResponse convertToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}