package com.learn.learnE_Backend.sentence.dto;

import com.learn.learnE_Backend.grammar.dto.GrammarRefDto;

import java.util.List;

/** One exercise as the learner sees it: the answer is withheld until they have attempted it. */
public record SentenceExerciseDto(
        Long id,
        int sortOrder,
        String promptVi,
        List<NewWordHintDto> newWords,
        /**
         * Grammar this sentence uses that the learner has not studied yet. A warning, not a block:
         * they may still translate it, they just know what they are up against.
         */
        List<GrammarRefDto> unlearnedGrammar,
        /** Null until this learner has submitted an answer. */
        String answerTarget,
        String answerPhonetic,
        SentenceAttemptDto lastAttempt,
        Integer bestScore
) {
}
