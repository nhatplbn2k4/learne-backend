package com.learn.learnE_Backend.grammar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The AI names the same grammar point a slightly different way every time it is asked. Folding
 * those spellings onto one key is the only thing stopping the admin's list filling with duplicates
 * of a single missing lesson.
 */
class GrammarGapNormalizeTest {

    @Test
    void diacriticsAndCaseAreIgnored() {
        assertThat(GrammarGapService.normalize("Trợ từ kết cấu"))
                .isEqualTo(GrammarGapService.normalize("TRO TU KET CAU"));
    }

    @Test
    void punctuationAndSpacingAreIgnored() {
        assertThat(GrammarGapService.normalize("Bổ ngữ  xu hướng!"))
                .isEqualTo(GrammarGapService.normalize("Bổ ngữ, xu hướng"));
    }

    /** The Chinese characters are what actually identify the pattern, so they must survive. */
    @Test
    void chineseCharactersSurvive() {
        assertThat(GrammarGapService.normalize("Trợ từ 的")).contains("的");
        assertThat(GrammarGapService.normalize("Trợ từ '的'"))
                .isEqualTo(GrammarGapService.normalize("trợ từ 的"));
    }

    /** Đ is not a plain D under NFD, so it needs its own rule. */
    @Test
    void vietnameseDStrokeIsFolded() {
        assertThat(GrammarGapService.normalize("Đại từ nghi vấn")).isEqualTo("dai tu nghi van");
    }

    @Test
    void differentPointsStayDifferent() {
        assertThat(GrammarGapService.normalize("Trợ từ 了"))
                .isNotEqualTo(GrammarGapService.normalize("Trợ từ 的"));
    }

    /**
     * The model sometimes answers with the KIND of mistake rather than the name of a pattern.
     * Those must not reach the admin's list: no lesson could ever close them.
     */
    @Test
    void mistakeKindsAreNotGrammarPoints() {
        for (String name : new String[]{
                "Lỗi dịch sai nghĩa", "Sai từ vựng", "Thiếu chủ ngữ", "Thừa từ",
                "Chính tả", "Diễn đạt chưa tự nhiên", "Từ vựng",
                // The telling word can sit anywhere, which a prefix rule missed.
                "Dùng từ sai", "Dùng sai từ", "Dịch sai nghĩa của từ", "Câu chưa tự nhiên"}) {
            assertThat(GrammarGapService.describesAMistakeKind(GrammarGapService.normalize(name)))
                    .as(name)
                    .isTrue();
        }
    }

    @Test
    void realGrammarPointsGetThrough() {
        for (String name : new String[]{
                "Trợ từ 了", "Câu chữ 把", "Bổ ngữ kết quả", "Trạng ngữ chỉ địa điểm",
                "Động từ năng nguyện", "Đại từ nghi vấn", "太…了",
                "Câu so sánh với 比", "Bổ ngữ khả năng", "Động từ li hợp", "Câu chữ 被"}) {
            assertThat(GrammarGapService.describesAMistakeKind(GrammarGapService.normalize(name)))
                    .as(name)
                    .isFalse();
        }
    }

    @Test
    void emptyAndNullCollapseToNothing() {
        assertThat(GrammarGapService.normalize(null)).isEmpty();
        assertThat(GrammarGapService.normalize("   ")).isEmpty();
        assertThat(GrammarGapService.normalize("!!!")).isEmpty();
    }
}
