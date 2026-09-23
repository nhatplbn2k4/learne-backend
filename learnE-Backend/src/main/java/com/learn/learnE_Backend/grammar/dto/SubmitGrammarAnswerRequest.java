package com.learn.learnE_Backend.grammar.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitGrammarAnswerRequest(@NotBlank String answerTarget) {
}
