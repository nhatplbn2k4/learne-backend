package com.learn.learnE_Backend.ai.dto;

public record GeneratedWritingPromptDto(
        String title,
        String instructions,
        int minWords
) {
}
