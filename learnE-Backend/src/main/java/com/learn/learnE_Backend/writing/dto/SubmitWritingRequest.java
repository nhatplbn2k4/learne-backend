package com.learn.learnE_Backend.writing.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitWritingRequest(
        @NotBlank String content
) {
}
