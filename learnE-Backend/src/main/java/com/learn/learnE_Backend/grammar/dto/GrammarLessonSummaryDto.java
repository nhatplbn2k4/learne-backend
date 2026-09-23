package com.learn.learnE_Backend.grammar.dto;

/** A row in the lesson list: enough to choose a lesson without loading its whole content. */
public record GrammarLessonSummaryDto(
        Long id,
        String code,
        String title,
        String summary,
        String formula,
        /** 1..5, shown as how demanding the pattern is. */
        int difficulty,
        String sourceLessonLabel,
        int exerciseCount,
        /** True once the learner has submitted at least one drill sentence. */
        boolean learned,
        /** Distinct sentences answered, not submissions. */
        int exercisesDone,
        /** Mean of the best score per answered sentence; null before anything is graded. */
        Double averageScore
) {
}
