package com.learn.learnE_Backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedSentencesDto(List<GeneratedSentenceDto> sentences) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedSentenceDto(
            String promptVi,
            String answerTarget,
            String answerPhonetic,
            /** Words used that were not in the vocabulary list given to the AI. */
            List<GeneratedNewWordDto> newWords,
            /** Grammar drills only: which case of the pattern this one exercises. */
            String caseLabel,
            /** Vocabulary practice only: grammar codes the sentence relies on. */
            List<String> grammarCodes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedNewWordDto(String term, String phonetic, String meaning) {
    }
}
