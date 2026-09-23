package com.learn.learnE_Backend.vocabulary;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sm2CalculatorTest {

    @Test
    void firstReviewWithGoodQualityGivesOneDayInterval() {
        Sm2Calculator.Result result = Sm2Calculator.compute(2.5, 0, 0, ReviewQuality.GOOD.score());

        assertThat(result.repetitions()).isEqualTo(1);
        assertThat(result.intervalDays()).isEqualTo(1);
    }

    @Test
    void secondReviewWithGoodQualityGivesSixDayInterval() {
        Sm2Calculator.Result first = Sm2Calculator.compute(2.5, 0, 0, ReviewQuality.GOOD.score());
        Sm2Calculator.Result second = Sm2Calculator.compute(first.easeFactor(), first.intervalDays(), first.repetitions(), ReviewQuality.GOOD.score());

        assertThat(second.repetitions()).isEqualTo(2);
        assertThat(second.intervalDays()).isEqualTo(6);
    }

    @Test
    void thirdReviewIntervalGrowsByEaseFactor() {
        Sm2Calculator.Result r1 = Sm2Calculator.compute(2.5, 0, 0, ReviewQuality.GOOD.score());
        Sm2Calculator.Result r2 = Sm2Calculator.compute(r1.easeFactor(), r1.intervalDays(), r1.repetitions(), ReviewQuality.GOOD.score());
        Sm2Calculator.Result r3 = Sm2Calculator.compute(r2.easeFactor(), r2.intervalDays(), r2.repetitions(), ReviewQuality.GOOD.score());

        assertThat(r3.repetitions()).isEqualTo(3);
        assertThat(r3.intervalDays()).isEqualTo((int) Math.round(6 * r2.easeFactor()));
    }

    @Test
    void againResetsRepetitionsAndIntervalToOneDay() {
        Sm2Calculator.Result r1 = Sm2Calculator.compute(2.5, 0, 0, ReviewQuality.GOOD.score());
        Sm2Calculator.Result r2 = Sm2Calculator.compute(r1.easeFactor(), r1.intervalDays(), r1.repetitions(), ReviewQuality.GOOD.score());
        Sm2Calculator.Result afterAgain = Sm2Calculator.compute(r2.easeFactor(), r2.intervalDays(), r2.repetitions(), ReviewQuality.AGAIN.score());

        assertThat(afterAgain.repetitions()).isEqualTo(0);
        assertThat(afterAgain.intervalDays()).isEqualTo(1);
    }

    @Test
    void easeFactorNeverDropsBelowMinimum() {
        double ease = 1.3;
        int interval = 1;
        int repetitions = 1;

        for (int i = 0; i < 20; i++) {
            Sm2Calculator.Result result = Sm2Calculator.compute(ease, interval, repetitions, ReviewQuality.AGAIN.score());
            ease = result.easeFactor();
            interval = result.intervalDays();
            repetitions = result.repetitions();
            assertThat(ease).isGreaterThanOrEqualTo(1.3);
        }
    }

    @Test
    void easyQualityGrowsEaseFactor() {
        Sm2Calculator.Result result = Sm2Calculator.compute(2.5, 0, 0, ReviewQuality.EASY.score());
        assertThat(result.easeFactor()).isGreaterThan(2.5);
    }
}
