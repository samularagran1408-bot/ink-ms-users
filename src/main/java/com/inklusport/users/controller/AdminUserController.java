package com.inklusport.users.controller;

import com.inklusport.users.dto.*;
import com.inklusport.users.repository.UserRepository;
import com.inklusport.users.service.AdminAuditService;
import com.inklusport.users.service.RoleService;
import com.inklusport.users.service.SystemConfigService;
import com.inklusport.users.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Panel administrativo (RF26–RF30): usuarios, roles, bloqueos, auditoría y configuración.
 * Requiere rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final AdminAuditService adminAuditService;
    private final SystemConfigService systemConfigService;


    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/active")
    public ResponseEntity<List<UserProfileResponse>> getActiveUsers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<UserProfileResponse>> getInactiveUsers() {
        return ResponseEntity.ok(userService.getInactiveUsers());
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countUsers() {
        return ResponseEntity.ok(userRepository.count());
    }

    @GetMapping("/active/count")
    public ResponseEntity<Long> countActiveUsers() {
        return ResponseEntity.ok(userRepository.countByIsActiveTrue());
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserProfileResponse> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserProfileByEmail(email));
    }

    @PutMapping("/{email}")
    public ResponseEntity<?> updateUser(
            @PathVariable String email,
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal String adminEmail,
            HttpServletRequest httpRequest) {
        String targetEmail = decodeEmail(email);
        try {
            return ResponseEntity.ok(userService.adminUpdateUser(
                    targetEmail, request, adminEmail, clientIp(httpRequest)));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + targetEmail);
        }
    }

    @GetMapping("/{email}/exists")
    public ResponseEntity<Boolean> userExists(@PathVariable String email) {
        return ResponseEntity.ok(userService.userExists(decodeEmail(email)));
    }


    @PostMapping("/{email}/deactivate")
    public ResponseEntity<?> deactivateUser(
            @PathVariable String email,
            @RequestBody(required = false) BlockUserRequest request,
            @AuthenticationPrincipal String adminEmail,
            HttpServletRequest httpRequest) {
        String targetEmail = decodeEmail(email);
        try {
            BlockUserRequest body = request != null ? request : new BlockUserRequest();
            return ResponseEntity.ok(userService.deactivateUser(
                    targetEmail, body, adminEmail, clientIp(httpRequest)));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + targetEmail + "/deactivate");
        }
    }

    @PostMapping("/{email}/block")
    public ResponseEntity<?> blockUser(
            @PathVariable String email,
            @Valid @RequestBody BlockUserRequest request,
            @AuthenticationPrincipal String adminEmail,
            HttpServletRequest httpRequest) {
        String targetEmail = decodeEmail(email);
        try {
            return ResponseEntity.ok(userService.deactivateUser(
                    targetEmail, request, adminEmail, clientIp(httpRequest)));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + targetEmail + "/block");
        }
    }

    @PostMapping("/{email}/activate")
    public ResponseEntity<?> activateUser(
            @PathVariable String email,
            @AuthenticationPrincipal String adminEmail,
            HttpServletRequest httpRequest) {
        String targetEmail = decodeEmail(email);
        try {
            return ResponseEntity.ok(userService.activateUser(
                    targetEmail, adminEmail, clientIp(httpRequest)));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + targetEmail + "/activate");
        }
    }


    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/roles-by-email")
    public ResponseEntity<List<String>> getUserRoles(@RequestParam String email) {
        return ResponseEntity.ok(roleService.getUserRoles(email));
    }

    @PostMapping("/{email}/roles")
    public ResponseEntity<?> assignRole(
            @PathVariable String email,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal String adminEmail,
            HttpServletRequest httpRequest) {
        String targetEmail = decodeEmail(email);
        try {
            AssignRoleResponse response = roleService.assignRoleToUser(
                    targetEmail, request, adminEmail, clientIp(httpRequest));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + targetEmail + "/roles");
        }
    }

    @PutMapping("/{email}/roles")
    public ResponseEntity<?> replaceRoles(
            @PathVariable String email,
            @Valid @RequestBody ReplaceRolesRequest request,
            @AuthenticationPrincipal String adminEmail,
            HttpServletRequest httpRequest) {
        String targetEmail = decodeEmail(email);
        try {
            return ResponseEntity.ok(roleService.replaceUserRoles(
                    targetEmail, request, adminEmail, clientIp(httpRequest)));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + targetEmail + "/roles");
        }
    }

    @DeleteMapping("/{email}/roles/{roleId}")
    public ResponseEntity<?> removeRole(
            @PathVariable String email,
            @PathVariable Long roleId,
            @AuthenticationPrincipal String adminEmail,
            HttpServletRequest httpRequest) {
        String targetEmail = decodeEmail(email);
        try {
            roleService.removeRoleFromUser(targetEmail, roleId, adminEmail, clientIp(httpRequest));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/" + targetEmail + "/roles/" + roleId);
        }
    }

    
    @GetMapping("/audit")
    public ResponseEntity<List<AdminAuditLogResponse>> getAuditLog(
            @RequestParam(required = false) String targetEmail,
            @RequestParam(required = false) String adminEmail) {
        if (targetEmail != null && !targetEmail.isBlank()) {
            return ResponseEntity.ok(adminAuditService.getByTargetEmail(targetEmail));
        }
        if (adminEmail != null && !adminEmail.isBlank()) {
            return ResponseEntity.ok(adminAuditService.getByAdminEmail(adminEmail));
        }
        return ResponseEntity.ok(adminAuditService.getAll());
    }

    
    @GetMapping("/config")
    public ResponseEntity<List<SystemConfigResponse>> getSystemConfig() {
        return ResponseEntity.ok(systemConfigService.getAll());
    }

    @GetMapping("/config/{key}")
    public ResponseEntity<?> getSystemConfigKey(@PathVariable String key) {
        try {
            return ResponseEntity.ok(systemConfigService.getByKey(key));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/config/" + key);
        }
    }

    @PutMapping("/config/{key}")
    public ResponseEntity<?> updateSystemConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request,
            @AuthenticationPrincipal String adminEmail,
            HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(systemConfigService.update(
                    key, request, adminEmail, clientIp(httpRequest)));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/admin/users/config/" + key);
        }
    }


    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception e, String path) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(e.getMessage())
                .path(path)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private static String decodeEmail(String email) {
        return URLDecoder.decode(email, StandardCharsets.UTF_8);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
