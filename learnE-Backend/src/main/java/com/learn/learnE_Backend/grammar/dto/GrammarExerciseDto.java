package com.learn.learnE_Backend.grammar.dto;

import com.learn.learnE_Backend.sentence.dto.NewWordHintDto;
import com.learn.learnE_Backend.sentence.dto.SentenceAttemptDto;

import java.util.List;

public record GrammarExerciseDto(
        Long id,
        int sortOrder,
        String promptVi,
        /** Which case of the pattern this drills, e.g. "phủ định". */
        String caseLabel,
        List<NewWordHintDto> newWords,
        /** Null until this learner has submitted an answer. */
        String answerTarget,
        String answerPhonetic,
        SentenceAttemptDto lastAttempt,
        Integer bestScore
) {
}
