package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.PracticeMode;
import com.learn.learnE_Backend.vocabulary.Word;

import java.util.List;

public record PracticeWordDto(
        Long id,
        String term,
        String phonetic,
        String partOfSpeech,
        String vietnameseMeaning,
        String hanViet,
        String usageNote,
        String exampleSentenceTarget,
        String exampleSentenceVi,
        /** Modes this word has not been answered correctly in yet — practise these first. */
        List<PracticeMode> pendingModes,
        boolean fullyMastered
) {
    public static PracticeWordDto from(Word word, List<PracticeMode> pendingModes) {
        return new PracticeWordDto(
                word.getId(),
                word.getTerm(),
                word.getPhonetic(),
                word.getPartOfSpeech(),
                word.getVietnameseMeaning(),
                word.getHanViet(),
                word.getUsageNote(),
                word.getExampleSentenceTarget(),
                word.getExampleSentenceVi(),
                pendingModes,
                pendingModes.isEmpty()
        );
    }
}
