package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.vocabulary.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrammarLessonRepository extends JpaRepository<GrammarLesson, Long> {

    List<GrammarLesson> findByCourse_IdOrderBySortOrderAscIdAsc(Long courseId);

    Optional<GrammarLesson> findByCode(String code);

    /** Every lesson of one language, for the catalogue the AI is given while grading. */
    List<GrammarLesson> findByCourse_Topic_LanguageOrderBySortOrderAscIdAsc(Language language);

    boolean existsByCode(String code);
}
