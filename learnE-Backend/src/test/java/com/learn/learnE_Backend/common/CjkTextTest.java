package com.learn.learnE_Backend.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CjkTextTest {

    @Test
    void removesSpacesBetweenChineseCharacters() {
        assertThat(CjkText.tidy("周末你 怎么 去 玩儿 啊？")).isEqualTo("周末你怎么去玩儿啊？");
        assertThat(CjkText.tidy("坐公共汽车 和 走路 都 很 快。")).isEqualTo("坐公共汽车和走路都很快。");
    }

    /** The exporter breaks a sentence across lines, not just at spaces. */
    @Test
    void removesLineBreaksBetweenChineseCharacters() {
        assertThat(CjkText.tidy("今天\n太冷了。")).isEqualTo("今天太冷了。");
    }

    @Test
    void spacesAroundChinesePunctuationGoToo() {
        assertThat(CjkText.tidy("不过 ，从这儿 到 图书馆 几 分钟 ？")).isEqualTo("不过，从这儿到图书馆几分钟？");
    }

    /** Pinyin and Vietnamese are ordinary spaced text and must survive untouched. */
    @Test
    void leavesLatinTextAlone() {
        assertThat(CjkText.tidy("Jīntiān tài lěng le.")).isEqualTo("Jīntiān tài lěng le.");
        assertThat(CjkText.tidy("Hôm nay lạnh quá.")).isEqualTo("Hôm nay lạnh quá.");
    }

    /** A space between a Chinese character and a Latin one is a real separator. */
    @Test
    void keepsTheSpaceWhereChineseMeetsLatin() {
        assertThat(CjkText.tidy("我说 hello")).isEqualTo("我说 hello");
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(CjkText.tidy(null)).isNull();
        assertThat(CjkText.tidy("  ")).isEmpty();
    }
}
