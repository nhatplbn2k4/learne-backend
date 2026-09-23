package com.learn.learnE_Backend.sentence;

import com.learn.learnE_Backend.vocabulary.LessonDay;
import jakarta.persistence.*;
import lombok.*;

/** One Vietnamese prompt to be translated into the course's target language. */
@Entity
@Table(name = "sentence_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentenceExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_day_id")
    private LessonDay lessonDay;

    @Column(name = "prompt_vi", nullable = false, columnDefinition = "TEXT")
    private String promptVi;

    /** The reference translation; the AI grades against meaning, not this string. */
    @Column(name = "answer_target", nullable = false, columnDefinition = "TEXT")
    private String answerTarget;

    @Column(name = "answer_phonetic", columnDefinition = "TEXT")
    private String answerPhonetic;

    /** JSON array of words the sentence needs but the learner has not studied yet. */
    @Column(name = "new_words_json", columnDefinition = "TEXT")
    private String newWordsJson;

    /** JSON array of grammar_lessons.code this sentence relies on; null when never tagged. */
    @Column(name = "grammar_codes", columnDefinition = "TEXT")
    private String grammarCodes;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
