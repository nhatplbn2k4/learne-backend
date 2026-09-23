package com.learn.learnE_Backend.reading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateReadingQuestionRequest(
        @NotBlank String questionText,
        @NotBlank String optionA,
        @NotBlank String optionB,
        @NotBlank String optionC,
        @NotBlank String optionD,
        @NotBlank @Pattern(regexp = "[ABCD]") String correctOption,
        String explanation
) {
}
