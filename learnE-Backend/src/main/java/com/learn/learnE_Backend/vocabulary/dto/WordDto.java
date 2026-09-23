package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.Word;

public record WordDto(
        Long id,
        String term,
        String phonetic,
        String partOfSpeech,
        String vietnameseMeaning,
        String hanViet,
        String usageNote,
        String exampleSentenceTarget,
        String exampleSentenceVi
) {
    public static WordDto from(Word word) {
        return new WordDto(
                word.getId(),
                word.getTerm(),
                word.getPhonetic(),
                word.getPartOfSpeech(),
                word.getVietnameseMeaning(),
                word.getHanViet(),
                word.getUsageNote(),
                word.getExampleSentenceTarget(),
                word.getExampleSentenceVi()
        );
    }
}
