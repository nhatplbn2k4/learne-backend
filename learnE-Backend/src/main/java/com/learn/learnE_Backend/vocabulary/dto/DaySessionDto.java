package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.sentence.dto.DaySentenceStatsDto;
import com.learn.learnE_Backend.vocabulary.Language;

import java.util.List;

public record DaySessionDto(
        Long lessonDayId,
        int dayNumber,
        String title,
        String courseTitle,
        Language language,
        List<PracticeWordDto> words,
        /** Translation progress for this day, so the session can offer a way straight into it. */
        DaySentenceStatsDto sentences
) {
}
