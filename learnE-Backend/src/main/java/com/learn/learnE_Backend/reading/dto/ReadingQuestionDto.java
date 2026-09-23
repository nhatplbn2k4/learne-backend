package com.learn.learnE_Backend.reading.dto;

import com.learn.learnE_Backend.reading.ReadingQuestion;

public record ReadingQuestionDto(
        Long id,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD
) {
    public static ReadingQuestionDto from(ReadingQuestion q) {
        return new ReadingQuestionDto(q.getId(), q.getQuestionText(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD());
    }
}
