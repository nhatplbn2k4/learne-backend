package com.learn.learnE_Backend.vocabulary;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A course narrowing its practice modes, and the defaults it falls back to.
 *
 * <p>The radical course needs this: three of Chinese's seven modes ask the learner to type or
 * hear the character, which cannot be done with 氵 or 宀. The danger is asking fewer questions
 * than mastery checks for — then a word can never be mastered, sentence translation never opens
 * and the day is flagged as needing review forever. So the list has to come from one place.
 */
class CourseModesTest {

    private static final EnumSet<PracticeMode> RADICAL_MODES = EnumSet.of(
            PracticeMode.FLASHCARD,
            PracticeMode.MULTIPLE_CHOICE_VI,
            PracticeMode.MULTIPLE_CHOICE_TERM,
            PracticeMode.PINYIN_TONE);

    private static Course course(Language language, EnumSet<PracticeMode> modes) {
        Topic topic = Topic.builder().slug("t").name("T").language(language).build();
        return Course.builder()
                .topic(topic)
                .level(CourseLevel.BEGINNER)
                .title("C")
                .practiceModes(modes)
                .build();
    }

    // ---- language defaults: this is what protects the courses already running ----

    @Test
    void chineseRequiresSevenModes() {
        assertThat(LanguageModes.requiredFor(Language.CHINESE)).hasSize(7);
    }

    @Test
    void englishRequiresFiveModes() {
        assertThat(LanguageModes.requiredFor(Language.ENGLISH))
                .hasSize(5)
                .doesNotContain(PracticeMode.PINYIN_TONE, PracticeMode.LISTEN_CHOICE);
    }

    // ---- per-course override ----

    @Test
    void emptyModesFallBackToTheLanguageDefault() {
        Course course = course(Language.CHINESE, EnumSet.noneOf(PracticeMode.class));

        assertThat(LanguageModes.requiredFor(course))
                .isEqualTo(LanguageModes.requiredFor(Language.CHINESE));
    }

    /** The converter hands back an empty set for a null column, but a builder can still leave null. */
    @Test
    void nullModesFallBackToTheLanguageDefault() {
        Course course = course(Language.CHINESE, null);

        assertThat(LanguageModes.requiredFor(course)).hasSize(7);
    }

    @Test
    void aCourseWithItsOwnModesDropsTheRest() {
        Course course = course(Language.CHINESE, RADICAL_MODES);

        assertThat(LanguageModes.requiredFor(course))
                .containsExactlyInAnyOrderElementsOf(RADICAL_MODES)
                .doesNotContain(
                        PracticeMode.TRANSLATE_TYPE,
                        PracticeMode.LISTEN_TYPE,
                        PracticeMode.LISTEN_CHOICE);
    }

    /** Nothing here is Chinese-specific — the override beats the default whatever the language. */
    @Test
    void theOverrideBeatsTheLanguageForEnglishToo() {
        Course course = course(Language.ENGLISH, EnumSet.of(PracticeMode.FLASHCARD));

        assertThat(LanguageModes.requiredFor(course)).containsExactly(PracticeMode.FLASHCARD);
    }

    // ---- builder defaults: leaving these out must not change how a course behaves ----

    @Test
    void aCourseBuiltWithoutSettingsKeepsTheOldBehaviour() {
        Topic topic = Topic.builder().slug("t").name("T").language(Language.CHINESE).build();
        Course course = Course.builder()
                .topic(topic)
                .level(CourseLevel.BEGINNER)
                .title("C")
                .build();

        assertThat(course.getPracticeModes()).isEmpty();
        assertThat(course.isSentenceTranslationEnabled()).isTrue();
        assertThat(course.isDaysAlwaysUnlocked()).isFalse();
        assertThat(LanguageModes.requiredFor(course)).hasSize(7);
    }

    // ---- the trap this whole design exists to avoid ----

    /**
     * Four correct answers satisfy a four-mode course and fail a seven-mode one. If the question
     * list and the mastery check ever read from different places, this is the shape of the bug:
     * the learner answers everything asked and the word still never counts as learnt.
     */
    @Test
    void ticksForFourModesDoNotSatisfyTheSevenModeDefault() {
        UserWordProgress progress = new UserWordProgress();
        progress.startSession("s1");
        RADICAL_MODES.forEach(mode -> progress.setModeMastered(mode, true));

        assertThat(progress.isAllModesCorrectThisSession(List.copyOf(RADICAL_MODES))).isTrue();
        assertThat(progress.isAllModesCorrectThisSession(LanguageModes.requiredFor(Language.CHINESE)))
                .isFalse();
    }
}
