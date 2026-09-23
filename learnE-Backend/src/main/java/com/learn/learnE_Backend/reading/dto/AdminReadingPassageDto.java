package com.learn.learnE_Backend.reading.dto;

import com.learn.learnE_Backend.vocabulary.CourseLevel;

import java.util.List;

public record AdminReadingPassageDto(
        Long id,
        String title,
        String content,
        Long topicId,
        String topicName,
        CourseLevel level,
        List<AdminReadingQuestionDto> questions
) {
}
