package com.learn.learnE_Backend.grammar.dto;

/**
 * One entry of the catalogue handed to the AI while grading, so it can name the lesson that covers
 * a mistake instead of inventing a label. Deliberately just the code and the title — the model only
 * has to choose from a list, never to reproduce the content.
 */
public record GrammarPointDto(String code, String title, String formula) {
}
