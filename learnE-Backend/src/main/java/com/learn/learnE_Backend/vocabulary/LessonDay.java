package com.learn.learnE_Backend.vocabulary;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lesson_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    private String title;

    /** Which textbook lesson this day was split out of, e.g. "Bài 1". */
    @Column(name = "source_lesson_label", length = 60)
    private String sourceLessonLabel;

    /** 1-based part number within that lesson: "Bài 1 — phần 2/3". */
    @Column(name = "part_index")
    private Integer partIndex;
}
