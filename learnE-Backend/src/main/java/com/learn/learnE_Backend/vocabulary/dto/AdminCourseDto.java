package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.Course;
import com.learn.learnE_Backend.vocabulary.CourseKind;
import com.learn.learnE_Backend.vocabulary.CourseLevel;
import com.learn.learnE_Backend.vocabulary.Language;

public record AdminCourseDto(
        Long id,
        Long topicId,
        String topicName,
        Language language,
        CourseLevel level,
        CourseKind kind,
        String levelLabel,
        String title,
        String description,
        int sortOrder,
        /** Empty means the course uses its language's default set. */
        java.util.Set<com.learn.learnE_Backend.vocabulary.PracticeMode> practiceModes,
        boolean sentenceTranslationEnabled,
        boolean daysAlwaysUnlocked,
        int lessonDayCount
) {
    public static AdminCourseDto from(Course course, int lessonDayCount) {
        return new AdminCourseDto(
                course.getId(),
                course.getTopic().getId(),
                course.getTopic().getName(),
                course.getLanguage(),
                course.getLevel(),
                course.getKind(),
                course.getLevelLabel(),
                course.getTitle(),
                course.getDescription(),
                course.getSortOrder(),
                course.getPracticeModes(),
                course.isSentenceTranslationEnabled(),
                course.isDaysAlwaysUnlocked(),
                lessonDayCount
        );
    }
}
