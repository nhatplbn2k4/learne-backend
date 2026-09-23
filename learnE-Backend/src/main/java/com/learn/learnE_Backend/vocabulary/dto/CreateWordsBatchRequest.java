package com.learn.learnE_Backend.vocabulary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateWordsBatchRequest(
        @NotEmpty @Valid List<CreateWordRequest> words,
        /** Optional: also rename the lesson day (used when the AI generated its title too). */
        String dayTitle
) {
}
