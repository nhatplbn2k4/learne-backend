package com.learn.learnE_Backend.vocabulary;

/**
 * Classic SM-2 (SuperMemo 2) spaced-repetition scheduling algorithm.
 */
public final class Sm2Calculator {

    private static final double MIN_EASE_FACTOR = 1.3;

    private Sm2Calculator() {
    }

    public record Result(double easeFactor, int intervalDays, int repetitions) {
    }

    /**
     * @param quality SM-2 quality score, 0-5. Below 3 resets the repetition streak.
     */
    public static Result compute(double easeFactor, int intervalDays, int repetitions, int quality) {
        double newEaseFactor = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        newEaseFactor = Math.max(newEaseFactor, MIN_EASE_FACTOR);

        int newRepetitions;
        int newInterval;
        if (quality < 3) {
            newRepetitions = 0;
            newInterval = 1;
        } else {
            newRepetitions = repetitions + 1;
            if (newRepetitions == 1) {
                newInterval = 1;
            } else if (newRepetitions == 2) {
                newInterval = 6;
            } else {
                newInterval = (int) Math.round(intervalDays * newEaseFactor);
            }
        }

        return new Result(newEaseFactor, newInterval, newRepetitions);
    }
}
