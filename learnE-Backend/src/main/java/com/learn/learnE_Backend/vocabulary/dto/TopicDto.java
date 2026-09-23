package com.learn.learnE_Backend.vocabulary.dto;

import com.learn.learnE_Backend.vocabulary.Language;
import com.learn.learnE_Backend.vocabulary.Topic;

public record TopicDto(Long id, String slug, String name, String description, Language language) {
    public static TopicDto from(Topic topic) {
        return new TopicDto(
                topic.getId(), topic.getSlug(), topic.getName(), topic.getDescription(), topic.getLanguage());
    }
}
