package com.learn.learnE_Backend.sentence.dto;

/**
 * Progress towards unlocking the next day early on a Chinese course, by acing this day's
 * translation set instead of waiting for tomorrow.
 *
 * @param available  sentences this day actually has; fewer than {@code required} makes it impossible
 * @param answered   distinct sentences answered at least once (repeats do not count twice)
 * @param average    mean of the best score per sentence, {@code null} when nothing is graded yet
 */
public record SkipAheadDto(
        int available,
        int answered,
        int required,
        Double average,
        double minAverage,
        boolean eligible
) {
}
