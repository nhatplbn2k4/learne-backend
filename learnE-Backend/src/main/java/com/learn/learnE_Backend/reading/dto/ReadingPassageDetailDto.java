package com.learn.learnE_Backend.reading.dto;

import com.learn.learnE_Backend.vocabulary.CourseLevel;

import java.util.List;

public record ReadingPassageDetailDto(
        Long id,
        String title,
        String content,
        String topicName,
        CourseLevel level,
        List<ReadingQuestionDto> questions
) {
}
