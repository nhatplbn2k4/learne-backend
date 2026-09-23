package com.learn.learnE_Backend.common;

public record ApiError(String timestamp, int status, String error, String message, String path) {
}
