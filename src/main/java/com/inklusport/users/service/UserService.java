package com.inklusport.users.service;

import com.inklusport.users.client.AuthServiceClient;
import com.inklusport.users.client.SportsServiceClient;
import com.inklusport.users.dto.BlockUserRequest;
import com.inklusport.users.dto.BulkActionResponse;
import com.inklusport.users.dto.CreateProfileFromRegisterRequest;
import com.inklusport.users.dto.FutureRegistrationsCheckResponse;
import com.inklusport.users.dto.LastLoginResponse;
import com.inklusport.users.dto.PageResponse;
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
import com.inklusport.users.exception.UserHasFutureEventsException;
import com.inklusport.users.repository.RoleRepository;
import com.inklusport.users.repository.UserRepository;
import com.inklusport.users.repository.UserRoleRepository;
import com.inklusport.users.util.DisabilityProfileRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Gestión de perfiles de usuario, verificación, bloqueos y listados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AdminAuditService adminAuditService;
    private final AdminNotificationService adminNotificationService;
    private final SportsServiceClient sportsServiceClient;
    private final AuthServiceClient authServiceClient;
    private final CloudinaryStorageService cloudinaryStorage;
    private static final int LIST_CAP = 50;


    /**
     * Crea un perfil básico activo a partir de correo y nombre.
     */
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

    /**
     * Asigna el rol USUARIO por defecto si el usuario aún no lo tiene.
     */
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

    /**
     * Recorta espacios y convierte cadenas vacías a null.
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Obtiene el perfil por correo, enriquecido con el último login.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        return withLastLogin(convertToResponse(user));
    }

    /**
     * Obtiene el perfil por identificador, enriquecido con el último login.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return withLastLogin(convertToResponse(user));
    }

    /**
     * Actualiza los campos enviados del perfil y valida datos de acompañante.
     */
    @Transactional
    public UserProfileResponse updateUserProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(trimToNull(request.getPhone()));
        if (request.getProfilePicture() != null) applyProfilePicture(user, request.getProfilePicture());
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
     * Sube la foto de perfil a Cloudinary y la guarda en el usuario.
     */
    @Transactional
    public UserProfileResponse uploadProfilePhoto(String email, MultipartFile file) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        String url = cloudinaryStorage.uploadImage(user.getId(), file);
        user.setProfilePicture(url);
        User saved = userRepository.save(user);
        log.info("Foto de perfil subida a Cloudinary: {}", email);
        return convertToResponse(saved);
    }

    /**
     * Elimina la foto de perfil almacenada y limpia el campo.
     */
    @Transactional
    public UserProfileResponse deleteProfilePhoto(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        applyProfilePicture(user, "");
        User saved = userRepository.save(user);
        log.info("Foto de perfil eliminada: {}", email);
        return convertToResponse(saved);
    }

    /**
     * Aplica, reemplaza o borra la foto según data URL, URL o valor vacío.
     */
    private void applyProfilePicture(User user, String incoming) {
        if (incoming == null) {
            return;
        }
        String value = incoming.trim();
        if (value.isEmpty()) {
            cloudinaryStorage.deleteStored(user.getId(), user.getProfilePicture());
            user.setProfilePicture(null);
            return;
        }
        if (value.regionMatches(true, 0, "data:image/", 0, 11)) {
            String url = cloudinaryStorage.uploadDataUrl(user.getId(), value);
            user.setProfilePicture(url);
            return;
        }
        if (value.startsWith("https://") || value.startsWith("http://")) {
            String previous = user.getProfilePicture();
            if (previous != null && !previous.equals(value)) {
                cloudinaryStorage.deleteStored(user.getId(), previous);
            }
            user.setProfilePicture(value);
            return;
        }
        throw new RuntimeException("Formato de foto de perfil no válido.");
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

    /**
     * Bloquea al usuario de forma permanente o temporal y audita la acción.
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

    /**
     * Reactiva al usuario y limpia bloqueos previos.
     */
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
     * Eliminación lógica. Se bloquea si el usuario tiene eventos futuros inscritos.
     */
    @Transactional
    public void deleteUser(String email, String adminEmail, String ipAddress) {
        if (adminEmail != null && adminEmail.equalsIgnoreCase(email)) {
            throw new RuntimeException("No puedes eliminarte a ti mismo");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
        if (user.isDeleted()) {
            throw new RuntimeException("El usuario ya está eliminado: " + email);
        }

        assertNoFutureRegistrations(user);

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setActive(false);
        user.setBlockReason("Eliminación lógica por administrador");
        cloudinaryStorage.deleteStored(user.getId(), user.getProfilePicture());
        user.setProfilePicture(null);
        userRepository.save(user);

        adminAuditService.log(adminEmail, "SOFT_DELETE_USER", email, user.getId(), "{}", ipAddress);
        log.info("Usuario eliminado lógicamente: {}", email);
    }

    /**
     * Impide eliminar al usuario si tiene inscripciones a eventos futuros.
     */
    private void assertNoFutureRegistrations(User user) {
        FutureRegistrationsCheckResponse check;
        try {
            check = sportsServiceClient.getFutureRegistrations(user.getId());
        } catch (Exception e) {
            log.warn("No se pudo consultar eventos futuros de {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException(
                    "No se pudo verificar si el usuario tiene eventos futuros inscritos. Intenta de nuevo.");
        }
        if (check != null && check.isHasFutureRegistrations()) {
            String names = check.getEventNames() == null || check.getEventNames().isEmpty()
                    ? ""
                    : ": " + String.join(", ", check.getEventNames());
            throw new UserHasFutureEventsException(
                    "No se puede eliminar: el usuario tiene " + check.getCount()
                            + " evento(s) futuro(s) inscrito(s)" + names
                            + ". Cancela esas inscripciones primero.");
        }
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

        boolean allowed = user.isActive() && !user.isDeleted();
        String message;
        if (user.isDeleted()) {
            message = "Usuario eliminado";
        } else if (allowed) {
            message = "Acceso permitido";
        } else {
            message = user.isBlockedPermanently()
                    ? "Usuario bloqueado permanentemente"
                    : "Usuario bloqueado temporalmente";
        }

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

    /**
     * Reactiva automáticamente un bloqueo temporal ya vencido.
     */
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

    /**
     * Actualiza el perfil como administrador y deja constancia en auditoría.
     */
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

    /**
     * Página de usuarios visibles (filtro activo/inactivo/todos + búsqueda).
     * size máximo 50.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> pageUsers(
            String filter, String name, String disability, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), LIST_CAP);
        int safePage = Math.max(page, 0);
        Page<User> result = userRepository.pageVisible(
                statusFilter(filter),
                blankToNull(name),
                blankToNull(disability),
                PageRequest.of(safePage, safeSize, Sort.by("fullName").ascending()));
        return PageResponse.of(
                result,
                withLastLogins(result.getContent().stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList())));
    }

    /**
     * Lista usuarios visibles (tope 50). Prefiere {@link #pageUsers} en paneles.
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return pageUsers("all", null, null, 0, LIST_CAP).getContent();
    }

    /**
     * Lista los usuarios visibles activos (tope 50).
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getActiveUsers() {
        return pageUsers("active", null, null, 0, LIST_CAP).getContent();
    }

    /**
     * Lista los usuarios visibles inactivos (tope 50).
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getInactiveUsers() {
        return pageUsers("inactive", null, null, 0, LIST_CAP).getContent();
    }

    /**
     * Busca usuarios visibles por nombre y/o discapacidad (tope 50).
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsers(String name, String disability) {
        return pageUsers("all", name, disability, 0, LIST_CAP).getContent();
    }

    private static String statusFilter(String filter) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return null;
        }
        if ("inactive".equalsIgnoreCase(filter)) {
            return "inactive";
        }
        return "active";
    }

    /**
     * Cuenta los usuarios visibles (no eliminados lógicamente).
     */
    public long countVisibleUsers() {
        return userRepository.countVisible();
    }

    /**
     * Cuenta los usuarios visibles que están activos.
     */
    public long countVisibleActiveUsers() {
        return userRepository.countVisibleActive();
    }

    /**
     * Convierte texto en blanco a null; si no, lo recorta.
     */
    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Indica si existe un usuario con el correo dado.
     */
    @Transactional(readOnly = true)
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Escapa barras y comillas para incrustar texto en JSON.
     */
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
                .deleted(user.isDeleted())
                .deletedAt(user.getDeletedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
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

    /**
     * Completa el último login de un único perfil consultando auth.
     */
    private UserProfileResponse withLastLogin(UserProfileResponse user) {
        withLastLogins(List.of(user));
        return user;
    }

    /**
     * Enriquece una lista de perfiles con los últimos accesos de auth-ms.
     */
    private List<UserProfileResponse> withLastLogins(List<UserProfileResponse> users) {
        if (users == null || users.isEmpty()) {
            return users;
        }
        try {
            List<String> emails = users.stream()
                    .map(UserProfileResponse::getEmail)
                    .filter(Objects::nonNull)
                    .toList();
            if (emails.isEmpty()) {
                return users;
            }
            List<LastLoginResponse> remote = authServiceClient.getLastLogins(emails);
            if (remote == null || remote.isEmpty()) {
                return users;
            }
            Map<String, LocalDateTime> byEmail = new HashMap<>();
            for (LastLoginResponse item : remote) {
                if (item.getEmail() != null && item.getLastLogin() != null) {
                    byEmail.put(item.getEmail().toLowerCase(Locale.ROOT), item.getLastLogin());
                }
            }
            for (UserProfileResponse user : users) {
                if (user.getEmail() == null) {
                    continue;
                }
                LocalDateTime fromAuth = byEmail.get(user.getEmail().toLowerCase(Locale.ROOT));
                if (fromAuth != null) {
                    user.setLastLoginAt(fromAuth);
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudieron consultar últimos accesos en auth-ms: {}", ex.getMessage());
        }
        return users;
    }
}