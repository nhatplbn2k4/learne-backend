package com.learn.learnE_Backend.common;

import java.util.regex.Pattern;

/**
 * Cleanup for Chinese text that arrives from outside the app.
 *
 * <p>Two sources leave stray spaces between characters: the slide PDFs, whose exporter inserts
 * them at line-layout boundaries, and the AI, which picks up the habit from text like that. Either
 * way "周末你 怎么 去 玩儿 啊？" is wrong — Chinese has no word spacing — and it ends up in the
 * sentence the learner is asked to match.
 */
public final class CjkText {

    /** A Han character or Chinese punctuation: the two things a stray space can sit between. */
    private static final String CJK = "[\\p{IsHan}。，、？！：；]";
    private static final Pattern SPACE_INSIDE_CJK =
            Pattern.compile("(?<=" + CJK + ")\\s+(?=" + CJK + ")");

    private CjkText() {
    }

    /** Removes spaces between Chinese characters, leaving other text untouched. */
    public static String tidy(String text) {
        if (text == null) {
            return null;
        }
        return SPACE_INSIDE_CJK.matcher(text).replaceAll("").trim();
    }
}
