package com.inklusport.users.controller;

import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.dto.ErrorResponse;
import com.inklusport.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final UserService userService;

    /**
     * Verifica si un usuario cumple los requisitos para ser ORGANIZADOR.
     * POST /api/users/verify/organizer/{userId}
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
     * Verifica si un usuario cumple los requisitos para ser ENTRENADOR.
     * POST /api/users/verify/trainer/{userId}
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
     * Obtiene el estado de verificación completo de un usuario.
     * GET /api/users/verify/status/{userId}
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
     * Incrementa el contador de eventos asistidos (llamado desde Sports Service).
     * POST /api/users/verify/attended/{userId}
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
     * Incrementa el contador de eventos creados (llamado desde Sports Service).
     * POST /api/users/verify/created/{userId}
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
     * Guarda el puntaje del quiz de ORGANIZADOR.
     * POST /api/users/verify/quiz/organizer/{userId}?score=85.5
     */
    @PostMapping("/quiz/organizer/{userId}")
    public ResponseEntity<?> saveOrganizerQuizScore(
            @PathVariable String userId,
            @RequestParam double score) {
        try {
            log.info("Guardando puntaje de quiz ORGANIZADOR para usuario {}: {}", userId, score);
            userService.saveOrganizerQuizScore(userId, score);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/quiz/organizer/" + userId);
        }
    }

    /**
     * Guarda el puntaje del quiz de ENTRENADOR.
     * POST /api/users/verify/quiz/trainer/{userId}?score=85.5
     */
    @PostMapping("/quiz/trainer/{userId}")
    public ResponseEntity<?> saveTrainerQuizScore(
            @PathVariable String userId,
            @RequestParam double score) {
        try {
            log.info("Guardando puntaje de quiz ENTRENADOR para usuario {}: {}", userId, score);
            userService.saveTrainerQuizScore(userId, score);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return buildErrorResponse(e, "/api/users/verify/quiz/trainer/" + userId);
        }
    }

    /**
     * Construye una respuesta de error estandarizada
     * @param e
     * @param path
     * @return
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