package com.learn.learnE_Backend.grammar.dto;

/**
 * One drill sentence a learner has answered, with their best score on it.
 *
 * <p>Both figures the lesson list needs come from these rows: how many sentences were answered is
 * how many rows a lesson has, and the average is the mean of the scores. Taking the best score per
 * sentence rather than every attempt means redoing one can only pull the average up.
 */
public record LessonExerciseScore(Long lessonId, Long exerciseId, Integer bestScore) {
}
