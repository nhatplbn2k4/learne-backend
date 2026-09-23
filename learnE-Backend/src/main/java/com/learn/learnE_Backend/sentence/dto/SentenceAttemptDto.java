package com.learn.learnE_Backend.sentence.dto;

import java.time.Instant;

public record SentenceAttemptDto(
        Long id,
        Long exerciseId,
        String answerTarget,
        Integer score,
        SentenceFeedbackDto feedback,
        Instant submittedAt
) {
}
