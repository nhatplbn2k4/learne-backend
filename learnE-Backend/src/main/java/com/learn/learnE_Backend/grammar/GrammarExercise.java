package com.learn.learnE_Backend.grammar;

import jakarta.persistence.*;
import lombok.*;

/** A Vietnamese prompt drilling one case of its lesson's pattern. */
@Entity
@Table(name = "grammar_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrammarExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grammar_lesson_id")
    private GrammarLesson lesson;

    @Column(name = "prompt_vi", nullable = false, columnDefinition = "TEXT")
    private String promptVi;

    /** The reference translation; the AI grades against meaning, not this string. */
    @Column(name = "answer_target", nullable = false, columnDefinition = "TEXT")
    private String answerTarget;

    @Column(name = "answer_phonetic", columnDefinition = "TEXT")
    private String answerPhonetic;

    /** JSON array of words the sentence needs but the learner may not have met yet. */
    @Column(name = "new_words_json", columnDefinition = "TEXT")
    private String newWordsJson;

    /** Which case of the pattern this drills, e.g. "phủ định" or "câu hỏi". */
    @Column(name = "case_label", length = 120)
    private String caseLabel;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
