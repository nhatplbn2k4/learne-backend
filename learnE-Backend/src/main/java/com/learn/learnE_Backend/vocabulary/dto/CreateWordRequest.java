package com.learn.learnE_Backend.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWordRequest(
        @NotBlank String term,
        String phonetic,
        String partOfSpeech,
        @NotBlank String vietnameseMeaning,
        String hanViet,
        String usageNote,
        String exampleSentenceTarget,
        String exampleSentenceVi
) {
}
