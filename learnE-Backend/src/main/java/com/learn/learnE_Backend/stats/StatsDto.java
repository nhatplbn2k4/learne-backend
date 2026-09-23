package com.learn.learnE_Backend.stats;

import com.learn.learnE_Backend.vocabulary.Language;

/**
 * Dashboard counters. Reading and writing exist only for English and sentence translation only for
 * Chinese, so the counters that don't apply to {@code language} come back as zero/null.
 */
public record StatsDto(
        Language language,
        int coursesEnrolled,
        int totalWordsLearned,
        int wordsMastered,
        int wordsDueToday,
        int longestStreak,
        int readingAttemptsCount,
        Double readingAverageScorePercent,
        int writingSubmissionsCount,
        Double writingAverageScore,
        int sentenceAttemptsCount,
        Double sentenceAverageScore
) {
}
