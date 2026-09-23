package com.learn.learnE_Backend.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String email,
        String displayName,
        String role
) {
    public static AuthResponse of(String accessToken, Long userId, String email, String displayName, String role) {
        return new AuthResponse(accessToken, "Bearer", userId, email, displayName, role);
    }
}
