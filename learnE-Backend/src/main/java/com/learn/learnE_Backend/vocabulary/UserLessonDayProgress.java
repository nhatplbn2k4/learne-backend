package com.learn.learnE_Backend.vocabulary;

import com.learn.learnE_Backend.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_lesson_day_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_day_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLessonDayProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_day_id")
    private LessonDay lessonDay;

    @Column(name = "completed_at", nullable = false)
    @Builder.Default
    private Instant completedAt = Instant.now();
}
