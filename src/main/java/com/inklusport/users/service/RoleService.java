package com.inklusport.users.service;

import com.inklusport.users.dto.AssignRoleRequest;
import com.inklusport.users.dto.AssignRoleResponse;
import com.inklusport.users.dto.ReplaceRolesRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RF27: administración de roles (ADMIN, USUARIO, ORGANIZADOR, ENTRENADOR).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssignRoleResponse assignRoleToUser(String userEmail, AssignRoleRequest request,
                                               String assignedByAdminEmail, String ipAddress) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado. Debe existir un perfil creado para: " + userEmail));

        Role role = resolveRole(request);

        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            throw new RuntimeException("El usuario ya tiene el rol " + role.getName());
        }

        UserRoleId id = new UserRoleId(user.getId(), role.getId());
        UserRole userRole = new UserRole();
        userRole.setId(id);
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedBy(assignedByAdminEmail);

        userRoleRepository.save(userRole);

        adminAuditService.log(assignedByAdminEmail, "ASSIGN_ROLE", userEmail, user.getId(),
                "{\"role\":\"" + role.getName() + "\",\"roleId\":" + role.getId() + "}", ipAddress);

        log.info("Rol {} asignado a {} por admin {}", role.getName(), userEmail, assignedByAdminEmail);

        return AssignRoleResponse.builder()
                .email(userEmail)
                .roleId(role.getId())
                .roleName(role.getName())
                .message("Rol asignado correctamente")
                .build();
    }

    /**
     * reemplaza todos los roles del usuario por el conjunto indicado.
     */
    @Transactional
    public List<String> replaceUserRoles(String userEmail, ReplaceRolesRequest request,
                                         String adminEmail, String ipAddress) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userEmail));

        List<String> previous = userRoleRepository.findRoleNamesByUserId(user.getId());
        userRoleRepository.deleteByUserId(user.getId());
        userRoleRepository.flush();

        List<String> assigned = new ArrayList<>();
        for (String roleName : request.getRoleNames()) {
            if (roleName == null || roleName.isBlank()) {
                continue;
            }
            Role role = roleRepository.findByName(roleName.trim().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName));

            UserRoleId id = new UserRoleId(user.getId(), role.getId());
            UserRole userRole = new UserRole();
            userRole.setId(id);
            userRole.setUser(user);
            userRole.setRole(role);
            userRole.setAssignedBy(adminEmail);
            userRoleRepository.save(userRole);
            assigned.add(role.getName());
        }

        if (assigned.isEmpty()) {
            throw new RuntimeException("Debe indicar al menos un rol válido");
        }

        adminAuditService.log(adminEmail, "REPLACE_ROLES", userEmail, user.getId(),
                "{\"from\":" + toJsonArray(previous) + ",\"to\":" + toJsonArray(assigned) + "}",
                ipAddress);

        log.info("Roles de {} reemplazados por {} (admin {})", userEmail, assigned, adminEmail);
        return assigned;
    }

    @Transactional
    public void removeRoleFromUser(String userEmail, Long roleId, String adminEmail, String ipAddress) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), roleId)) {
            throw new RuntimeException("El usuario no tiene este rol asignado");
        }

        Role role = roleRepository.findById(roleId).orElse(null);
        userRoleRepository.deleteByUserIdAndRoleId(user.getId(), roleId);

        adminAuditService.log(adminEmail, "REMOVE_ROLE", userEmail, user.getId(),
                "{\"roleId\":" + roleId + ",\"role\":\""
                        + (role != null ? role.getName() : "unknown") + "\"}",
                ipAddress);

        log.info("Rol {} removido de usuario {}", roleId, userEmail);
    }

    @Transactional(readOnly = true)
    public List<String> getUserRoles(String email) {
        return userRepository.findByEmail(email)
                .map(user -> userRoleRepository.findRoleNamesByUserId(user.getId()))
                .orElse(List.of());
    }

    private Role resolveRole(AssignRoleRequest request) {
        if (request.getRoleId() != null) {
            return roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException(
                            "Rol no encontrado. Consulte los IDs en GET /api/admin/users/roles"));
        }
        if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            String name = request.getRoleName().trim().toUpperCase();
            return roleRepository.findByName(name)
                    .orElseThrow(() -> new RuntimeException(
                            "Rol no encontrado: " + name + ". Roles válidos: ADMIN, USUARIO, ORGANIZADOR, ENTRENADOR"));
        }
        throw new RuntimeException("Debe indicar roleId o roleName");
    }

    private static String toJsonArray(List<String> values) {
        return values.stream()
                .map(v -> "\"" + v + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private RoleResponse convertToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}
