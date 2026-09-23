package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.reading.ReadingService;
import com.learn.learnE_Backend.reading.dto.AdminReadingPassageDto;
import com.learn.learnE_Backend.reading.dto.CreateReadingPassageRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reading")
public class AdminReadingController {

    private final ReadingService readingService;

    public AdminReadingController(ReadingService readingService) {
        this.readingService = readingService;
    }

    @GetMapping("/passages/{id}")
    public AdminReadingPassageDto getPassage(@PathVariable Long id) {
        return readingService.getPassageForAdmin(id);
    }

    @PostMapping("/passages")
    public AdminReadingPassageDto createPassage(@Valid @RequestBody CreateReadingPassageRequest request) {
        return readingService.createPassageWithQuestions(request);
    }
}
