package com.learn.learnE_Backend.auth.dto;

import com.learn.learnE_Backend.auth.UserStatus;

/**
 * Registration no longer hands back a token: the account exists but cannot be used until an admin
 * approves it, so there is nothing to sign in with yet.
 */
public record RegisterResponse(Long userId, String email, UserStatus status, String message) {
}
