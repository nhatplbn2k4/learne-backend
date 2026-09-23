package com.learn.learnE_Backend.skipahead.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitSkipAheadAnswerRequest(@NotBlank String answerTarget) {
}
