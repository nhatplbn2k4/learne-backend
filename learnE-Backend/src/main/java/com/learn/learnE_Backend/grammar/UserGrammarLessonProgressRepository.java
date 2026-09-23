package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.vocabulary.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserGrammarLessonProgressRepository extends JpaRepository<UserGrammarLessonProgress, Long> {

    Optional<UserGrammarLessonProgress> findByUser_IdAndLesson_Id(Long userId, Long lessonId);

    List<UserGrammarLessonProgress> findByUser_IdAndLesson_Course_Id(Long userId, Long courseId);

    /** Everything this learner has studied in one language — the basis of the "chưa học" warning. */
    List<UserGrammarLessonProgress> findByUser_IdAndLesson_Course_Topic_Language(Long userId, Language language);

    void deleteByLesson_Id(Long lessonId);
}
