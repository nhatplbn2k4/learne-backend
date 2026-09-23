package com.learn.learnE_Backend.sentence;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.sentence.dto.SentenceAttemptDto;
import com.learn.learnE_Backend.sentence.dto.SentenceSetDto;
import com.learn.learnE_Backend.sentence.dto.SubmitSentenceRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SentenceController {

    private final SentenceService sentenceService;

    public SentenceController(SentenceService sentenceService) {
        this.sentenceService = sentenceService;
    }

    @GetMapping("/lesson-days/{lessonDayId}/sentences")
    public SentenceSetDto getDaySentences(@AuthenticationPrincipal User user, @PathVariable Long lessonDayId) {
        return sentenceService.getDaySentences(user, lessonDayId);
    }

    @PostMapping("/sentences/{exerciseId}/attempts")
    public SentenceAttemptDto submit(
            @AuthenticationPrincipal User user,
            @PathVariable Long exerciseId,
            @Valid @RequestBody SubmitSentenceRequest request
    ) {
        return sentenceService.submit(user, exerciseId, request);
    }
}
