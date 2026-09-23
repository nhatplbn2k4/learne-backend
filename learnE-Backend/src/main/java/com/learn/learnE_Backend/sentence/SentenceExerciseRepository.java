package com.learn.learnE_Backend.sentence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentenceExerciseRepository extends JpaRepository<SentenceExercise, Long> {
    List<SentenceExercise> findByLessonDay_IdOrderBySortOrderAscIdAsc(Long lessonDayId);

    long countByLessonDay_Id(Long lessonDayId);

    /** Every sentence across a set of days — the pool a skip-ahead test draws its questions from. */
    List<SentenceExercise> findByLessonDay_IdIn(List<Long> lessonDayIds);

    List<SentenceExercise> findByLessonDay_Course_Id(Long courseId);

    void deleteByLessonDay_Id(Long lessonDayId);
}
