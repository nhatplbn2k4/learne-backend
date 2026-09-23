package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.UserCourseEnrollment;

public record EnrollmentDto(
        Long id,
        Long courseId,
        int currentDayNumber,
        int streakCount
) {
    public static EnrollmentDto from(UserCourseEnrollment enrollment) {
        return new EnrollmentDto(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCurrentDayNumber(),
                enrollment.getStreakCount()
        );
    }
}
