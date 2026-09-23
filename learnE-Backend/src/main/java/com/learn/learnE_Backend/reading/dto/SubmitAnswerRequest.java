package com.learn.learnE_Backend.reading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SubmitAnswerRequest(
        @NotNull Long questionId,
        @NotBlank @Pattern(regexp = "[ABCD]") String selectedOption
) {
}
