package com.learn.learnE_Backend.reading.dto;

import com.learn.learnE_Backend.vocabulary.CourseLevel;

public record ReadingPassageSummaryDto(
        Long id,
        String title,
        String topicName,
        CourseLevel level,
        int questionCount,
        Integer bestScore,
        int attemptsCount
) {
}
