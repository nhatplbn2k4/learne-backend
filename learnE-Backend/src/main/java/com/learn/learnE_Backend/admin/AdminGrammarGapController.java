package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.ai.dto.GeneratedGrammarLessonDto;
import com.learn.learnE_Backend.grammar.GrammarGapService;
import com.learn.learnE_Backend.grammar.dto.GrammarGapRequestDto;
import com.learn.learnE_Backend.vocabulary.Language;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Grammar points learners keep getting wrong that no lesson covers yet. */
@RestController
@RequestMapping("/api/admin/grammar-gaps")
public class AdminGrammarGapController {

    private final GrammarGapService gapService;

    public AdminGrammarGapController(GrammarGapService gapService) {
        this.gapService = gapService;
    }

    @GetMapping
    public List<GrammarGapRequestDto> list(@RequestParam(defaultValue = "CHINESE") Language language) {
        return gapService.list(language);
    }

    /** Drafts the missing lesson with AI; the admin reviews and saves it themselves. */
    @PostMapping("/{gapId}/draft-lesson")
    public GeneratedGrammarLessonDto draftLesson(@PathVariable Long gapId) {
        return gapService.draftLesson(gapId);
    }

    /** @param lessonId optional: the lesson just written to cover this gap. */
    @PostMapping("/{gapId}/resolve")
    public GrammarGapRequestDto resolve(
            @PathVariable Long gapId, @RequestParam(required = false) Long lessonId) {
        return gapService.resolve(gapId, lessonId);
    }

    @PostMapping("/{gapId}/ignore")
    public GrammarGapRequestDto ignore(@PathVariable Long gapId) {
        return gapService.ignore(gapId);
    }

    @DeleteMapping("/{gapId}")
    public void delete(@PathVariable Long gapId) {
        gapService.delete(gapId);
    }
}
