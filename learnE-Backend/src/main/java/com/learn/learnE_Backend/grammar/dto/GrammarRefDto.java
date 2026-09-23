package com.learn.learnE_Backend.grammar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.learn.learnE_Backend.grammar.GrammarLesson;

/**
 * A pointer to the lesson that teaches a grammar point, shown beside a correction or an
 * unlearned-grammar warning.
 *
 * <p>Always built by the server from a code looked up in the database — never taken from whatever
 * the AI wrote, which may name a lesson that does not exist.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GrammarRefDto(
        Long lessonId,
        String code,
        String lessonTitle,
        Long courseId,
        String courseTitle
) {
    public static GrammarRefDto from(GrammarLesson lesson) {
        return new GrammarRefDto(
                lesson.getId(),
                lesson.getCode(),
                lesson.getTitle(),
                lesson.getCourse().getId(),
                lesson.getCourse().getTitle());
    }
}
