package com.learn.learnE_Backend.skipahead.dto;

import java.time.Instant;
import java.util.List;

public record SkipAheadTestDto(
        Long id,
        Long courseId,
        int targetDayNumber,
        int requiredSentences,
        double minAverage,
        Instant startedAt,
        boolean finished,
        Boolean passed,
        Double averageScore,
        int answeredCount,
        List<SkipAheadItemDto> items
) {
}
