package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.vocabulary.Course;
import com.learn.learnE_Backend.vocabulary.Language;
import jakarta.persistence.*;
import lombok.*;

/** One grammar pattern: its formula, what each part means, worked examples and drill sentences. */
@Entity
@Table(name = "grammar_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrammarLesson {

    /** Drill sentences per difficulty level, 1..5 — the "5 đến 25 câu" the learner asked for. */
    private static final int EXERCISES_PER_DIFFICULTY = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    /**
     * Stable handle the AI quotes when it spots this mistake while grading, e.g. "de-attributive".
     * Unique across every course, so one grammar point always resolves to one lesson.
     */
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String formula;

    /** JSON array of {part, meaning}: what each piece of the formula stands for. */
    @Column(name = "components_json", columnDefinition = "TEXT")
    private String componentsJson;

    /** JSON array of {target, phonetic, vi, note}. */
    @Column(name = "examples_json", columnDefinition = "TEXT")
    private String examplesJson;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Which textbook lesson this came from, matching {@code LessonDay.sourceLessonLabel}. */
    @Column(name = "source_lesson_label", length = 60)
    private String sourceLessonLabel;

    @Column(nullable = false)
    @Builder.Default
    private int difficulty = 1;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    public Language getLanguage() {
        return course.getLanguage();
    }

    /** How many drill sentences this lesson is worth: 5 for the simplest, 25 for the hardest. */
    public int targetExerciseCount() {
        return Math.clamp(difficulty, 1, 5) * EXERCISES_PER_DIFFICULTY;
    }
}
