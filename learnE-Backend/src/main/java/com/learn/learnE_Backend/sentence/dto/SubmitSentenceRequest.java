package com.learn.learnE_Backend.sentence.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitSentenceRequest(@NotBlank String answerTarget) {
}
