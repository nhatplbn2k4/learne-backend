package com.learn.learnE_Backend.reading.dto;

public record GradedAnswerDto(
        Long questionId,
        String selectedOption,
        String correctOption,
        boolean correct,
        String explanation
) {
}
