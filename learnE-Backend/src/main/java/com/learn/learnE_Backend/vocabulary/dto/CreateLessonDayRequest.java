package com.learn.learnE_Backend.vocabulary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateLessonDayRequest(
        @NotNull @Min(1) Integer dayNumber,
        String title,
        /** Textbook lesson this day was split out of, e.g. "Bài 12". */
        String sourceLessonLabel,
        /** 1-based part within that lesson. */
        @Min(1) Integer partIndex
) {
}
