package com.learn.learnE_Backend.skipahead;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.vocabulary.Course;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** One sitting of the "test out of the waiting period" exam for a chosen target day. */
@Entity
@Table(name = "skip_ahead_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkipAheadTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    /** The day that opens if this test is passed; every earlier day opens with it. */
    @Column(name = "target_day_number", nullable = false)
    private int targetDayNumber;

    @Column(name = "required_sentences", nullable = false)
    private int requiredSentences;

    @Column(name = "min_average", nullable = false, precision = 4, scale = 2)
    private BigDecimal minAverage;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    /** Null while the test is still open. */
    @Column(name = "finished_at")
    private Instant finishedAt;

    private Boolean passed;

    @Column(name = "average_score", precision = 4, scale = 2)
    private BigDecimal averageScore;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<SkipAheadTestItem> items = new ArrayList<>();

    public boolean isFinished() {
        return finishedAt != null;
    }
}
