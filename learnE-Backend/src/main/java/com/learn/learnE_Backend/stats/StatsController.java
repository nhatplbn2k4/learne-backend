package com.learn.learnE_Backend.stats;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.vocabulary.Language;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsDto getStats(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Language language
    ) {
        return statsService.getStats(user.getId(), language);
    }
}
