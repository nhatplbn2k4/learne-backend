package com.learn.learnE_Backend.ai.dto;

import com.learn.learnE_Backend.vocabulary.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateReadingRequest(
        @NotBlank String topicName,
        @NotNull CourseLevel level,
        @NotBlank String theme
) {
}
