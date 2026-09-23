package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** One submitted answer to a grammar drill, kept so earlier tries stay visible beside the feedback. */
@Entity
@Table(name = "user_grammar_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGrammarAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private GrammarExercise exercise;

    @Column(name = "answer_target", nullable = false, columnDefinition = "TEXT")
    private String answerTarget;

    /** 0-10, or null when the AI was unavailable. */
    private Integer score;

    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private Instant submittedAt = Instant.now();
}
