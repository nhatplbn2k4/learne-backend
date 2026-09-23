package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.writing.WritingService;
import com.learn.learnE_Backend.writing.dto.CreateWritingPromptRequest;
import com.learn.learnE_Backend.writing.dto.WritingPromptDetailDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/writing")
public class AdminWritingController {

    private final WritingService writingService;

    public AdminWritingController(WritingService writingService) {
        this.writingService = writingService;
    }

    @PostMapping("/prompts")
    public WritingPromptDetailDto createPrompt(@Valid @RequestBody CreateWritingPromptRequest request) {
        return writingService.createPrompt(request);
    }
}
