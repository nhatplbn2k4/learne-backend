package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.CourseKind;
import com.learn.learnE_Backend.vocabulary.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCourseRequest(
        @NotNull Long topicId,
        @NotNull CourseLevel level,
        /** Omitted means a vocabulary course, which is what every course was before grammar existed. */
        CourseKind kind,
        @NotBlank String title,
        String description,
        /** Optional display label replacing the coarse level, e.g. "Boya Sơ cấp I". */
        String levelLabel,
        /** Optional ordering inside a topic; several courses may share the same level. */
        Integer sortOrder
) {
    public CourseKind kindOrDefault() {
        return kind == null ? CourseKind.VOCABULARY : kind;
    }
}
