package com.learn.learnE_Backend.skipahead;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A frozen copy of one question. Copying rather than referencing the practice sentence means an
 * admin editing or deleting content cannot change a test that is under way or already sat.
 */
@Entity
@Table(name = "skip_ahead_test_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkipAheadTestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id")
    private SkipAheadTest test;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "prompt_vi", nullable = false, columnDefinition = "TEXT")
    private String promptVi;

    @Column(name = "answer_target", nullable = false, columnDefinition = "TEXT")
    private String answerTarget;

    @Column(name = "answer_phonetic", columnDefinition = "TEXT")
    private String answerPhonetic;

    /** The practice sentence this was taken from; null when the AI generated it for this test. */
    @Column(name = "source_exercise_id")
    private Long sourceExerciseId;

    @Column(name = "answer_submitted", columnDefinition = "TEXT")
    private String answerSubmitted;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    private Integer score;

    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson;

    public boolean isAnswered() {
        return submittedAt != null;
    }
}
