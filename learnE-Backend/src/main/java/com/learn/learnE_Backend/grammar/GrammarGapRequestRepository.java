package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.vocabulary.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrammarGapRequestRepository extends JpaRepository<GrammarGapRequest, Long> {

    Optional<GrammarGapRequest> findByLanguageAndNormalizedName(Language language, String normalizedName);

    List<GrammarGapRequest> findByLanguageOrderByStatusAscReportCountDescLastReportedAtDesc(Language language);

    long countByStatus(GrammarGapStatus status);
}
