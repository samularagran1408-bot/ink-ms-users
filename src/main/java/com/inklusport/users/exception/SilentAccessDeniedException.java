package com.inklusport.users.exception;

/**
 * Bloqueo de acceso sin revelar la causa real al cliente.
 * Se usa cuando la experiencia declarada no cumple el mínimo del quiz.
 */
public class SilentAccessDeniedException extends RuntimeException {

    /**
     * Mensaje genérico expuesto al cliente (no menciona la causa).
     */
    public static final String GENERIC_MESSAGE = "No se pudo completar el acceso.";

    /**
     * Crea la excepción con el mensaje genérico de acceso denegado.
     */
    public SilentAccessDeniedException() {
        super(GENERIC_MESSAGE);
    }
}
