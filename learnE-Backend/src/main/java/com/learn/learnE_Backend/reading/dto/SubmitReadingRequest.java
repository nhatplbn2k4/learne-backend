package com.learn.learnE_Backend.reading.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitReadingRequest(
        @NotEmpty @Valid List<SubmitAnswerRequest> answers
) {
}
