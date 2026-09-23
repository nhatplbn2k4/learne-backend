package com.learn.learnE_Backend.grammar;

/** Where an admin has got to with a reported grammar gap. */
public enum GrammarGapStatus {
    /** Still waiting for a lesson to be written. */
    OPEN,
    /** A lesson now covers it. */
    RESOLVED,
    /** Deliberately not worth a lesson — kept so it stops coming back to the top of the list. */
    IGNORED
}
