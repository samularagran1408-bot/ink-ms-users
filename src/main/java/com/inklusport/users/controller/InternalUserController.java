package com.inklusport.users.controller;

import com.inklusport.users.dto.CreateProfileFromRegisterRequest;
import com.inklusport.users.dto.UserAccessStatusResponse;
import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.service.RoleService;
import com.inklusport.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints internos para consumo entre microservicios.
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final RoleService roleService;
    private final UserService userService;

    @GetMapping("/roles-by-email")
    public List<String> getUserRoles(@RequestParam String email) {
        return roleService.getUserRoles(email);
    }

    /**
     * RF28: estado de acceso efectivo (bloqueos temporales/permanentes).
     */
    @GetMapping("/access-status")
    public UserAccessStatusResponse getAccessStatus(@RequestParam String email) {
        return userService.getAccessStatus(email);
    }

    @PostMapping("/profile-from-register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse createProfileFromRegister(
            @Valid @RequestBody CreateProfileFromRegisterRequest request) {
        return userService.createProfileFromRegister(request);
    }

    @GetMapping("/{id}")
    public UserProfileResponse getUserById(@PathVariable String id) {
        return userService.getUserProfileById(id);
    }
}
