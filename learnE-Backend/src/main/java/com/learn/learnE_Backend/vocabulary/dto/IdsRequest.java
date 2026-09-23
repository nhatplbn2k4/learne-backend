package com.learn.learnE_Backend.vocabulary.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record IdsRequest(@NotEmpty List<Long> ids) {
}
