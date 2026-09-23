package com.learn.learnE_Backend.reading.dto;

import com.learn.learnE_Backend.reading.ReadingQuestion;

public record AdminReadingQuestionDto(
        Long id,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String explanation
) {
    public static AdminReadingQuestionDto from(ReadingQuestion q) {
        return new AdminReadingQuestionDto(
                q.getId(), q.getQuestionText(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getCorrectOption(), q.getExplanation()
        );
    }
}
