package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.vocabulary.Language;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A grammar point learners keep getting wrong that has no lesson yet. */
@Entity
@Table(name = "grammar_gap_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrammarGapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(name = "grammar_name", nullable = false, length = 200)
    private String grammarName;

    /** Dedup key: the name lower-cased and stripped of diacritics and punctuation. */
    @Column(name = "normalized_name", nullable = false, length = 200)
    private String normalizedName;

    @Column(name = "example_prompt", columnDefinition = "TEXT")
    private String examplePrompt;

    @Column(name = "example_answer", columnDefinition = "TEXT")
    private String exampleAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "first_reported_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant firstReportedAt = Instant.now();

    @Column(name = "last_reported_at", nullable = false)
    @Builder.Default
    private Instant lastReportedAt = Instant.now();

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private int reportCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GrammarGapStatus status = GrammarGapStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_lesson_id")
    private GrammarLesson resolvedLesson;
}
