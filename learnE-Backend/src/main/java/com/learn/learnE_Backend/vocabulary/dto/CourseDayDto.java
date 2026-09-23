package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.sentence.dto.DaySentenceStatsDto;

import java.time.LocalDate;

public record CourseDayDto(
        Long lessonDayId,
        int dayNumber,
        String title,
        int wordCount,
        int masteredWordCount,
        boolean completed,
        boolean unlocked,
        /** Studied on an earlier day but still has words that are not fully mastered. */
        boolean needsReview,
        LocalDate completedOn,
        String lockedReason,
        /** Translation progress for this day; totals are 0 when no sentences exist yet. */
        DaySentenceStatsDto sentences
) {
}
