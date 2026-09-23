package com.learn.learnE_Backend.writing.dto;

import com.learn.learnE_Backend.vocabulary.CourseLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWritingPromptRequest(
        Long topicId,
        @NotNull CourseLevel level,
        @NotBlank String title,
        @NotBlank String instructions,
        @Min(1) int minWords
) {
}
