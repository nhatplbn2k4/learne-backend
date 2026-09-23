package com.learn.learnE_Backend.writing.dto;

import com.learn.learnE_Backend.vocabulary.CourseLevel;

public record WritingPromptDetailDto(
        Long id,
        String title,
        String instructions,
        String topicName,
        CourseLevel level,
        int minWords
) {
}
