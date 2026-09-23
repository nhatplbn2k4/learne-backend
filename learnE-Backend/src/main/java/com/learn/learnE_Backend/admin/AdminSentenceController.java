package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.sentence.SentenceService;
import com.learn.learnE_Backend.sentence.dto.GenerateSentencesRequest;
import com.learn.learnE_Backend.sentence.dto.SentenceExerciseDto;
import com.learn.learnE_Backend.sentence.dto.TagGrammarResultDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminSentenceController {

    private final SentenceService sentenceService;

    public AdminSentenceController(SentenceService sentenceService) {
        this.sentenceService = sentenceService;
    }

    /** Unlike the learner endpoint this ignores the mastery gate and always shows the answer. */
    @GetMapping("/lesson-days/{lessonDayId}/sentences")
    public List<SentenceExerciseDto> list(@PathVariable Long lessonDayId) {
        return sentenceService.listForAdmin(lessonDayId);
    }

    /** Tags existing sentences with the grammar they use, for the warning on practice. */
    @PostMapping("/lesson-days/{lessonDayId}/tag-grammar")
    public TagGrammarResultDto tagGrammar(@PathVariable Long lessonDayId) {
        return sentenceService.tagGrammar(lessonDayId);
    }

    @PostMapping("/lesson-days/{lessonDayId}/generate-sentences")
    public List<SentenceExerciseDto> generate(
            @PathVariable Long lessonDayId,
            @Valid @RequestBody GenerateSentencesRequest request
    ) {
        return sentenceService.generate(lessonDayId, request);
    }

    @DeleteMapping("/lesson-days/{lessonDayId}/sentences")
    public void deleteAll(@PathVariable Long lessonDayId) {
        sentenceService.deleteAll(lessonDayId);
    }

    @DeleteMapping("/sentences/{exerciseId}")
    public void delete(@PathVariable Long exerciseId) {
        sentenceService.delete(exerciseId);
    }
}
