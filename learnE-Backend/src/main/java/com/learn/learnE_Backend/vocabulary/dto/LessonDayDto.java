package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.LessonDay;

public record LessonDayDto(
        Long id,
        Long courseId,
        int dayNumber,
        String title,
        String sourceLessonLabel,
        Integer partIndex,
        int wordCount,
        /** Translation exercises attached to this day (Chinese only). */
        int sentenceCount
) {
    public static LessonDayDto from(LessonDay lessonDay, int wordCount, int sentenceCount) {
        return new LessonDayDto(
                lessonDay.getId(),
                lessonDay.getCourse().getId(),
                lessonDay.getDayNumber(),
                lessonDay.getTitle(),
                lessonDay.getSourceLessonLabel(),
                lessonDay.getPartIndex(),
                wordCount,
                sentenceCount
        );
    }
}
