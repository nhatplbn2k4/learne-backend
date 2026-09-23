package com.learn.learnE_Backend.ai.dto;

public record GeneratedReadingQuestionDto(
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String explanation
) {
}
