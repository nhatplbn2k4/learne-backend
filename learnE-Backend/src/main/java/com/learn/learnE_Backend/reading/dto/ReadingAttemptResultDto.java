package com.learn.learnE_Backend.reading.dto;

import java.util.List;

public record ReadingAttemptResultDto(
        Long attemptId,
        int score,
        int totalQuestions,
        List<GradedAnswerDto> answers
) {
}
