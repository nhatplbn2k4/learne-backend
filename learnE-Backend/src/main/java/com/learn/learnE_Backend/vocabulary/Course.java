package com.learn.learnE_Backend.vocabulary;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseLevel level;

    /** Vocabulary courses run on days of words; grammar courses on lessons of patterns. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourseKind kind = CourseKind.VOCABULARY;

    @Column(nullable = false)
    private String title;

    private String description;

    /** Free-text level shown to the learner, e.g. "Boya Sơ cấp I" or "HSK 3". */
    @Column(name = "level_label", length = 60)
    private String levelLabel;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    public Language getLanguage() {
        return topic.getLanguage();
    }
}
