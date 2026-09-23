package com.learn.learnE_Backend.reading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingAttemptRepository extends JpaRepository<ReadingAttempt, Long> {
    List<ReadingAttempt> findByUser_IdAndPassage_IdOrderByCompletedAtDesc(Long userId, Long passageId);

    Optional<ReadingAttempt> findTopByUser_IdAndPassage_IdOrderByScoreDesc(Long userId, Long passageId);

    long countByUser_IdAndPassage_Id(Long userId, Long passageId);

    List<ReadingAttempt> findByUser_Id(Long userId);
}
