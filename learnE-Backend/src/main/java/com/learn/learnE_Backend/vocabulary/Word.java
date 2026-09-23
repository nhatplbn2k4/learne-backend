package com.learn.learnE_Backend.vocabulary;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "words")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_day_id")
    private LessonDay lessonDay;

    /** The word itself in the target language: "book" or "学生". */
    @Column(nullable = false, length = 200)
    private String term;

    /** IPA for English, tone-marked pinyin for Chinese. */
    @Column(length = 200)
    private String phonetic;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    @Column(name = "vietnamese_meaning", nullable = false)
    private String vietnameseMeaning;

    /** Sino-Vietnamese reading, Chinese only (学生 → "học sinh"). */
    @Column(name = "han_viet", length = 200)
    private String hanViet;

    /** Grammar/usage note shown on the result screen. */
    @Column(name = "usage_note", columnDefinition = "TEXT")
    private String usageNote;

    @Column(name = "example_sentence_target")
    private String exampleSentenceTarget;

    @Column(name = "example_sentence_vi")
    private String exampleSentenceVi;

    public Language getLanguage() {
        return lessonDay.getCourse().getLanguage();
    }
}
