package com.inklusport.users.service;

import com.inklusport.users.dto.AssignRoleRequest;
import com.inklusport.users.dto.AssignRoleResponse;
import com.inklusport.users.entity.Role;
import com.inklusport.users.entity.User;
import com.inklusport.users.repository.RoleRepository;
import com.inklusport.users.repository.UserRepository;
import com.inklusport.users.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private RoleService roleService;

    @Test
    void getUserRoles_devuelveVacioSiNoExiste() {
        when(userRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        assertTrue(roleService.getUserRoles("nadie@test.com").isEmpty());
    }

    @Test
    void assignRoleToUser_rechazaRolDuplicado() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("ana@inklusport.test");
        Role role = new Role();
        role.setId(2L);
        role.setName("ENTRENADOR");

        AssignRoleRequest request = new AssignRoleRequest();
        request.setRoleName("entrenador");

        when(userRepository.findByEmail("ana@inklusport.test")).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ENTRENADOR")).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserIdAndRoleId("user-1", 2L)).thenReturn(true);

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> roleService.assignRoleToUser("ana@inklusport.test", request, "admin@test.com", "127.0.0.1")
        );
        assertTrue(error.getMessage().contains("ya tiene el rol"));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignRoleToUser_asignaYAudita() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("ana@inklusport.test");
        Role role = new Role();
        role.setId(2L);
        role.setName("ENTRENADOR");

        AssignRoleRequest request = new AssignRoleRequest();
        request.setRoleId(2L);

        when(userRepository.findByEmail("ana@inklusport.test")).thenReturn(Optional.of(user));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserIdAndRoleId("user-1", 2L)).thenReturn(false);

        AssignRoleResponse response = roleService.assignRoleToUser(
                "ana@inklusport.test", request, "admin@test.com", "127.0.0.1");

        assertEquals("ENTRENADOR", response.getRoleName());
        verify(userRoleRepository).save(any());
        verify(adminAuditService).log(
                "admin@test.com", "ASSIGN_ROLE", "ana@inklusport.test", "user-1",
                "{\"role\":\"ENTRENADOR\",\"roleId\":2}", "127.0.0.1");
    }
}
