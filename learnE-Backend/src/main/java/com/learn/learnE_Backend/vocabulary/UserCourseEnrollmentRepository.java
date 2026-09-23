package com.learn.learnE_Backend.vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCourseEnrollmentRepository extends JpaRepository<UserCourseEnrollment, Long> {
    List<UserCourseEnrollment> findByUser_IdAndCompletedAtIsNull(Long userId);

    Optional<UserCourseEnrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);

    List<UserCourseEnrollment> findByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);
}
