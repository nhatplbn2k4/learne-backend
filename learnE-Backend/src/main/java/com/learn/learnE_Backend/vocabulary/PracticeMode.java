package com.learn.learnE_Backend.vocabulary;

/**
 * The question types a word can be practised in. Which ones a word must be aced in to count as
 * mastered depends on its language — see {@link LanguageModes}.
 */
public enum PracticeMode {
    /** Shown the word, recall the meaning yourself. */
    FLASHCARD,
    /** Hear the word, type it. */
    LISTEN_TYPE,
    /** Shown the Vietnamese meaning, type the word. */
    TRANSLATE_TYPE,
    /** Shown the word, pick the Vietnamese meaning. */
    MULTIPLE_CHOICE_VI,
    /** Shown the Vietnamese meaning, pick the word. */
    MULTIPLE_CHOICE_TERM,
    /** Chinese only: shown the characters, pick the pinyin with the right tones. */
    PINYIN_TONE,
    /** Chinese only: hear the word, pick the characters. */
    LISTEN_CHOICE
}
