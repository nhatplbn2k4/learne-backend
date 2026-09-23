package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.ai.AiContentService;
import com.learn.learnE_Backend.ai.GeminiClient;
import com.learn.learnE_Backend.ai.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai")
public class AdminAiController {

    private final AiContentService aiContentService;
    private final GeminiClient geminiClient;

    public AdminAiController(AiContentService aiContentService, GeminiClient geminiClient) {
        this.aiContentService = aiContentService;
        this.geminiClient = geminiClient;
    }

    /** Which model grading and generation would use right now, and what is rate-limited. */
    @GetMapping("/models")
    public List<ModelStatusDto> models() {
        return geminiClient.modelStatus();
    }

    @PostMapping("/generate-reading")
    public GeneratedReadingDto generateReading(@Valid @RequestBody GenerateReadingRequest request) {
        return aiContentService.generateReadingPassage(request);
    }

    @PostMapping("/generate-writing-prompt")
    public GeneratedWritingPromptDto generateWritingPrompt(@Valid @RequestBody GenerateWritingPromptRequest request) {
        return aiContentService.generateWritingPrompt(request);
    }
}
