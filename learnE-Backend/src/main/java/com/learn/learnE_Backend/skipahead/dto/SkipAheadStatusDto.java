package com.learn.learnE_Backend.skipahead.dto;

import java.time.Instant;

/**
 * Whether the learner may sit a test for a given target day right now.
 *
 * @param reason        why not, when {@code canStart} is false
 * @param retryAfter    when the cooldown from a failed sitting expires
 * @param openTestId    a test already in progress, which must be finished before starting another
 */
public record SkipAheadStatusDto(
        Long courseId,
        int targetDayNumber,
        boolean canStart,
        String reason,
        Instant retryAfter,
        Long openTestId,
        int requiredSentences,
        double minAverage,
        /** How many practice sentences are available to draw on; the rest come from AI. */
        int poolAvailable,
        Boolean lastPassed,
        Double lastAverageScore
) {
}
