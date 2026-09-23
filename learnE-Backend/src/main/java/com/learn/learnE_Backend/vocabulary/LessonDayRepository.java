package com.learn.learnE_Backend.vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonDayRepository extends JpaRepository<LessonDay, Long> {
    Optional<LessonDay> findByCourse_IdAndDayNumber(Long courseId, int dayNumber);

    List<LessonDay> findByCourse_IdOrderByDayNumberAsc(Long courseId);

    long countByCourse_Id(Long courseId);
}
