package com.learn.learnE_Backend.grammar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrammarExerciseRepository extends JpaRepository<GrammarExercise, Long> {

    List<GrammarExercise> findByLesson_IdOrderBySortOrderAscIdAsc(Long lessonId);

    long countByLesson_Id(Long lessonId);

    /** One query for a whole course, so a lesson list does not issue one per lesson. */
    List<GrammarExercise> findByLesson_Course_Id(Long courseId);

    void deleteByLesson_Id(Long lessonId);
}
