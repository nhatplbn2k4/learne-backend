package com.learn.learnE_Backend.grammar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Admin payload for creating or updating a grammar lesson. */
public record SaveGrammarLessonRequest(
        /** Lower-case slug; it is what the AI quotes when grading, so it must stay stable. */
        @NotBlank @Size(max = 60)
        @Pattern(regexp = "[a-z0-9-]+", message = "Mã chỉ gồm chữ thường, số và dấu gạch ngang")
        String code,
        @NotBlank @Size(max = 200) String title,
        String summary,
        @NotBlank String formula,
        List<GrammarContentDto.ComponentDto> components,
        List<GrammarContentDto.ExampleDto> examples,
        String notes,
        @Size(max = 60) String sourceLessonLabel,
        @Min(1) @Max(5) Integer difficulty,
        Integer sortOrder
) {
    public int difficultyOrDefault() {
        return difficulty == null ? 1 : difficulty;
    }
}
