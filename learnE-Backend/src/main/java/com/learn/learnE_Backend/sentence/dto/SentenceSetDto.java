package com.learn.learnE_Backend.sentence.dto;

import com.learn.learnE_Backend.vocabulary.Language;

import java.util.List;

/** The whole translation set for one lesson day, plus why it is locked if it is. */
public record SentenceSetDto(
        Long lessonDayId,
        int dayNumber,
        String dayTitle,
        String courseTitle,
        Language language,
        boolean unlocked,
        /** Null when unlocked. */
        String lockedReason,
        int masteredWordCount,
        int totalWordCount,
        /** Sentences answered and the average score, shown above the exercises. */
        DaySentenceStatsDto progress,
        /** How close this day is to unlocking the next one early. */
        SkipAheadDto skipAhead,
        List<SentenceExerciseDto> exercises
) {
}
