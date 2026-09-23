package com.learn.learnE_Backend.auth;

/** Whether an account may be used. New sign-ups wait for an admin. */
public enum UserStatus {
    PENDING,
    ACTIVE,
    REJECTED
}
