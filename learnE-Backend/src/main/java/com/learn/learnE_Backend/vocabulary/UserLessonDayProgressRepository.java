package com.learn.learnE_Backend.vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLessonDayProgressRepository extends JpaRepository<UserLessonDayProgress, Long> {
    boolean existsByUser_IdAndLessonDay_Id(Long userId, Long lessonDayId);

    Optional<UserLessonDayProgress> findByUser_IdAndLessonDay_Id(Long userId, Long lessonDayId);

    void deleteByLessonDay_Id(Long lessonDayId);

    void deleteByUser_Id(Long userId);
}
