package com.learn.learnE_Backend.ai.dto;

import java.util.List;

/** A day's generated content: a sub-theme title plus the vocabulary for it. */
public record GeneratedDayWordsDto(
        String dayTitle,
        List<GeneratedWordDto> words
) {
}
