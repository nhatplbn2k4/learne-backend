package com.learn.learnE_Backend.skipahead;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.skipahead.dto.SkipAheadStatusDto;
import com.learn.learnE_Backend.skipahead.dto.SkipAheadTestDto;
import com.learn.learnE_Backend.skipahead.dto.SubmitSkipAheadAnswerRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SkipAheadController {

    private final SkipAheadService skipAheadService;

    public SkipAheadController(SkipAheadService skipAheadService) {
        this.skipAheadService = skipAheadService;
    }

    @GetMapping("/courses/{courseId}/skip-ahead/{targetDayNumber}")
    public SkipAheadStatusDto status(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @PathVariable int targetDayNumber
    ) {
        return skipAheadService.status(user, courseId, targetDayNumber);
    }

    @PostMapping("/courses/{courseId}/skip-ahead/{targetDayNumber}/start")
    public SkipAheadTestDto start(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @PathVariable int targetDayNumber
    ) {
        return skipAheadService.start(user, courseId, targetDayNumber);
    }

    @GetMapping("/skip-ahead-tests/{testId}")
    public SkipAheadTestDto get(@AuthenticationPrincipal User user, @PathVariable Long testId) {
        return skipAheadService.get(user, testId);
    }

    /** One shot per question: the answer is recorded but not graded or scored yet. */
    @PostMapping("/skip-ahead-tests/{testId}/items/{itemId}")
    public SkipAheadTestDto submitAnswer(
            @AuthenticationPrincipal User user,
            @PathVariable Long testId,
            @PathVariable Long itemId,
            @Valid @RequestBody SubmitSkipAheadAnswerRequest request
    ) {
        return skipAheadService.submitAnswer(user, testId, itemId, request.answerTarget());
    }

    /** Grades the whole paper in one go and reveals every result. */
    @PostMapping("/skip-ahead-tests/{testId}/finish")
    public SkipAheadTestDto finish(@AuthenticationPrincipal User user, @PathVariable Long testId) {
        return skipAheadService.finish(user, testId);
    }
}
