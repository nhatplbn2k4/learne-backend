package com.learn.learnE_Backend.sentence.dto;

/**
 * Outcome of tagging a day's sentences.
 *
 * <p>{@code failedBatches} exists because "nothing matched" and "the AI call failed" both end with
 * nothing written, and an admin who cannot tell them apart will conclude the grammar simply does
 * not apply and never retry.
 */
public record TagGrammarResultDto(int tagged, int totalSentences, int failedBatches) {
}
