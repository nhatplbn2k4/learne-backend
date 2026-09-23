package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.Course;
import com.learn.learnE_Backend.vocabulary.CourseKind;
import com.learn.learnE_Backend.vocabulary.CourseLevel;
import com.learn.learnE_Backend.vocabulary.Language;

public record CourseSummaryDto(
        Long id,
        String topicName,
        Language language,
        CourseLevel level,
        CourseKind kind,
        /** Free-text level shown instead of {@code level} when set, e.g. "Boya Sơ cấp I". */
        String levelLabel,
        String title,
        String description,
        boolean enrolled
) {
    public static CourseSummaryDto from(Course course, boolean enrolled) {
        return new CourseSummaryDto(
                course.getId(),
                course.getTopic().getName(),
                course.getLanguage(),
                course.getLevel(),
                course.getKind(),
                course.getLevelLabel(),
                course.getTitle(),
                course.getDescription(),
                enrolled
        );
    }
}
