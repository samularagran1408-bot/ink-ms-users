package com.inklusport.users.service;

import com.inklusport.users.dto.CreateProfileFromRegisterRequest;
import com.inklusport.users.dto.UpdateProfileRequest;
import com.inklusport.users.dto.UserProfileResponse;
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
        user.setDisability(trimToNull(request.getDisability()));
        user.setCompanionFullName(trimToNull(request.getCompanionFullName()));
        user.setCompanionPhone(trimToNull(request.getCompanionPhone()));
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
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getProfilePicture() != null) user.setProfilePicture(request.getProfilePicture());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getDisability() != null) user.setDisability(request.getDisability());
        if (request.getCompanionFullName() != null) user.setCompanionFullName(request.getCompanionFullName());
        if (request.getCompanionPhone() != null) user.setCompanionPhone(request.getCompanionPhone());
        if (request.getCompanionRelationship() != null) user.setCompanionRelationship(request.getCompanionRelationship());
        if (request.getCompanionEmail() != null) user.setCompanionEmail(request.getCompanionEmail());
        if (request.getSupportPreference() != null) user.setSupportPreference(request.getSupportPreference());
        if (request.getSupportPreferenceNotes() != null) user.setSupportPreferenceNotes(request.getSupportPreferenceNotes());

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

    // MÉTODOS DE ACTIVACIÓN/DESACTIVACIÓN

    @Transactional
    public void deactivateUser(String email) {
        userRepository.deactivateUser(email);
        log.info("Usuario desactivado: {}", email);
    }

    @Transactional
    public void activateUser(String email) {
        userRepository.activateUser(email);
        log.info("Usuario activado: {}", email);
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
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
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