package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.grammar.dto.LessonExerciseScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserGrammarAttemptRepository extends JpaRepository<UserGrammarAttempt, Long> {

    List<UserGrammarAttempt> findByUser_IdAndExercise_Lesson_IdOrderBySubmittedAtDesc(
            Long userId, Long lessonId);

    /**
     * The best score on every sentence this learner has answered, for a whole course in one query.
     *
     * <p>Read from the attempts rather than from a running total: the total on the progress row
     * counts submissions, so redoing one sentence three times moved it by three and the lesson list
     * ended up claiming "27/10 câu bài tập".
     */
    @Query("""
            select new com.learn.learnE_Backend.grammar.dto.LessonExerciseScore(
                       a.exercise.lesson.id, a.exercise.id, max(a.score))
            from UserGrammarAttempt a
            where a.user.id = :userId and a.exercise.lesson.course.id = :courseId
            group by a.exercise.lesson.id, a.exercise.id
            """)
    List<LessonExerciseScore> bestScoresPerLesson(
            @Param("userId") Long userId, @Param("courseId") Long courseId);

    void deleteByExercise_Id(Long exerciseId);

    void deleteByExercise_Lesson_Id(Long lessonId);
}
