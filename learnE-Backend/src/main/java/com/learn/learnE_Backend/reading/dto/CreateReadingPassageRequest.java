package com.learn.learnE_Backend.reading.dto;

import com.learn.learnE_Backend.vocabulary.CourseLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateReadingPassageRequest(
        Long topicId,
        @NotNull CourseLevel level,
        @NotBlank String title,
        @NotBlank String content,
        @NotEmpty @Valid List<CreateReadingQuestionRequest> questions
) {
}
