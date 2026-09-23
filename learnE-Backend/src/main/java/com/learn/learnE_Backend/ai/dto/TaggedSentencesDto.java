package com.learn.learnE_Backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Which grammar points each sentence of a batch relies on, one entry per sentence. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaggedSentencesDto(List<TaggedSentenceDto> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TaggedSentenceDto(
            /** 1-based position, so a shuffled or partial reply can still be matched up. */
            Integer index,
            List<TagDto> grammar
    ) {
    }

    /**
     * One tag, with the words in the sentence that prove it.
     *
     * <p>The evidence is the whole point: asked only for codes, the model tags "了" anywhere as
     * 太…了 and any sentence mentioning a place as a place adverbial. Made to quote the text, it
     * either finds the pattern or produces a fragment the server can reject.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TagDto(String code, String evidence) {
    }
}
