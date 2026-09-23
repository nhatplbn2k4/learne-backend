package com.learn.learnE_Backend.grammar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Formulas and sentences below are the real ones from the Boya lessons. */
class GrammarFormulaTest {

    private static final String TAI_LE = "太 + tính từ + 了";
    private static final String PLACE_ADVERBIAL = "Chủ ngữ + 在 + địa điểm + V";
    private static final String MA_QUESTION = "Câu trần thuật + 吗？";

    /** The sentence that started this: tagged 太…了 although it uses 就. */
    @Test
    void rejectsAPatternWhoseCharactersAreMissing() {
        assertThat(GrammarFormula.matches("打电话就快了。", TAI_LE)).isFalse();
        assertThat(GrammarFormula.matches("周末你去哪儿玩儿？", MA_QUESTION)).isFalse();
        assertThat(GrammarFormula.matches("老师九点去图书馆。", PLACE_ADVERBIAL)).isFalse();
    }

    @Test
    void acceptsASentenceThatReallyUsesThePattern() {
        assertThat(GrammarFormula.matches("今天太冷了。", TAI_LE)).isTrue();
        assertThat(GrammarFormula.matches("我在宿舍看书。", PLACE_ADVERBIAL)).isTrue();
        assertThat(GrammarFormula.matches("你是学生吗？", MA_QUESTION)).isTrue();
    }

    /** "/" without spaces offers two words for one slot, so either one satisfies it. */
    @Test
    void aSlashInsideASlotMeansEitherWord() {
        String formula = "这/那 + lượng từ + danh từ";
        assertThat(GrammarFormula.matches("这瓶啤酒多少钱？", formula)).isTrue();
        assertThat(GrammarFormula.matches("那是什么书？", formula)).isTrue();
        assertThat(GrammarFormula.matches("我买了啤酒。", formula)).isFalse();
    }

    /** " / " with spaces separates two whole patterns; satisfying either is enough. */
    @Test
    void aSpacedSlashSeparatesWholePatterns() {
        String formula = "S + V + O + 了 / Hình thức phủ định là S + 没 + V + O";
        assertThat(GrammarFormula.matches("老师来了。", formula)).isTrue();
        assertThat(GrammarFormula.matches("他没来。", formula)).isTrue();
        assertThat(GrammarFormula.matches("他来。", formula)).isFalse();
    }

    /** Numbered lines are alternatives too — the ways of telling the time. */
    @Test
    void eachLineIsItsOwnAlternative() {
        String formula = """
                1. Giờ đúng:  Số giờ + 点
                2. Giờ hơn:  Số giờ + 点 + số phút + 分
                3. Giờ rưỡi:  Số giờ + 点 + 半
                4. Hỏi giờ:  几 + 点""";
        assertThat(GrammarFormula.matches("现在十点半。", formula)).isTrue();
        assertThat(GrammarFormula.matches("现在八点五十分。", formula)).isTrue();
        assertThat(GrammarFormula.matches("现在几点？", formula)).isTrue();
        assertThat(GrammarFormula.matches("我们上课。", formula)).isFalse();
    }

    /** A bracketed variant is optional, so it can never be the thing that is required. */
    @Test
    void parenthesisedVariantsAreNotRequired() {
        String formula = "V + V  (hoặc V 一 V / V 了 V)";
        // Nothing outside the brackets names a character, so every sentence passes.
        assertThat(GrammarFormula.matches("周末我在家洗洗衣服。", formula)).isTrue();
        assertThat(GrammarFormula.matches("我去学校。", formula)).isTrue();
    }

    /** A pattern described only in Vietnamese cannot be checked, so it is left alone. */
    @Test
    void aFormulaWithoutChineseIsExempt() {
        assertThat(GrammarFormula.matches("我去学校。", "Chủ ngữ + Từ nghi vấn + ...")).isTrue();
        assertThat(GrammarFormula.matches("我去学校。", "")).isTrue();
        assertThat(GrammarFormula.matches("我去学校。", null)).isTrue();
    }

    @Test
    void aNullSentenceMatchesNothing() {
        assertThat(GrammarFormula.matches(null, TAI_LE)).isFalse();
    }
}
