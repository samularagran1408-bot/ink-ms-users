package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Estado del prep/quiz para un rol (entrenador u organizador).
 */
@Data
@Builder
public class QuizPrepResponse {

    /**
     * Rol normalizado del quiz (ORGANIZADOR o ENTRENADOR).
     */
    private String role;

    /**
     * Indica si ya puede llamar a generar el quiz en el asistente IA.
     */
    private boolean canStartQuiz;

    /**
     * Indica si el quiz de ese rol ya fue aprobado.
     */
    private boolean quizPassed;

    /**
     * Años de experiencia persistidos (derivados de experienceMonths).
     */
    private Integer experienceYears;

    /**
     * Disciplinas guardadas para personalizar las preguntas.
     */
    private List<Integer> disciplineSportIds;

    /**
     * Intentos fallidos ya consumidos.
     */
    private int attemptsUsed;

    /**
     * Intentos fallidos que aún puede usar.
     */
    private int attemptsRemaining;

    /**
     * Tope de reintentos configurado (por defecto 3).
     */
    private int maxAttempts;

    /**
     * Último puntaje registrado para el rol, si existe.
     */
    private Double lastScore;

    /**
     * Mensaje orientativo para el cliente.
     */
    private String message;
}
