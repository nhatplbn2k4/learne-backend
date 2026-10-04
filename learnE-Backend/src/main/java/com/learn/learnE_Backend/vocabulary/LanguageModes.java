package com.learn.learnE_Backend.vocabulary;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** The single place declaring which practice modes a course requires for mastery. */
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

    /**
     * What this course asks of a word before calling it mastered: its own list where it sets one,
     * otherwise the language default.
     *
     * <p>Everything that needs the list goes through here — marking mastery, and the list the
     * frontend builds its questions from. Two sources would be a trap rather than a convenience:
     * ask fewer questions than mastery checks for and a word can never be mastered, which in turn
     * never opens sentence translation and leaves the day flagged as needing review forever.
     */
    public static List<PracticeMode> requiredFor(Course course) {
        EnumSet<PracticeMode> own = course.getPracticeModes();
        return own == null || own.isEmpty() ? requiredFor(course.getLanguage()) : List.copyOf(own);
    }

    /** The default for a language, used by every course that does not narrow it. */
    public static List<PracticeMode> requiredFor(Language language) {
        return REQUIRED_MODES.get(language == null ? Language.ENGLISH : language);
    }
}
