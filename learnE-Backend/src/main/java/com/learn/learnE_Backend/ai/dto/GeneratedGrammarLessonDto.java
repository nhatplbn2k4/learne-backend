package com.learn.learnE_Backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.learn.learnE_Backend.grammar.dto.GrammarContentDto;

import java.util.List;

/** An AI-written grammar lesson, for an admin to review before it is saved. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedGrammarLessonDto(
        String code,
        String title,
        String summary,
        String formula,
        List<GrammarContentDto.ComponentDto> components,
        List<GrammarContentDto.ExampleDto> examples,
        String notes,
        Integer difficulty
) {
}
