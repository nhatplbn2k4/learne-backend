package com.learn.learnE_Backend.sentence.dto;

/**
 * A word the sentence needs but the learner has not studied yet. The rule agreed for the course is
 * that such words are allowed only when annotated, so all three fields are shown as a hint.
 */
public record NewWordHintDto(
        String term,
        String phonetic,
        String meaning
) {
}
