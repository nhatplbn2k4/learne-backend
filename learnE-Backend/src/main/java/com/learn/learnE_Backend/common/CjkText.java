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

    /**
     * Whether the text contains at least one Han character.
     *
     * <p>Cheap guard against content that is Chinese in name only. Ten grammar lessons shipped
     * with worked examples whose "Chinese" line was the Vietnamese prompt, under invented pinyin,
     * because the slide they were imported from was an exercise sheet with no model answers at
     * all. A generated drill once came back answered in English prose. Neither survives this
     * check, and unlike an instruction in a prompt it cannot be ignored by the model.
     *
     * <p>Written against the Unicode script rather than a regex on purpose: the pattern would be
     * {@code "\\p{IsHan}"}, and a single backslash there compiles to a different thing entirely.
     * That exact slip has already cost this project an outage.
     */
    public static boolean hasHan(String text) {
        return text != null && text.codePoints()
                .anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    /** Removes spaces between Chinese characters, leaving other text untouched. */
    public static String tidy(String text) {
        if (text == null) {
            return null;
        }
        return SPACE_INSIDE_CJK.matcher(text).replaceAll("").trim();
    }
}
