package com.learn.learnE_Backend.vocabulary;

import com.learn.learnE_Backend.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;

@Entity
@Table(name = "user_word_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "word_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWordProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id")
    private Word word;

    @Column(name = "ease_factor", nullable = false)
    @Builder.Default
    private double easeFactor = 2.5;

    @Column(name = "interval_days", nullable = false)
    @Builder.Default
    private int intervalDays = 0;

    @Column(nullable = false)
    @Builder.Default
    private int repetitions = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    /** Modes answered correctly so far within {@link #masterySessionId}. */
    @Convert(converter = PracticeModeSetConverter.class)
    @Column(name = "session_correct_modes", length = 255)
    @Builder.Default
    private EnumSet<PracticeMode> sessionCorrectModes = EnumSet.noneOf(PracticeMode.class);

    /** Earned once every mode required by the word's language was aced in the same session. */
    @Column(nullable = false)
    @Builder.Default
    private boolean mastered = false;

    /** Which session the ticks in {@link #sessionCorrectModes} belong to. */
    @Column(name = "mastery_session_id", length = 64)
    private String masterySessionId;

    /** Ticks only count within one session, so a new session starts from scratch. */
    public void startSession(String sessionId) {
        masterySessionId = sessionId;
        sessionCorrectModes = EnumSet.noneOf(PracticeMode.class);
    }

    public boolean isAllModesCorrectThisSession(Collection<PracticeMode> requiredModes) {
        return sessionCorrectModes.containsAll(requiredModes);
    }

    public void setModeMastered(PracticeMode mode, boolean correct) {
        if (sessionCorrectModes == null) {
            sessionCorrectModes = EnumSet.noneOf(PracticeMode.class);
        }
        if (correct) {
            sessionCorrectModes.add(mode);
        } else {
            sessionCorrectModes.remove(mode);
        }
    }

    public boolean isModeMastered(PracticeMode mode) {
        return sessionCorrectModes != null && sessionCorrectModes.contains(mode);
    }

    /** Fully mastered = aced every required practice mode within one session. */
    public boolean isFullyMastered() {
        return mastered;
    }
}
