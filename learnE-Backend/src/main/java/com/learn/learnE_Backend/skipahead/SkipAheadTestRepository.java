package com.learn.learnE_Backend.skipahead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkipAheadTestRepository extends JpaRepository<SkipAheadTest, Long> {

    /** At most one test may be open at a time, so a fresh sitting cannot dodge the cooldown. */
    Optional<SkipAheadTest> findByUser_IdAndFinishedAtIsNull(Long userId);

    List<SkipAheadTest> findByUser_Id(Long userId);

    Optional<SkipAheadTest> findFirstByUser_IdAndCourse_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(
            Long userId, Long courseId);

    /**
     * Practice sentences this learner has already met in an earlier sitting. Without this a second
     * exam could hand back the very same questions, which is what the test is meant to avoid.
     */
    @Query("select i.sourceExerciseId from SkipAheadTestItem i "
            + "where i.test.user.id = :userId and i.sourceExerciseId is not null")
    List<Long> findUsedSourceExerciseIds(@Param("userId") Long userId);
}
