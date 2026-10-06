package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.UserCourseEnrollment;

import java.time.LocalDate;

public record EnrollmentDto(
        Long id,
        Long courseId,
        int currentDayNumber,
        /** The run still going, not the stored count - that one outlives the streak itself. */
        int streakCount
) {
    public static EnrollmentDto from(UserCourseEnrollment enrollment) {
        return new EnrollmentDto(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCurrentDayNumber(),
                enrollment.currentStreak(LocalDate.now())
        );
    }
}
