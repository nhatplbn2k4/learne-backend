package com.learn.learnE_Backend.vocabulary;

import com.learn.learnE_Backend.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "user_course_enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "current_day_number", nullable = false)
    @Builder.Default
    private int currentDayNumber = 1;

    /**
     * Days studied back to back, as of {@link #lastStudyDate}.
     *
     * <p>Only recomputed when a day is finished, so on its own it says nothing about today: a
     * learner who stopped three weeks ago still carries the count they stopped on. Read it
     * through {@link #currentStreak(LocalDate)} rather than directly.
     */
    @Column(name = "streak_count", nullable = false)
    @Builder.Default
    private int streakCount = 0;

    /** The best run ever reached on this course. Only ever rises. */
    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private int longestStreak = 0;

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate;

    @Column(name = "started_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * The run still going today, or zero once it has lapsed.
     *
     * <p>A streak survives a single night: studied today means it is running, studied yesterday
     * means it is still alive and waiting for today. Anything older is over, however large the
     * stored count. Worked out on read because nothing runs at midnight to retire it.
     */
    public int currentStreak(LocalDate today) {
        if (lastStudyDate == null) {
            return 0;
        }
        boolean alive = lastStudyDate.equals(today) || lastStudyDate.equals(today.minusDays(1));
        return alive ? streakCount : 0;
    }

    /**
     * Highest day opened by passing a skip-ahead test. Every day up to and including this number is
     * unlocked regardless of the day-by-day rules, so the learner can also go back and study the
     * days they jumped over.
     */
    @Column(name = "skip_ahead_day_number", nullable = false)
    @Builder.Default
    private int skipAheadDayNumber = 0;
}
