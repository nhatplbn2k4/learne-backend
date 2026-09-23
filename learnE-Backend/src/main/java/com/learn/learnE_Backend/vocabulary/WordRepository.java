package com.learn.learnE_Backend.vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {
    List<Word> findByLessonDay_Id(Long lessonDayId);

    long countByLessonDay_Id(Long lessonDayId);

    void deleteByLessonDay_Id(Long lessonDayId);
}
