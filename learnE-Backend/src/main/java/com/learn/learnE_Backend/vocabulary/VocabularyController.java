package com.learn.learnE_Backend.vocabulary;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.vocabulary.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    /**
     * @param kind defaults to VOCABULARY so that a caller which predates grammar courses — or
     *             simply forgets the parameter — keeps getting exactly what it got before.
     */
    @GetMapping("/courses")
    public List<CourseSummaryDto> listCourses(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Language language,
            @RequestParam(defaultValue = "VOCABULARY") CourseKind kind
    ) {
        return vocabularyService.listCourses(user.getId(), language, kind);
    }

    @PostMapping("/courses/{courseId}/enroll")
    public EnrollmentDto enroll(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return vocabularyService.enroll(user, courseId);
    }

    @GetMapping("/courses/{courseId}/days")
    public List<CourseDayDto> courseDays(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return vocabularyService.listCourseDays(user, courseId);
    }

    @GetMapping("/lesson-days/{lessonDayId}/session")
    public DaySessionDto daySession(@AuthenticationPrincipal User user, @PathVariable Long lessonDayId) {
        return vocabularyService.getDaySession(user, lessonDayId);
    }

    @GetMapping("/today")
    public List<EnrollmentSessionDto> today(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Language language
    ) {
        return vocabularyService.getToday(user, language);
    }

    @PostMapping("/vocabulary/review")
    public ReviewResponse submitReview(@AuthenticationPrincipal User user, @Valid @RequestBody ReviewRequest request) {
        return vocabularyService.submitReview(user, request);
    }
}
