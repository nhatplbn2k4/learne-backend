package com.learn.learnE_Backend.vocabulary;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** The single place declaring which practice modes each language requires for mastery. */
public final class LanguageModes {

    private static final Map<Language, List<PracticeMode>> REQUIRED_MODES = Map.of(
            Language.ENGLISH, List.of(
                    PracticeMode.FLASHCARD,
                    PracticeMode.LISTEN_TYPE,
                    PracticeMode.TRANSLATE_TYPE,
                    PracticeMode.MULTIPLE_CHOICE_VI,
                    PracticeMode.MULTIPLE_CHOICE_TERM),
            Language.CHINESE, List.of(
                    PracticeMode.FLASHCARD,
                    PracticeMode.LISTEN_TYPE,
                    PracticeMode.TRANSLATE_TYPE,
                    PracticeMode.MULTIPLE_CHOICE_VI,
                    PracticeMode.MULTIPLE_CHOICE_TERM,
                    PracticeMode.PINYIN_TONE,
                    PracticeMode.LISTEN_CHOICE));

    private LanguageModes() {
    }

    public static List<PracticeMode> requiredFor(Language language) {
        return REQUIRED_MODES.get(language == null ? Language.ENGLISH : language);
    }

    public static Set<PracticeMode> requiredSetFor(Language language) {
        return Set.copyOf(requiredFor(language));
    }
}
