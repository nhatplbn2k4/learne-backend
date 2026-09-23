package com.learn.learnE_Backend.reading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReadingQuestionRepository extends JpaRepository<ReadingQuestion, Long> {
    List<ReadingQuestion> findByPassage_IdOrderByIdAsc(Long passageId);

    long countByPassage_Id(Long passageId);
}
