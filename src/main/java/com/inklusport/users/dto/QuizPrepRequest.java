package com.inklusport.users.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Body del paso previo al quiz: experiencia declarada y disciplinas del catálogo.
 */
@Data
public class QuizPrepRequest {

    /**
     * Años de experiencia declarados por el usuario antes de iniciar el quiz.
     */
    @NotNull(message = "Los años de experiencia son obligatorios")
    @Min(value = 0, message = "Los años de experiencia no pueden ser negativos")
    @Max(value = 80, message = "Los años de experiencia no son válidos")
    private Integer experienceYears;

    /**
     * Identificadores de deportes del catálogo en los que se desempeña el usuario.
     */
    @NotEmpty(message = "Debes indicar al menos una disciplina")
    private List<Integer> disciplineSportIds;
}
