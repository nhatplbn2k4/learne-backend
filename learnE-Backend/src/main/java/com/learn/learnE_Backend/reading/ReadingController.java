package com.learn.learnE_Backend.reading;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.reading.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading")
public class ReadingController {

    private final ReadingService readingService;

    public ReadingController(ReadingService readingService) {
        this.readingService = readingService;
    }

    @GetMapping("/passages")
    public List<ReadingPassageSummaryDto> listPassages(@AuthenticationPrincipal User user) {
        return readingService.listPassages(user.getId());
    }

    @GetMapping("/passages/{id}")
    public ReadingPassageDetailDto getPassage(@PathVariable Long id) {
        return readingService.getPassage(id);
    }

    @PostMapping("/passages/{id}/submit")
    public ReadingAttemptResultDto submit(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody SubmitReadingRequest request
    ) {
        return readingService.submit(user, id, request);
    }
}
