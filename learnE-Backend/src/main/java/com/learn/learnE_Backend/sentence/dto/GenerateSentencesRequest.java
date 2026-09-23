package com.learn.learnE_Backend.sentence.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateSentencesRequest(
        @Min(1) @Max(20) Integer count,
        /** Replace the day's existing sentences instead of adding to them. */
        Boolean replaceExisting
) {
    public int countOrDefault() {
        // 20 matches the skip-ahead requirement, so a freshly generated day supports it.
        return count == null ? 20 : count;
    }

    public boolean replaceOrDefault() {
        return replaceExisting != null && replaceExisting;
    }
}
