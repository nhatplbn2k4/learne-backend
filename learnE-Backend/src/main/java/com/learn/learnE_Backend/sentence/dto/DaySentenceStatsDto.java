package com.learn.learnE_Backend.sentence.dto;

/**
 * How far through a day's translation set the learner is.
 *
 * @param total    sentences the day has
 * @param answered distinct sentences answered at least once (re-doing one does not count twice)
 * @param average  mean of the best score per sentence, {@code null} until something is graded
 */
public record DaySentenceStatsDto(int total, int answered, Double average) {

    public static final DaySentenceStatsDto EMPTY = new DaySentenceStatsDto(0, 0, null);
}
