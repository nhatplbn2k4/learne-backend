package com.learn.learnE_Backend.grammar.dto;

import com.learn.learnE_Backend.vocabulary.Language;

import java.util.List;

/** One grammar lesson in full: the explanation followed by its drill sentences. */
public record GrammarLessonDto(
        Long id,
        Long courseId,
        String courseTitle,
        Language language,
        String code,
        String title,
        String summary,
        String formula,
        List<GrammarContentDto.ComponentDto> components,
        List<GrammarContentDto.ExampleDto> examples,
        String notes,
        String sourceLessonLabel,
        int difficulty,
        boolean learned,
        List<GrammarExerciseDto> exercises
) {
}
