package com.learn.learnE_Backend.vocabulary;

import jakarta.persistence.*;
import lombok.*;

import java.util.EnumSet;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseLevel level;

    /** Vocabulary courses run on days of words; grammar courses on lessons of patterns. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourseKind kind = CourseKind.VOCABULARY;

    @Column(nullable = false)
    private String title;

    private String description;

    /** Free-text level shown to the learner, e.g. "Boya Sơ cấp I" or "HSK 3". */
    @Column(name = "level_label", length = 60)
    private String levelLabel;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /**
     * Practice modes this course requires for mastery, overriding the language default.
     *
     * <p><b>Empty means "use the language default"</b> — not "no modes at all". The converter maps
     * a null column to an empty set and an empty set back to null, so the two cannot be told apart
     * here. That turns out to be the safe way round: a course with genuinely no required modes
     * would mark every word mastered on its first answer, because "all required modes correct" is
     * vacuously true for an empty set.
     *
     * <p>The radical course sets this because three of Chinese's seven modes ask the learner to
     * type or hear the character, and a radical like 氵 cannot be typed with a pinyin IME.
     */
    @Convert(converter = PracticeModeSetConverter.class)
    @Column(name = "practice_modes", length = 255)
    @Builder.Default
    private EnumSet<PracticeMode> practiceModes = EnumSet.noneOf(PracticeMode.class);

    /**
     * Whether this course offers sentence translation at all.
     *
     * <p>Narrowing only: the language still decides whether the feature exists, and this can turn
     * it off for one course. A course of radicals has nothing to translate.
     */
    @Column(name = "sentence_translation_enabled", nullable = false)
    @Builder.Default
    private boolean sentenceTranslationEnabled = true;

    /** Every day open from the start, with no "come back tomorrow" gate between them. */
    @Column(name = "days_always_unlocked", nullable = false)
    @Builder.Default
    private boolean daysAlwaysUnlocked = false;

    public Language getLanguage() {
        return topic.getLanguage();
    }
}
