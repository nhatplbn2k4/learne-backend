package com.learn.learnE_Backend.admin.dto;

import com.learn.learnE_Backend.auth.Role;
import com.learn.learnE_Backend.auth.UserStatus;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String email,
        String displayName,
        Role role,
        UserStatus status,
        Instant createdAt,
        int wordsStudied,
        int wordsMastered
) {
}
