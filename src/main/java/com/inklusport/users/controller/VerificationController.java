package com.inklusport.users.controller;

import com.inklusport.users.dto.ErrorResponse;
import com.inklusport.users.dto.QuizPrepRequest;
import com.inklusport.users.dto.QuizPrepResponse;
import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.exception.SilentAccessDeniedException;
import com.inklusport.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoints de verificación de roles y del flujo de quiz de aptitud.
 */
@RestController
@RequestMapping("/api/users/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final UserService userService;

    /**
     * Evalúa si el usuario puede quedar verificado como ORGANIZADOR (quiz aprobado).
     */
    @PostMapping("/organizer/{userId}")
    public ResponseEntity<?> verifyOrganizer(@PathVariable String userId) {
        try {
            log.info("Verificando ORGANIZADOR para usuario: {}", userId);
            UserProfileResponse response = userService.verifyOrganizer(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/organizer/" + userId);
        }
    }

    /**
     * Evalúa si el usuario puede quedar verificado como ENTRENADOR (quiz aprobado).
     */
    @PostMapping("/trainer/{userId}")
    public ResponseEntity<?> verifyTrainer(@PathVariable String userId) {
        try {
            log.info("Verificando ENTRENADOR para usuario: {}", userId);
            UserProfileResponse response = userService.verifyTrainer(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/trainer/" + userId);
        }
    }

    /**
     * Devuelve el estado de verificación y flags de quiz del usuario.
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getVerificationStatus(@PathVariable String userId) {
        try {
            log.info("Consultando estado de verificación para usuario: {}", userId);
            UserProfileResponse response = userService.getUserProfileById(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/status/" + userId);
        }
    }

    /**
     * Incrementa el contador de eventos asistidos (consumo interno / sports).
     */
    @PostMapping("/attended/{userId}")
    public ResponseEntity<?> incrementEventsAttended(@PathVariable String userId) {
        try {
            log.info("Incrementando eventos asistidos para usuario: {}", userId);
            userService.incrementEventsAttended(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/attended/" + userId);
        }
    }

    /**
     * Incrementa el contador de eventos creados (consumo interno / sports).
     */
    @PostMapping("/created/{userId}")
    public ResponseEntity<?> incrementEventsCreated(@PathVariable String userId) {
        try {
            log.info("Incrementando eventos creados para usuario: {}", userId);
            userService.incrementEventsCreated(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/created/" + userId);
        }
    }

    /**
     * Paso previo al quiz: guarda años de experiencia y disciplinas.
     * Si la experiencia no cumple el mínimo, bloquea la cuenta sin revelar el motivo.
     */
    @PostMapping("/quiz/prep/{role}/{userId}")
    public ResponseEntity<?> prepareQuiz(
            @PathVariable String role,
            @PathVariable String userId,
            @Valid @RequestBody QuizPrepRequest request) {
        try {
            QuizPrepResponse response = userService.prepareQuiz(userId, role, request);
            return ResponseEntity.ok(response);
        } catch (SilentAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", HttpStatus.FORBIDDEN.value(),
                    "error", "Forbidden",
                    "message", SilentAccessDeniedException.GENERIC_MESSAGE,
                    "accessRevoked", true,
                    "path", "/api/users/verify/quiz/prep/" + role + "/" + userId
            ));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/quiz/prep/" + role + "/" + userId);
        }
    }

    /**
     * Consulta si el usuario puede iniciar el quiz y cuántos intentos le quedan.
     */
    @GetMapping("/quiz/prep/{role}/{userId}")
    public ResponseEntity<?> getQuizPrepStatus(
            @PathVariable String role,
            @PathVariable String userId) {
        try {
            return ResponseEntity.ok(userService.getQuizPrepStatus(userId, role));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/quiz/prep/" + role + "/" + userId);
        }
    }

    /**
     * Registra el puntaje del quiz de ORGANIZADOR (umbral 70).
     */
    @PostMapping("/quiz/organizer/{userId}")
    public ResponseEntity<?> saveOrganizerQuizScore(
            @PathVariable String userId,
            @RequestParam double score) {
        try {
            log.info("Guardando puntaje de quiz ORGANIZADOR para usuario {}: {}", userId, score);
            userService.saveOrganizerQuizScore(userId, score);
            return ResponseEntity.ok().build();
        } catch (SilentAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", SilentAccessDeniedException.GENERIC_MESSAGE,
                    "accessRevoked", true
            ));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/quiz/organizer/" + userId);
        }
    }

    /**
     * Registra el puntaje del quiz de ENTRENADOR (umbral 75).
     */
    @PostMapping("/quiz/trainer/{userId}")
    public ResponseEntity<?> saveTrainerQuizScore(
            @PathVariable String userId,
            @RequestParam double score) {
        try {
            log.info("Guardando puntaje de quiz ENTRENADOR para usuario {}: {}", userId, score);
            userService.saveTrainerQuizScore(userId, score);
            return ResponseEntity.ok().build();
        } catch (SilentAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", SilentAccessDeniedException.GENERIC_MESSAGE,
                    "accessRevoked", true
            ));
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/quiz/trainer/" + userId);
        }
    }

    /**
     * Construye una respuesta de error HTTP 400 estandarizada.
     */
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
}
