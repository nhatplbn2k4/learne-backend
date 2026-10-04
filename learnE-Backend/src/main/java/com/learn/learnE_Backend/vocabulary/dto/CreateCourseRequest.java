package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.CourseKind;
import com.learn.learnE_Backend.vocabulary.CourseLevel;
import com.learn.learnE_Backend.vocabulary.PracticeMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.EnumSet;
import java.util.Set;

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
        Integer sortOrder,
        /** Omitted or empty means the language's default set, not "no modes at all". */
        Set<PracticeMode> practiceModes,
        /** Boxed on purpose: the admin UI omits these keys, and absent must not read as false. */
        Boolean sentenceTranslationEnabled,
        Boolean daysAlwaysUnlocked
) {
    public CourseKind kindOrDefault() {
        return kind == null ? CourseKind.VOCABULARY : kind;
    }

    public EnumSet<PracticeMode> practiceModesOrEmpty() {
        return practiceModes == null || practiceModes.isEmpty()
                ? EnumSet.noneOf(PracticeMode.class)
                : EnumSet.copyOf(practiceModes);
    }

    public boolean sentenceTranslationEnabledOrDefault() {
        return sentenceTranslationEnabled == null || sentenceTranslationEnabled;
    }

    public boolean daysAlwaysUnlockedOrDefault() {
        return daysAlwaysUnlocked != null && daysAlwaysUnlocked;
    }
}
