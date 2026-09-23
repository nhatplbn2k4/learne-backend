package com.learn.learnE_Backend.vocabulary;

/**
 * What a course teaches, which decides how it is structured and studied.
 *
 * <p>{@link #VOCABULARY} courses are split into days of words, unlocked one per day.
 * {@link #GRAMMAR} courses are split into lessons of patterns ordered from basic to hard, all
 * open from the start so a learner can look one up the moment they meet it.
 */
public enum CourseKind {
    VOCABULARY,
    GRAMMAR
}
