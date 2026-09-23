package com.learn.learnE_Backend.writing.dto;

import java.util.List;

public record WritingFeedbackDto(
        Integer score,
        String overallComment,
        List<CorrectionDto> corrections
) {
    public static WritingFeedbackDto unavailable(String reason) {
        return new WritingFeedbackDto(null, reason, List.of());
    }
}
