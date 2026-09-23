package com.learn.learnE_Backend.skipahead.dto;

import com.learn.learnE_Backend.sentence.dto.SentenceFeedbackDto;

/**
 * One question. While the test is open only {@code promptVi} and {@code answered} are filled in —
 * scores, feedback and the reference answer arrive once the whole paper is handed in.
 */
public record SkipAheadItemDto(
        Long id,
        int sortOrder,
        String promptVi,
        boolean answered,
        String answerSubmitted,
        Integer score,
        SentenceFeedbackDto feedback,
        String answerTarget,
        String answerPhonetic
) {
}
