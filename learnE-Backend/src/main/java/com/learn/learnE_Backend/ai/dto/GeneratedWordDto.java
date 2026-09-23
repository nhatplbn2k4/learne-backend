package com.learn.learnE_Backend.ai.dto;

/**
 * One generated word. The field names are language-neutral: {@code term} is the English word or the
 * 汉字, {@code phonetic} is IPA or pinyin with tone marks. {@code hanViet} and {@code usageNote} are
 * only filled in for Chinese.
 */
public record GeneratedWordDto(
        String term,
        String phonetic,
        String partOfSpeech,
        String vietnameseMeaning,
        String hanViet,
        String usageNote,
        String exampleSentenceTarget,
        String exampleSentenceVi
) {
}
