package com.learn.learnE_Backend.ai.dto;

import java.time.Instant;

/**
 * What the app currently thinks of one model.
 *
 * @param model      the model name as configured
 * @param preferred  true for the one a normal request starts with right now
 * @param available  false while a rate limit or an exhausted daily quota is still in force
 * @param retryAt    when it becomes available again; null when it is available
 * @param reason     why it is out of play, in plain words
 */
public record ModelStatusDto(
        String model,
        boolean preferred,
        boolean available,
        Instant retryAt,
        String reason
) {
}
