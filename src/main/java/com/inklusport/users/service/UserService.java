package com.inklusport.users.service;

import com.inklusport.users.dto.BlockUserRequest;
import com.inklusport.users.dto.CreateProfileFromRegisterRequest;
import com.inklusport.users.dto.UpdateProfileRequest;
import com.inklusport.users.dto.UserAccessStatusResponse;
import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.entity.Role;
import com.inklusport.users.entity.User;
import com.inklusport.users.entity.UserRole;
import com.inklusport.users.entity.UserRoleId;
import com.inklusport.users.repository.RoleRepository;
import com.inklusport.users.repository.UserRepository;
import com.inklusport.users.repository.UserRoleRepository;
import com.inklusport.users.util.DisabilityProfileRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AdminAuditService adminAuditService;

    // CRUD BÁSICO

    @Transactional
    public UserProfileResponse createUserProfile(String email, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("El usuario ya existe");
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        log.info("Perfil de usuario creado: {}", email);

        return convertToResponse(savedUser);
    }

    /**
     * Crea el perfil completo desde el registro de auth (discapacidad, acompañante y preferencia de apoyo)
     * y asigna el rol USUARIO por defecto.
     */
    @Transactional
    public UserProfileResponse createProfileFromRegister(CreateProfileFromRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El usuario ya existe");
        }

        User user = new User();
        user.setEmail(request.getEmail().trim());
        user.setFullName(request.getFullName().trim());
        user.setActive(true);
        String disability = trimToNull(request.getDisability());
        String companionFullName = trimToNull(request.getCompanionFullName());
        String companionPhone = trimToNull(request.getCompanionPhone());
        DisabilityProfileRules.assertCompanionPresent(disability, companionFullName, companionPhone);

        user.setDisability(disability);
        user.setCompanionFullName(companionFullName);
        user.setCompanionPhone(companionPhone);
        user.setCompanionRelationship(trimToNull(request.getCompanionRelationship()));
        user.setCompanionEmail(trimToNull(request.getCompanionEmail()));
        user.setSupportPreference(trimToNull(request.getSupportPreference()));
        user.setSupportPreferenceNotes(trimToNull(request.getSupportPreferenceNotes()));

        User savedUser = userRepository.save(user);
        assignDefaultUsuarioRole(savedUser);

        log.info("Perfil creado desde registro: {} (disability={}, supportPreference={})",
                savedUser.getEmail(), savedUser.getDisability(), savedUser.getSupportPreference());

        return convertToResponse(savedUser);
    }

    private void assignDefaultUsuarioRole(User user) {
        Role role = roleRepository.findByName("USUARIO")
                .orElseThrow(() -> new RuntimeException("Rol USUARIO no encontrado en el catálogo"));

        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            return;
        }

        UserRoleId id = new UserRoleId(user.getId(), role.getId());
        UserRole userRole = new UserRole();
        userRole.setId(id);
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedBy("SYSTEM_REGISTER");
        userRoleRepository.save(userRole);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        return convertToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return convertToResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(trimToNull(request.getPhone()));
        if (request.getProfilePicture() != null) user.setProfilePicture(trimToNull(request.getProfilePicture()));
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getDisability() != null) user.setDisability(trimToNull(request.getDisability()));
        if (request.getCompanionFullName() != null) user.setCompanionFullName(trimToNull(request.getCompanionFullName()));
        if (request.getCompanionPhone() != null) user.setCompanionPhone(trimToNull(request.getCompanionPhone()));
        if (request.getCompanionRelationship() != null) user.setCompanionRelationship(trimToNull(request.getCompanionRelationship()));
        if (request.getCompanionEmail() != null) user.setCompanionEmail(trimToNull(request.getCompanionEmail()));
        if (request.getSupportPreference() != null) user.setSupportPreference(trimToNull(request.getSupportPreference()));
        if (request.getSupportPreferenceNotes() != null) user.setSupportPreferenceNotes(trimToNull(request.getSupportPreferenceNotes()));

        DisabilityProfileRules.assertCompanionPresent(
                user.getDisability(),
                user.getCompanionFullName(),
                user.getCompanionPhone());

        User updatedUser = userRepository.save(user);
        log.info("Perfil actualizado: {}", email);

        return convertToResponse(updatedUser);
    }

    // MÉTODOS PARA VERIFICACIÓN

    @Transactional
    public UserProfileResponse verifyOrganizer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        long days = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
        user.setPlatformDays((int) days);

        boolean meetsRequirements =
                user.getEventsAttended() >= 5 &&
                user.isTestEventCreated() &&
                user.isEmailVerified() &&
                user.isPhoneVerified() &&
                days >= 30 &&
                user.isOrganizerQuizPassed();

        if (meetsRequirements) {
            user.setOrganizerVerificationStatus(User.VerificationStatus.approved);
            if (!user.getVerifiedRoles().contains("ORGANIZADOR")) {
                user.setVerifiedRoles(user.getVerifiedRoles() + ",ORGANIZADOR");
            }
            log.info("Usuario {} verificado como ORGANIZADOR", userId);
        } else {
            user.setOrganizerVerificationStatus(User.VerificationStatus.rejected);
            log.info("Usuario {} NO cumple requisitos para ORGANIZADOR", userId);
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }

    @Transactional
    public UserProfileResponse verifyTrainer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean meetsRequirements =
                user.getCertificationFile() != null &&
                user.getExperienceMonths() >= 6 &&
                user.getEventsAsTrainer() >= 3 &&
                user.isTrainerQuizPassed() &&
                user.getIdentityDocument() != null;

        if (meetsRequirements) {
            user.setTrainerVerificationStatus(User.VerificationStatus.approved);
            if (!user.getVerifiedRoles().contains("ENTRENADOR")) {
                user.setVerifiedRoles(user.getVerifiedRoles() + ",ENTRENADOR");
            }
            log.info("Usuario {} verificado como ENTRENADOR", userId);
        } else {
            user.setTrainerVerificationStatus(User.VerificationStatus.rejected);
            log.info("Usuario {} NO cumple requisitos para ENTRENADOR", userId);
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }

    @Transactional
    public void incrementEventsAttended(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setEventsAttended(user.getEventsAttended() + 1);
        userRepository.save(user);
    }

    @Transactional
    public void incrementEventsCreated(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setEventsCreated(user.getEventsCreated() + 1);
        if (user.getEventsCreated() >= 1) {
            user.setTestEventCreated(true);
        }
        userRepository.save(user);
    }

    @Transactional
    public void saveOrganizerQuizScore(String userId, double score) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setOrganizerQuizScore(score);
        user.setOrganizerQuizPassed(score >= 70.0);
        userRepository.save(user);
    }

    @Transactional
    public void saveTrainerQuizScore(String userId, double score) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setTrainerQuizScore(score);
        user.setTrainerQuizPassed(score >= 75.0);
        userRepository.save(user);
    }

    // MÉTODOS DE ACTIVACIÓN / BLOQUEO (RF28)

    @Transactional
    public UserProfileResponse deactivateUser(String email, BlockUserRequest request,
                                              String adminEmail, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

        boolean permanent = request == null || request.isPermanent() || request.getBlockedUntil() == null;
        LocalDateTime until = permanent ? null : request.getBlockedUntil();
        if (!permanent && until.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("blockedUntil debe ser una fecha/hora futura");
        }

        String reason = request != null && request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : (permanent ? "Desactivado permanentemente por administrador" : "Desactivado temporalmente por administrador");

        user.setActive(false);
        user.setBlockedPermanently(permanent);
        user.setBlockedUntil(until);
        user.setBlockReason(reason);
        User saved = userRepository.save(user);

        adminAuditService.log(
                adminEmail,
                permanent ? "BLOCK_USER_PERMANENT" : "BLOCK_USER_TEMPORARY",
                email,
                saved.getId(),
                "{\"reason\":\"" + escapeJson(reason) + "\",\"blockedUntil\":"
                        + (until != null ? "\"" + until + "\"" : "null") + "}",
                ipAddress
        );

        log.info("Usuario desactivado ({}): {}", permanent ? "permanente" : "temporal", email);
        return convertToResponse(saved);
    }

    @Transactional
    public UserProfileResponse activateUser(String email, String adminEmail, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

        user.setActive(true);
        user.setBlockedPermanently(false);
        user.setBlockedUntil(null);
        user.setBlockReason(null);
        User saved = userRepository.save(user);

        adminAuditService.log(adminEmail, "ACTIVATE_USER", email, saved.getId(), "{}", ipAddress);
        log.info("Usuario activado: {}", email);
        return convertToResponse(saved);
    }

    /**
     * RF28 / auth: estado de acceso efectivo (reactiva bloqueos temporales vencidos).
     */
    @Transactional
    public UserAccessStatusResponse getAccessStatus(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

        clearExpiredTemporaryBlock(user);

        boolean allowed = user.isActive();
        String message = allowed
                ? "Acceso permitido"
                : (user.isBlockedPermanently()
                ? "Usuario bloqueado permanentemente"
                : "Usuario bloqueado temporalmente");

        if (!allowed && user.getBlockReason() != null) {
            message = message + ": " + user.getBlockReason();
        }

        return UserAccessStatusResponse.builder()
                .email(user.getEmail())
                .allowed(allowed)
                .active(user.isActive())
                .permanentlyBlocked(user.isBlockedPermanently())
                .blockedUntil(user.getBlockedUntil())
                .blockReason(user.getBlockReason())
                .message(message)
                .build();
    }

    private void clearExpiredTemporaryBlock(User user) {
        if (user.isActive()) {
            return;
        }
        if (user.isBlockedPermanently()) {
            return;
        }
        if (user.getBlockedUntil() != null && user.getBlockedUntil().isBefore(LocalDateTime.now())) {
            user.setActive(true);
            user.setBlockedUntil(null);
            user.setBlockReason(null);
            user.setBlockedPermanently(false);
            userRepository.save(user);
            adminAuditService.log("SYSTEM", "AUTO_UNBLOCK_EXPIRED", user.getEmail(), user.getId(),
                    "{}", null);
            log.info("Bloqueo temporal expirado; usuario reactivado: {}", user.getEmail());
        }
    }

    @Transactional
    public UserProfileResponse adminUpdateUser(String email, UpdateProfileRequest request,
                                               String adminEmail, String ipAddress) {
        UserProfileResponse updated = updateUserProfile(email, request);
        User user = userRepository.findByEmail(email).orElseThrow();
        adminAuditService.log(adminEmail, "UPDATE_USER_PROFILE", email, user.getId(),
                "{\"fields\":\"profile\"}", ipAddress);
        return updated;
    }

    // MÉTODOS DE LISTADO

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getActiveUsers() {
        return userRepository.findByIsActiveTrue().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getInactiveUsers() {
        return userRepository.findByIsActiveFalse().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // MAPEO A DTO

    private UserProfileResponse convertToResponse(User user) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .disability(user.getDisability())
                .companionFullName(user.getCompanionFullName())
                .companionPhone(user.getCompanionPhone())
                .companionRelationship(user.getCompanionRelationship())
                .companionEmail(user.getCompanionEmail())
                .supportPreference(user.getSupportPreference())
                .supportPreferenceNotes(user.getSupportPreferenceNotes())
                .isActive(user.isActive())
                .blockReason(user.getBlockReason())
                .blockedUntil(user.getBlockedUntil())
                .blockedPermanently(user.isBlockedPermanently())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roles)
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .eventsAttended(user.getEventsAttended())
                .eventsCreated(user.getEventsCreated())
                .platformDays(user.getPlatformDays())
                .testEventCreated(user.isTestEventCreated())
                .organizerQuizScore(user.getOrganizerQuizScore())
                .organizerQuizPassed(user.isOrganizerQuizPassed())
                .organizerVerificationStatus(
                        user.getOrganizerVerificationStatus() != null ?
                        user.getOrganizerVerificationStatus().name() : "pending"
                )
                .certificationFile(user.getCertificationFile())
                .experienceMonths(user.getExperienceMonths())
                .eventsAsTrainer(user.getEventsAsTrainer())
                .trainerQuizScore(user.getTrainerQuizScore())
                .trainerQuizPassed(user.isTrainerQuizPassed())
                .identityDocument(user.getIdentityDocument())
                .trainerVerificationStatus(
                        user.getTrainerVerificationStatus() != null ?
                        user.getTrainerVerificationStatus().name() : "pending"
                )
                .verifiedRoles(user.getVerifiedRoles())
                .build();
    }
}