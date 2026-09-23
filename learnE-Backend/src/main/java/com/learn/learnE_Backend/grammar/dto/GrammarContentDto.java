package com.learn.learnE_Backend.grammar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The two nested shapes stored as JSON on a lesson. */
public final class GrammarContentDto {

    private GrammarContentDto() {
    }

    /** One piece of the formula and what it stands for, e.g. ("在 + 地方", "trạng ngữ chỉ địa điểm"). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ComponentDto(String part, String meaning) {
    }

    /** A worked example straight from the slides. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExampleDto(String target, String phonetic, String vi, String note) {
    }
}
