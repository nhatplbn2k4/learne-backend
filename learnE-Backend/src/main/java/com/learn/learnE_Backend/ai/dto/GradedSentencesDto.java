package com.learn.learnE_Backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Result of grading a whole test paper in one request, one entry per question. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GradedSentencesDto(List<GradedSentenceDto> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GradedSentenceDto(
            /** 1-based position, so a shuffled or partial reply can still be matched up. */
            Integer index,
            Integer score,
            String comment,
            List<CorrectionDto> corrections,
            String betterVersion
    ) {
    }

    /** Mirrors SentenceFeedbackDto.SentenceCorrectionDto; the server resolves the code afterwards. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CorrectionDto(
            String original,
            String suggestion,
            String explanation,
            String grammarCode,
            String grammarName
    ) {
    }
}
