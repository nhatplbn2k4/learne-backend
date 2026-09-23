package com.learn.learnE_Backend.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {
    List<WritingSubmission> findByUser_IdOrderBySubmittedAtDesc(Long userId);

    List<WritingSubmission> findByUser_IdAndPrompt_IdOrderBySubmittedAtDesc(Long userId, Long promptId);

    Optional<WritingSubmission> findTopByUser_IdAndPrompt_IdOrderByScoreDesc(Long userId, Long promptId);

    long countByUser_IdAndPrompt_Id(Long userId, Long promptId);
}
