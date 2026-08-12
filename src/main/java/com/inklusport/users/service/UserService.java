package com.inklusport.users.service;

import com.inklusport.users.dto.BlockUserRequest;
import com.inklusport.users.dto.BulkActionResponse;
import com.inklusport.users.dto.CreateProfileFromRegisterRequest;
import com.inklusport.users.dto.QuizPrepRequest;
import com.inklusport.users.dto.QuizPrepResponse;
import com.inklusport.users.dto.UpdateProfileRequest;
import com.inklusport.users.dto.UserAccessStatusResponse;
import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.entity.Role;
import com.inklusport.users.entity.User;
import com.inklusport.users.entity.UserRole;
import com.inklusport.users.entity.UserRoleId;
import com.inklusport.users.exception.SilentAccessDeniedException;
import com.inklusport.users.repository.RoleRepository;
import com.inklusport.users.repository.UserRepository;
import com.inklusport.users.repository.UserRoleRepository;
import com.inklusport.users.util.DisabilityProfileRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final AdminNotificationService adminNotificationService;


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

        try {
            adminNotificationService.notifyAdminsNewUserRegistered(savedUser.getEmail(), savedUser.getFullName());
        } catch (Exception e) {
            log.warn("No se pudo notificar a admins del nuevo registro {}: {}", savedUser.getEmail(), e.getMessage());
        }

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


    /**
     * Años mínimos de experiencia declarados antes del quiz.
     */
    public static final int MIN_QUIZ_EXPERIENCE_YEARS = 3;

    /**
     * Reintentos fallidos permitidos por rol.
     */
    public static final int MAX_QUIZ_ATTEMPTS = 3;

    /**
     * Verifica ORGANIZADOR: el quiz aprobado es el único requisito operativo.
     */
    @Transactional
    public UserProfileResponse verifyOrganizer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.isOrganizerQuizPassed()) {
            user.setOrganizerVerificationStatus(User.VerificationStatus.approved);
            appendVerifiedRole(user, "ORGANIZADOR");
            log.info("Usuario {} verificado como ORGANIZADOR (quiz)", userId);
        } else {
            user.setOrganizerVerificationStatus(User.VerificationStatus.rejected);
            log.info("Usuario {} NO cumple requisitos para ORGANIZADOR", userId);
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }

    /**
     * Verifica ENTRENADOR: el quiz aprobado es el único requisito operativo.
     */
    @Transactional
    public UserProfileResponse verifyTrainer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.isTrainerQuizPassed()) {
            user.setTrainerVerificationStatus(User.VerificationStatus.approved);
            appendVerifiedRole(user, "ENTRENADOR");
            log.info("Usuario {} verificado como ENTRENADOR (quiz)", userId);
        } else {
            user.setTrainerVerificationStatus(User.VerificationStatus.rejected);
            log.info("Usuario {} NO cumple requisitos para ENTRENADOR", userId);
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }

    /**
     * Incrementa eventos asistidos del perfil (llamado desde sports).
     */
    @Transactional
    public void incrementEventsAttended(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setEventsAttended(user.getEventsAttended() + 1);
        userRepository.save(user);
    }

    /**
     * Incrementa eventos creados y marca testEventCreated si aplica.
     */
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

    /**
     * Guarda experiencia y disciplinas antes del quiz.
     * Si los años son menores al mínimo, bloquea la cuenta sin revelar el motivo.
     */
    @Transactional
    public QuizPrepResponse prepareQuiz(String userId, String role, QuizPrepRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String normalizedRole = normalizeQuizRole(role);
        if (!user.isActive()) {
            throw new SilentAccessDeniedException();
        }

        int years = request.getExperienceYears() == null ? 0 : request.getExperienceYears();
        if (years < MIN_QUIZ_EXPERIENCE_YEARS) {
            log.warn("Bloqueo silencioso por experiencia insuficiente para usuario {} (rol {})", userId, normalizedRole);
            user.setActive(false);
            user.setBlockedPermanently(true);
            user.setBlockedUntil(null);
            user.setBlockReason(null);
            userRepository.save(user);
            throw new SilentAccessDeniedException();
        }

        if (isQuizPassed(user, normalizedRole)) {
            return buildPrepResponse(user, normalizedRole, "Quiz ya aprobado.");
        }
        if (attemptsUsed(user, normalizedRole) >= MAX_QUIZ_ATTEMPTS) {
            throw new RuntimeException("Has agotado los intentos de verificación.");
        }

        user.setExperienceMonths(years * 12);
        user.setQuizDisciplines(serializeDisciplineIds(request.getDisciplineSportIds()));
        userRepository.save(user);

        return buildPrepResponse(user, normalizedRole,
                "Datos guardados. Puedes iniciar el quiz.");
    }

    /**
     * Consulta el estado de prep/intentos/aprobación del quiz para un rol.
     */
    @Transactional(readOnly = true)
    public QuizPrepResponse getQuizPrepStatus(String userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return buildPrepResponse(user, normalizeQuizRole(role), null);
    }

    /**
     * Persiste el puntaje del quiz de organizador (aprueba con score >= 70).
     */
    @Transactional
    public void saveOrganizerQuizScore(String userId, double score) {
        applyQuizScore(userId, "ORGANIZADOR", score, 70.0);
    }

    /**
     * Persiste el puntaje del quiz de entrenador (aprueba con score >= 75).
     */
    @Transactional
    public void saveTrainerQuizScore(String userId, double score) {
        applyQuizScore(userId, "ENTRENADOR", score, 75.0);
    }

    /**
     * Aplica puntaje, marca aprobado o incrementa intentos fallidos según el umbral.
     */
    private void applyQuizScore(String userId, String role, double score, double threshold) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!user.isActive()) {
            throw new SilentAccessDeniedException();
        }
        if (isQuizPassed(user, role)) {
            return;
        }
        if (attemptsUsed(user, role) >= MAX_QUIZ_ATTEMPTS) {
            throw new RuntimeException("Has agotado los intentos de verificación.");
        }

        boolean passed = score >= threshold;
        if ("ORGANIZADOR".equals(role)) {
            user.setOrganizerQuizScore(score);
            user.setOrganizerQuizPassed(passed);
            if (passed) {
                user.setOrganizerVerificationStatus(User.VerificationStatus.approved);
                appendVerifiedRole(user, "ORGANIZADOR");
            } else {
                user.setOrganizerQuizAttempts(user.getOrganizerQuizAttempts() + 1);
            }
        } else {
            user.setTrainerQuizScore(score);
            user.setTrainerQuizPassed(passed);
            if (passed) {
                user.setTrainerVerificationStatus(User.VerificationStatus.approved);
                appendVerifiedRole(user, "ENTRENADOR");
            } else {
                user.setTrainerQuizAttempts(user.getTrainerQuizAttempts() + 1);
            }
        }
        userRepository.save(user);
    }

    /**
     * MÉTODOS DE ACTIVACIÓN/DESACTIVACIÓN DE USUARIOS
     */

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
     * Elimina definitivamente el perfil (y roles/actividad en cascada).
     * No elimina la cuenta en auth-ms; el usuario deja de aparecer en el panel.
     */
    @Transactional
    public void deleteUser(String email, String adminEmail, String ipAddress) {
        if (adminEmail != null && adminEmail.equalsIgnoreCase(email)) {
            throw new RuntimeException("No puedes eliminarte a ti mismo");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
        String userId = user.getId();
        userRepository.delete(user);
        adminAuditService.log(adminEmail, "DELETE_USER", email, userId, "{}", ipAddress);
        log.info("Usuario eliminado: {}", email);
    }

    /** Cada email se elimina en su propia transacción para permitir éxitos parciales. */
    public BulkActionResponse bulkDeleteUsers(List<String> emails, String adminEmail, String ipAddress) {
        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (emails == null || emails.isEmpty()) {
            return BulkActionResponse.builder().succeeded(0).failed(0).build();
        }

        for (String raw : emails) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String email = raw.trim();
            try {
                deleteUser(email, adminEmail, ipAddress);
                succeeded.add(email);
            } catch (Exception ex) {
                failed.add(email);
                errors.add(email + ": " + ex.getMessage());
            }
        }

        return BulkActionResponse.builder()
                .succeeded(succeeded.size())
                .failed(failed.size())
                .succeededEmails(succeeded)
                .failedEmails(failed)
                .errors(errors)
                .build();
    }

    /**
     * RF28 / auth: estado de acceso efectivo (reactiva bloqueos temporales vencidos).
     * Solo expone motivo si el admin lo registró; los bloqueos silenciosos van sin reason.
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

        if (!allowed && user.getBlockReason() != null && !user.getBlockReason().isBlank()) {
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

    /**
     * Añade un rol verificado a la lista serializada del usuario si aún no está.
     */
    private void appendVerifiedRole(User user, String role) {
        String current = user.getVerifiedRoles() == null ? "" : user.getVerifiedRoles();
        if (!current.contains(role)) {
            user.setVerifiedRoles(current.isBlank() ? role : current + "," + role);
        }
    }

    /**
     * Normaliza alias de rol (organizer/trainer/coach) a ORGANIZADOR o ENTRENADOR.
     */
    private String normalizeQuizRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if ("ORGANIZER".equals(normalized) || "ORGANIZADOR".equals(normalized)) {
            return "ORGANIZADOR";
        }
        if ("TRAINER".equals(normalized) || "COACH".equals(normalized) || "ENTRENADOR".equals(normalized)) {
            return "ENTRENADOR";
        }
        throw new RuntimeException("Rol de quiz inválido. Usa ORGANIZADOR o ENTRENADOR.");
    }

    /**
     * Indica si el usuario ya aprobó el quiz del rol indicado.
     */
    private boolean isQuizPassed(User user, String role) {
        return "ORGANIZADOR".equals(role) ? user.isOrganizerQuizPassed() : user.isTrainerQuizPassed();
    }

    /**
     * Devuelve los intentos fallidos consumidos para el rol.
     */
    private int attemptsUsed(User user, String role) {
        return "ORGANIZADOR".equals(role) ? user.getOrganizerQuizAttempts() : user.getTrainerQuizAttempts();
    }

    /**
     * Serializa IDs de disciplinas a una cadena separada por comas.
     */
    private String serializeDisciplineIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Parsea la cadena de disciplinas persistida a una lista de IDs.
     */
    private List<Integer> parseDisciplineIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    /**
     * Arma la respuesta de prep con intentos restantes y si puede iniciar el quiz.
     */
    private QuizPrepResponse buildPrepResponse(User user, String role, String message) {
        int used = attemptsUsed(user, role);
        int remaining = Math.max(0, MAX_QUIZ_ATTEMPTS - used);
        boolean passed = isQuizPassed(user, role);
        boolean canStart = user.isActive() && !passed && remaining > 0
                && user.getExperienceMonths() >= MIN_QUIZ_EXPERIENCE_YEARS * 12
                && user.getQuizDisciplines() != null && !user.getQuizDisciplines().isBlank();

        return QuizPrepResponse.builder()
                .role(role)
                .canStartQuiz(canStart)
                .quizPassed(passed)
                .experienceYears(user.getExperienceMonths() / 12)
                .disciplineSportIds(parseDisciplineIds(user.getQuizDisciplines()))
                .attemptsUsed(used)
                .attemptsRemaining(remaining)
                .maxAttempts(MAX_QUIZ_ATTEMPTS)
                .lastScore("ORGANIZADOR".equals(role) ? user.getOrganizerQuizScore() : user.getTrainerQuizScore())
                .message(message)
                .build();
    }

    /**
     * Mapea la entidad User al DTO de perfil, incluyendo flags e intentos de quiz.
     */
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
                .experienceYears(user.getExperienceMonths() / 12)
                .eventsAsTrainer(user.getEventsAsTrainer())
                .trainerQuizScore(user.getTrainerQuizScore())
                .trainerQuizPassed(user.isTrainerQuizPassed())
                .trainerQuizAttempts(user.getTrainerQuizAttempts())
                .organizerQuizAttempts(user.getOrganizerQuizAttempts())
                .quizDisciplines(user.getQuizDisciplines())
                .disciplineSportIds(parseDisciplineIds(user.getQuizDisciplines()))
                .identityDocument(user.getIdentityDocument())
                .trainerVerificationStatus(
                        user.getTrainerVerificationStatus() != null ?
                        user.getTrainerVerificationStatus().name() : "pending"
                )
                .verifiedRoles(user.getVerifiedRoles())
                .build();
    }
}