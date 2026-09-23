package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * That this learner has studied this grammar point. Created by the first drill sentence they
 * submit, whatever it scores — the bar is having practised the pattern, not having got it right.
 */
@Entity
@Table(name = "user_grammar_lesson_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGrammarLessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grammar_lesson_id")
    private GrammarLesson lesson;

    @Column(name = "first_completed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant firstCompletedAt = Instant.now();

    @Column(name = "last_practised_at", nullable = false)
    @Builder.Default
    private Instant lastPractisedAt = Instant.now();

    /** Submissions, not distinct sentences — a redo counts again. Kept as a record of effort. */
    @Column(name = "exercises_done", nullable = false)
    @Builder.Default
    private int exercisesDone = 0;
}
