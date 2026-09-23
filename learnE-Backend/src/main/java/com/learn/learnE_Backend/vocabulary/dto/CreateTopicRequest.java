package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.Language;
import jakarta.validation.constraints.NotBlank;

public record CreateTopicRequest(
        @NotBlank String slug,
        @NotBlank String name,
        String description,
        /** Defaults to ENGLISH so older clients keep working. */
        Language language
) {
}
