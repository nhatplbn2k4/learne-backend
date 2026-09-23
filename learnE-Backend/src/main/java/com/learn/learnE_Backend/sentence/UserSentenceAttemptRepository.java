package com.learn.learnE_Backend.sentence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSentenceAttemptRepository extends JpaRepository<UserSentenceAttempt, Long> {
    List<UserSentenceAttempt> findByUser_IdAndExercise_LessonDay_IdOrderBySubmittedAtDesc(
            Long userId, Long lessonDayId);

    Optional<UserSentenceAttempt> findTopByUser_IdAndExercise_IdOrderByScoreDesc(Long userId, Long exerciseId);

    void deleteByExercise_Id(Long exerciseId);

    /** Every attempt by this user, newest first — feeds the dashboard's translation stats. */
    List<UserSentenceAttempt> findByUser_IdOrderBySubmittedAtDesc(Long userId);

    /** Every attempt this user made anywhere in a course, so the day map needs a single query. */
    List<UserSentenceAttempt> findByUser_IdAndExercise_LessonDay_Course_Id(Long userId, Long courseId);
}
