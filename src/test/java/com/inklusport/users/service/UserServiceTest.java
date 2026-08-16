package com.inklusport.users.service;

import com.inklusport.users.client.SportsServiceClient;
import com.inklusport.users.dto.UpdateProfileRequest;
import com.inklusport.users.dto.UserProfileResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @Mock
    private AdminNotificationService adminNotificationService;

    @Mock
    private SportsServiceClient sportsServiceClient;

    @InjectMocks
    private UserService userService;

    @Test
    void updateUserProfile_debeGuardarYResponderDiscapacidad() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@test.com");
        user.setFullName("Usuario Test");
        user.setActive(true);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRoleRepository.findRoleNamesByUserId("user-1")).thenReturn(List.of("USUARIO"));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisability("MOTORA");

        UserProfileResponse response = userService.updateUserProfile("test@test.com", request);

        assertEquals("MOTORA", response.getDisability());
        assertEquals("MOTORA", user.getDisability());
    }
}
