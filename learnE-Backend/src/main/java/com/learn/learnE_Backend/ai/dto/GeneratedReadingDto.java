package com.learn.learnE_Backend.ai.dto;

import java.util.List;

public record GeneratedReadingDto(
        String title,
        String content,
        List<GeneratedReadingQuestionDto> questions
) {
}
