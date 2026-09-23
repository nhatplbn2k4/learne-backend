package com.learn.learnE_Backend.reading;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reading_attempt_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    private ReadingAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private ReadingQuestion question;

    /** 'A', 'B', 'C' or 'D'. */
    @Column(name = "selected_option", nullable = false, length = 1)
    private String selectedOption;

    @Column(nullable = false)
    private boolean correct;
}
