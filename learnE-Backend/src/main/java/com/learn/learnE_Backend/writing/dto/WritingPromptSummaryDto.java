package com.learn.learnE_Backend.writing.dto;

import com.learn.learnE_Backend.vocabulary.CourseLevel;

public record WritingPromptSummaryDto(
        Long id,
        String title,
        String topicName,
        CourseLevel level,
        int minWords,
        Integer bestScore,
        int submissionsCount
) {
}
