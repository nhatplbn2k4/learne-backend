package com.learn.learnE_Backend.vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserWordProgressRepository extends JpaRepository<UserWordProgress, Long> {
    Optional<UserWordProgress> findByUser_IdAndWord_Id(Long userId, Long wordId);

    List<UserWordProgress> findByUser_IdAndNextReviewDateLessThanEqual(Long userId, LocalDate date);

    long countByUser_IdAndWord_LessonDay_Id(Long userId, Long lessonDayId);

    /**
     * Words of a day this user has actually mastered — distinct from the count above, which only
     * says a progress row exists (i.e. the word was practised at all).
     */
    long countByUser_IdAndWord_LessonDay_IdAndMasteredTrue(Long userId, Long lessonDayId);

    void deleteByWord_Id(Long wordId);

    void deleteByUser_Id(Long userId);

    void deleteByWord_LessonDay_Id(Long lessonDayId);

    long countByUser_Id(Long userId);

    /** Same "thuộc lòng" rule as the course day map: aced every mode within one session. */
    @Query("select count(p) from UserWordProgress p where p.user.id = :userId and p.mastered = true")
    long countFullyMastered(@Param("userId") Long userId);

    long countByUser_IdAndNextReviewDateLessThanEqual(Long userId, LocalDate date);

    /**
     * Every word of one language this learner has already practised.
     *
     * <p>Used to drop "Từ chưa học" hints for words they plainly know. Those hints are written by
     * the AI when the exercise is generated, from its own guess at what is hard — it has no idea
     * what this person has studied, so the guess has to be checked against their progress.
     */
    @Query("""
            select p.word.term from UserWordProgress p
            where p.user.id = :userId and p.word.lessonDay.course.topic.language = :language
            """)
    List<String> findStudiedTerms(@Param("userId") Long userId, @Param("language") Language language);

    // ---- Per-language variants, for when the dashboard is switched to one language ----

    @Query("""
            select count(p) from UserWordProgress p
            where p.user.id = :userId and p.word.lessonDay.course.topic.language = :language
            """)
    long countByUserAndLanguage(@Param("userId") Long userId, @Param("language") Language language);

    @Query("""
            select count(p) from UserWordProgress p
            where p.user.id = :userId and p.mastered = true
              and p.word.lessonDay.course.topic.language = :language
            """)
    long countFullyMasteredByLanguage(@Param("userId") Long userId, @Param("language") Language language);

    @Query("""
            select count(p) from UserWordProgress p
            where p.user.id = :userId and p.nextReviewDate <= :date
              and p.word.lessonDay.course.topic.language = :language
            """)
    long countDueByLanguage(@Param("userId") Long userId, @Param("language") Language language,
                            @Param("date") LocalDate date);
}
