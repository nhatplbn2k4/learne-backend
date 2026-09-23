package com.learn.learnE_Backend.writing;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.writing.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/writing")
public class WritingController {

    private final WritingService writingService;

    public WritingController(WritingService writingService) {
        this.writingService = writingService;
    }

    @GetMapping("/prompts")
    public List<WritingPromptSummaryDto> listPrompts(@AuthenticationPrincipal User user) {
        return writingService.listPrompts(user.getId());
    }

    @GetMapping("/prompts/{id}")
    public WritingPromptDetailDto getPrompt(@PathVariable Long id) {
        return writingService.getPrompt(id);
    }

    @PostMapping("/prompts/{id}/submit")
    public WritingSubmissionDto submit(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody SubmitWritingRequest request
    ) {
        return writingService.submit(user, id, request);
    }

    @GetMapping("/submissions")
    public List<WritingSubmissionDto> listSubmissions(@AuthenticationPrincipal User user) {
        return writingService.listSubmissions(user.getId());
    }
}
