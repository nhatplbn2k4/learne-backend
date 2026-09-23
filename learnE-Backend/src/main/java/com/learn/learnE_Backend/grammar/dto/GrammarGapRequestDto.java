package com.learn.learnE_Backend.grammar.dto;

import com.learn.learnE_Backend.grammar.GrammarGapRequest;
import com.learn.learnE_Backend.grammar.GrammarGapStatus;
import com.learn.learnE_Backend.vocabulary.Language;

import java.time.Instant;

public record GrammarGapRequestDto(
        Long id,
        Language language,
        String grammarName,
        /** One sample occurrence, so the admin can see what the learner was trying to say. */
        String examplePrompt,
        String exampleAnswer,
        String explanation,
        int reportCount,
        Instant firstReportedAt,
        Instant lastReportedAt,
        GrammarGapStatus status,
        Long resolvedLessonId,
        String resolvedLessonTitle
) {
    public static GrammarGapRequestDto from(GrammarGapRequest gap) {
        return new GrammarGapRequestDto(
                gap.getId(),
                gap.getLanguage(),
                gap.getGrammarName(),
                gap.getExamplePrompt(),
                gap.getExampleAnswer(),
                gap.getExplanation(),
                gap.getReportCount(),
                gap.getFirstReportedAt(),
                gap.getLastReportedAt(),
                gap.getStatus(),
                gap.getResolvedLesson() == null ? null : gap.getResolvedLesson().getId(),
                gap.getResolvedLesson() == null ? null : gap.getResolvedLesson().getTitle());
    }
}
