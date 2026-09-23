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

    @Column(name = "streak_count", nullable = false)
    @Builder.Default
    private int streakCount = 0;

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate;

    @Column(name = "started_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Highest day opened by passing a skip-ahead test. Every day up to and including this number is
     * unlocked regardless of the day-by-day rules, so the learner can also go back and study the
     * days they jumped over.
     */
    @Column(name = "skip_ahead_day_number", nullable = false)
    @Builder.Default
    private int skipAheadDayNumber = 0;
}
