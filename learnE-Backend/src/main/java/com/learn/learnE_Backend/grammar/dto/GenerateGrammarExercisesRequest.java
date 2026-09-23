package com.learn.learnE_Backend.grammar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** @param count omitted means the lesson's own target, derived from its difficulty (5..25). */
public record GenerateGrammarExercisesRequest(
        @Min(1) @Max(25) Integer count,
        Boolean replaceExisting
) {
    public boolean replaceOrDefault() {
        return replaceExisting == null || replaceExisting;
    }
}
