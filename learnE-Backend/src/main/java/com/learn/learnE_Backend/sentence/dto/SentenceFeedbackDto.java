package com.learn.learnE_Backend.sentence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.learn.learnE_Backend.grammar.dto.GrammarRefDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SentenceFeedbackDto(
        Integer score,
        /** Short verdict in Vietnamese. */
        String comment,
        /** Grammar/word-choice problems found in the answer. */
        List<SentenceCorrectionDto> corrections,
        /** A more idiomatic way to say the same thing, when the answer was already acceptable. */
        String betterVersion,
        String betterVersionPhonetic
) {
    public static SentenceFeedbackDto unavailable(String message) {
        return new SentenceFeedbackDto(null, message, List.of(), null, null);
    }

    /** Same feedback with a rewritten correction list, used once grammar codes are resolved. */
    public SentenceFeedbackDto withCorrections(List<SentenceCorrectionDto> resolved) {
        return new SentenceFeedbackDto(score, comment, resolved, betterVersion, betterVersionPhonetic);
    }

    /**
     * One problem found in the answer. When it is a grammar mistake the AI also names the point,
     * which the server turns into {@code grammar} by looking the code up — an old feedback_json
     * simply has all three null, which parses fine thanks to {@code ignoreUnknown}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SentenceCorrectionDto(
            String original,
            String suggestion,
            String explanation,
            /** Code the AI picked from the catalogue; blank when nothing matched. */
            String grammarCode,
            /** The point in plain Vietnamese, filled even when no lesson covers it yet. */
            String grammarName,
            /** Resolved by the server against the database; null when no lesson teaches this. */
            GrammarRefDto grammar
    ) {
        /** Same correction with the lesson pointer attached. */
        public SentenceCorrectionDto withGrammar(GrammarRefDto resolved) {
            return new SentenceCorrectionDto(
                    original, suggestion, explanation, grammarCode, grammarName, resolved);
        }

        /**
         * Same correction with every grammar field cleared — for when the model labelled a plain
         * word-choice slip as grammar. The correction itself still stands; only the label goes.
         */
        public SentenceCorrectionDto withoutGrammar() {
            return new SentenceCorrectionDto(original, suggestion, explanation, null, null, null);
        }
    }
}
