package com.learn.learnE_Backend.vocabulary.dto;

import java.time.LocalDate;

public record ReviewResponse(
        Long wordId,
        int intervalDays,
        LocalDate nextReviewDate,
        boolean lessonDayJustCompleted
) {
}
