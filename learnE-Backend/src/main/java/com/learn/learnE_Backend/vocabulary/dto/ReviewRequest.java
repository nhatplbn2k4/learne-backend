package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.PracticeMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.learn.learnE_Backend.vocabulary.ReviewQuality;

public record ReviewRequest(
        @NotNull Long wordId,
        @NotNull ReviewQuality quality,
        @NotNull PracticeMode mode,
        /** Identifies one practice run: a word is mastered only by acing every mode within it. */
        @NotBlank String sessionId
) {
}
