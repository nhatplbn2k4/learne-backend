package com.learn.learnE_Backend.vocabulary.dto;

/** Status summary of an enrolled course, shown on the dashboard. */
public record EnrollmentSessionDto(
        Long enrollmentId,
        Long courseId,
        String courseTitle,
        int dayNumber,
        String dayTitle,
        boolean courseFinished,
        boolean dayCompleted,
        boolean dayUnlocked,
        String lockedReason,
        int streakCount,
        int newWordCount,
        int dueReviewCount,
        int masteredWordCount,
        int totalWordCount
) {
}
