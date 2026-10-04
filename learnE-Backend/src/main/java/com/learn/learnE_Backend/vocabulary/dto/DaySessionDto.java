package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.sentence.dto.DaySentenceStatsDto;
import com.learn.learnE_Backend.vocabulary.Language;
import com.learn.learnE_Backend.vocabulary.PracticeMode;

import java.util.List;

public record DaySessionDto(
        Long lessonDayId,
        int dayNumber,
        String title,
        String courseTitle,
        Language language,
        /**
         * The modes this course asks for. Sent rather than derived on the client so both sides
         * judge mastery by the same list: a course of radicals drops the modes that would make
         * the learner type or hear a character like 氵.
         */
        List<PracticeMode> practiceModes,
        /** False for courses with nothing to translate, which hides the whole feature for them. */
        boolean sentenceTranslationEnabled,
        List<PracticeWordDto> words,
        /** Translation progress for this day, so the session can offer a way straight into it. */
        DaySentenceStatsDto sentences
) {
}
