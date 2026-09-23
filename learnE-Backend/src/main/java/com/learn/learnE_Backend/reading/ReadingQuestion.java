package com.learn.learnE_Backend.reading;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reading_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passage_id")
    private ReadingPassage passage;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "option_a", nullable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false)
    private String optionB;

    @Column(name = "option_c", nullable = false)
    private String optionC;

    @Column(name = "option_d", nullable = false)
    private String optionD;

    /** 'A', 'B', 'C' or 'D'. */
    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption;

    private String explanation;
}
