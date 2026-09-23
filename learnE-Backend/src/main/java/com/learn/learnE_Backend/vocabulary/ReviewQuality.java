package com.learn.learnE_Backend.vocabulary;

/**
 * The 4 self-assessment buttons shown after each review, mapped to SM-2 quality scores (0-5).
 */
public enum ReviewQuality {
    AGAIN(1),
    HARD(3),
    GOOD(4),
    EASY(5);

    private final int score;

    ReviewQuality(int score) {
        this.score = score;
    }

    public int score() {
        return score;
    }
}
