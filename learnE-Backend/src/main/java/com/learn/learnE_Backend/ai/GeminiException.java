package com.learn.learnE_Backend.ai;

import java.time.Duration;

/**
 * A call to Gemini failed. {@code status} carries the HTTP code when there was one, so callers can
 * tell "out of quota, try another model" (429) from "this request is wrong" (400), where switching
 * models would fail exactly the same way.
 */
public class GeminiException extends RuntimeException {

    /** 0 when the failure was not an HTTP response (timeout, connection refused...). */
    private final int status;

    /** True only for the 429 that means "no requests left today", not the per-minute one. */
    private final boolean dailyQuotaExhausted;

    /**
     * How long this model asked us to wait, when it said. Null for failures that are not a rate
     * limit. The caller uses it to stop picking this model for that long, rather than every later
     * request discovering the same limit and waiting all over again.
     */
    private final Duration retryAfter;

    public GeminiException(
            String message, Throwable cause, int status, boolean dailyQuotaExhausted, Duration retryAfter) {
        super(message, cause);
        this.status = status;
        this.dailyQuotaExhausted = dailyQuotaExhausted;
        this.retryAfter = retryAfter;
    }

    public GeminiException(String message, Throwable cause, int status, boolean dailyQuotaExhausted) {
        this(message, cause, status, dailyQuotaExhausted, null);
    }

    public GeminiException(String message, Throwable cause, int status) {
        this(message, cause, status, false, null);
    }

    public GeminiException(String message) {
        super(message);
        this.status = 0;
        this.dailyQuotaExhausted = false;
        this.retryAfter = null;
    }

    public int getStatus() {
        return status;
    }

    /** Null when the failure says nothing about when to try again. */
    public Duration getRetryAfter() {
        return retryAfter;
    }

    /**
     * Whether this model has no free-tier requests left until the quota resets. Waiting cannot help,
     * so the caller should move to another model immediately and stop picking this one meanwhile.
     */
    public boolean isDailyQuotaExhausted() {
        return dailyQuotaExhausted;
    }

    /**
     * Worth retrying on a different model: out of quota (429), overloaded (503), or the model no
     * longer exists (404) — Google retires model names over time, and that must not take the whole
     * feature down. A 400 or 401 would fail identically on every model, so those do not fall through.
     */
    public boolean isWorthAnotherModel() {
        return status == 429 || status == 503 || status == 404;
    }
}
