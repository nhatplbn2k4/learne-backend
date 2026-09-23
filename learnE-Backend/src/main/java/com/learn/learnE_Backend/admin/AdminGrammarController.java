package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.grammar.GrammarService;
import com.learn.learnE_Backend.grammar.dto.GenerateGrammarExercisesRequest;
import com.learn.learnE_Backend.grammar.dto.GrammarExerciseDto;
import com.learn.learnE_Backend.grammar.dto.GrammarLessonSummaryDto;
import com.learn.learnE_Backend.grammar.dto.SaveGrammarLessonRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminGrammarController {

    private final GrammarService grammarService;

    public AdminGrammarController(GrammarService grammarService) {
        this.grammarService = grammarService;
    }

    @GetMapping("/grammar-courses/{courseId}/lessons")
    public List<GrammarLessonSummaryDto> listLessons(@PathVariable Long courseId) {
        return grammarService.listForAdmin(courseId);
    }

    @PostMapping("/grammar-courses/{courseId}/lessons")
    public GrammarLessonSummaryDto create(
            @PathVariable Long courseId, @Valid @RequestBody SaveGrammarLessonRequest request) {
        return grammarService.create(courseId, request);
    }

    @PutMapping("/grammar-lessons/{lessonId}")
    public GrammarLessonSummaryDto update(
            @PathVariable Long lessonId, @Valid @RequestBody SaveGrammarLessonRequest request) {
        return grammarService.update(lessonId, request);
    }

    @DeleteMapping("/grammar-lessons/{lessonId}")
    public void delete(@PathVariable Long lessonId) {
        grammarService.delete(lessonId);
    }

    @GetMapping("/grammar-lessons/{lessonId}/exercises")
    public List<GrammarExerciseDto> listExercises(@PathVariable Long lessonId) {
        return grammarService.listExercisesForAdmin(lessonId);
    }

    @PostMapping("/grammar-lessons/{lessonId}/generate-exercises")
    public List<GrammarExerciseDto> generateExercises(
            @PathVariable Long lessonId, @Valid @RequestBody GenerateGrammarExercisesRequest request) {
        return grammarService.generateExercises(lessonId, request);
    }

    @DeleteMapping("/grammar-exercises/{exerciseId}")
    public void deleteExercise(@PathVariable Long exerciseId) {
        grammarService.deleteExercise(exerciseId);
    }
}
