package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.grammar.dto.GrammarLessonDto;
import com.learn.learnE_Backend.grammar.dto.GrammarLessonSummaryDto;
import com.learn.learnE_Backend.grammar.dto.SubmitGrammarAnswerRequest;
import com.learn.learnE_Backend.sentence.dto.SentenceAttemptDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GrammarController {

    private final GrammarService grammarService;

    public GrammarController(GrammarService grammarService) {
        this.grammarService = grammarService;
    }

    /** No enrolment and no lock: grammar is reference material, open from the start. */
    @GetMapping("/grammar-courses/{courseId}/lessons")
    public List<GrammarLessonSummaryDto> listLessons(
            @AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return grammarService.listCourseLessons(user, courseId);
    }

    @GetMapping("/grammar-lessons/{lessonId}")
    public GrammarLessonDto getLesson(@AuthenticationPrincipal User user, @PathVariable Long lessonId) {
        return grammarService.getLesson(user, lessonId);
    }

    @PostMapping("/grammar-exercises/{exerciseId}/attempts")
    public SentenceAttemptDto submit(
            @AuthenticationPrincipal User user,
            @PathVariable Long exerciseId,
            @Valid @RequestBody SubmitGrammarAnswerRequest request
    ) {
        return grammarService.submit(user, exerciseId, request);
    }
}
