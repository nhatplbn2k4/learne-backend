package com.learn.learnE_Backend.grammar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads a lesson's formula well enough to ask "does this sentence actually use the pattern?".
 *
 * <p>Needed because asking the model to quote its evidence was not enough on its own: for
 * "太…了" it quoted "快了" out of 打电话就快了, two characters that really are in the sentence, and
 * the tag stuck even though the sentence uses 就, not 太. Checking the sentence against the
 * pattern's own characters is what catches that.
 *
 * <p>The formulas are written in a small, consistent notation:
 * <ul>
 *   <li>{@code +} joins slots that must all appear — "太 + tính từ + 了"</li>
 *   <li>{@code /} with spaces around it separates whole alternative patterns —
 *       "S + V + O + 了 / phủ định là S + 没 + V + O"</li>
 *   <li>{@code /} without spaces offers two words for one slot — "这/那 + lượng từ"</li>
 *   <li>a newline also separates whole alternatives — the numbered ways of telling the time</li>
 *   <li>{@code ( )} marks an optional variant, which cannot be required — "V + V (hoặc V 一 V)"</li>
 * </ul>
 *
 * <p>Deliberately conservative: a pattern whose formula names no Chinese characters is exempt, and
 * a tag is only dropped when the sentence clearly lacks the characters the formula demands. The
 * cost of being wrong here is a warning the learner does not see, against a warning that sends
 * them to the wrong lesson.
 */
public final class GrammarFormula {

    private static final Pattern PARENTHESISED = Pattern.compile("[（(][^）)]*[）)]");
    private static final Pattern PATTERN_SEPARATOR = Pattern.compile("\\R|\\s+/\\s+");
    private static final Pattern SLOT_SEPARATOR = Pattern.compile("[+＋]");
    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");

    private GrammarFormula() {
    }

    /**
     * Whether the sentence uses the pattern this formula describes.
     *
     * <p>Only for deciding that a sentence <em>demonstrates</em> a pattern. It must not be used on
     * a learner's answer while grading: there the missing character is often the mistake itself,
     * and the lesson being pointed at is exactly the one they needed.
     */
    public static boolean matches(String sentence, String formula) {
        if (sentence == null) {
            return false;
        }
        List<List<Set<Character>>> alternatives = requiredCharacters(formula);
        if (alternatives.isEmpty()) {
            return true; // nothing in the formula to check against
        }
        for (List<Set<Character>> slots : alternatives) {
            if (satisfies(sentence, slots)) {
                return true;
            }
        }
        return false;
    }

    private static boolean satisfies(String sentence, List<Set<Character>> slots) {
        for (Set<Character> slot : slots) {
            boolean present = false;
            for (char option : slot) {
                if (sentence.indexOf(option) >= 0) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                return false;
            }
        }
        return true;
    }

    /** One entry per alternative pattern; each is the list of slots that name Chinese characters. */
    static List<List<Set<Character>>> requiredCharacters(String formula) {
        List<List<Set<Character>>> alternatives = new ArrayList<>();
        if (formula == null || formula.isBlank()) {
            return alternatives;
        }
        // Brackets come out first: a "/" inside one belongs to the optional variant, and splitting
        // on it would turn "(hoặc V 一 V / V 了 V)" into two patterns that demand 一 and 了.
        String withoutVariants = PARENTHESISED.matcher(formula).replaceAll("");
        for (String alternative : PATTERN_SEPARATOR.split(withoutVariants)) {
            List<Set<Character>> slots = new ArrayList<>();
            for (String slot : SLOT_SEPARATOR.split(alternative)) {
                Set<Character> chars = hanCharacters(slot);
                if (!chars.isEmpty()) {
                    slots.add(chars);
                }
            }
            if (!slots.isEmpty()) {
                alternatives.add(slots);
            }
        }
        return alternatives;
    }

    private static Set<Character> hanCharacters(String text) {
        Set<Character> chars = new LinkedHashSet<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (HAN.matcher(String.valueOf(c)).matches()) {
                chars.add(c);
            }
        }
        return chars;
    }
}
