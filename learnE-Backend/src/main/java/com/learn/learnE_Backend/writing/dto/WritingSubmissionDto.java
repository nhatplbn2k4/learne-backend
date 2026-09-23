package com.learn.learnE_Backend.writing.dto;

import java.time.Instant;

public record WritingSubmissionDto(
        Long id,
        Long promptId,
        String promptTitle,
        String content,
        int wordCount,
        Integer score,
        WritingFeedbackDto feedback,
        Instant submittedAt
) {
}
