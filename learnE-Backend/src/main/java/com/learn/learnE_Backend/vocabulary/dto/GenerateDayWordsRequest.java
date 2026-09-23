package com.learn.learnE_Backend.vocabulary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record GenerateDayWordsRequest(
        @Min(1) @Max(50) int count,
        /** Words generated earlier in the same bulk run but not saved yet — also avoid these. */
        List<String> excludeWords,
        /** Day titles generated earlier in the same bulk run but not saved yet. */
        List<String> excludeTitles
) {
}
