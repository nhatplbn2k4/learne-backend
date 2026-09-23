package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.ai.dto.GeneratedDayWordsDto;
import com.learn.learnE_Backend.vocabulary.VocabularyService;
import com.learn.learnE_Backend.vocabulary.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminVocabularyController {

    private final VocabularyService vocabularyService;

    public AdminVocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @GetMapping("/topics")
    public List<TopicDto> listTopics() {
        return vocabularyService.listTopics();
    }

    @PostMapping("/topics")
    public TopicDto createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return vocabularyService.createTopic(request);
    }

    @GetMapping("/courses")
    public List<AdminCourseDto> listCourses() {
        return vocabularyService.listAllCoursesAdmin();
    }

    @PostMapping("/courses")
    public AdminCourseDto createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return vocabularyService.createCourse(request);
    }

    @GetMapping("/courses/{courseId}/lesson-days")
    public List<LessonDayDto> listLessonDays(@PathVariable Long courseId) {
        return vocabularyService.listLessonDays(courseId);
    }

    @PostMapping("/courses/{courseId}/lesson-days")
    public LessonDayDto createLessonDay(@PathVariable Long courseId, @Valid @RequestBody CreateLessonDayRequest request) {
        return vocabularyService.createLessonDay(courseId, request);
    }

    @GetMapping("/lesson-days/{lessonDayId}/words")
    public List<WordDto> listWords(@PathVariable Long lessonDayId) {
        return vocabularyService.listWords(lessonDayId);
    }

    @PostMapping("/lesson-days/{lessonDayId}/words")
    public WordDto createWord(@PathVariable Long lessonDayId, @Valid @RequestBody CreateWordRequest request) {
        return vocabularyService.createWord(lessonDayId, request);
    }

    @PostMapping("/lesson-days/{lessonDayId}/words/batch")
    public List<WordDto> createWordsBatch(@PathVariable Long lessonDayId, @Valid @RequestBody CreateWordsBatchRequest request) {
        return vocabularyService.createWordsBatch(lessonDayId, request);
    }

    @PutMapping("/lesson-days/{lessonDayId}")
    public LessonDayDto updateLessonDay(@PathVariable Long lessonDayId, @RequestBody UpdateLessonDayRequest request) {
        return vocabularyService.updateLessonDay(lessonDayId, request);
    }

    @DeleteMapping("/lesson-days/{lessonDayId}")
    public void deleteLessonDay(@PathVariable Long lessonDayId) {
        vocabularyService.deleteLessonDay(lessonDayId);
    }

    @PostMapping("/lesson-days/delete-batch")
    public void deleteLessonDays(@Valid @RequestBody IdsRequest request) {
        vocabularyService.deleteLessonDays(request.ids());
    }

    @DeleteMapping("/lesson-days/{lessonDayId}/words")
    public void deleteAllWordsInDay(@PathVariable Long lessonDayId) {
        vocabularyService.deleteAllWordsInDay(lessonDayId);
    }

    @PostMapping("/words/delete-batch")
    public void deleteWords(@Valid @RequestBody IdsRequest request) {
        vocabularyService.deleteWords(request.ids());
    }

    @PutMapping("/words/{wordId}")
    public WordDto updateWord(@PathVariable Long wordId, @Valid @RequestBody CreateWordRequest request) {
        return vocabularyService.updateWord(wordId, request);
    }

    @DeleteMapping("/words/{wordId}")
    public void deleteWord(@PathVariable Long wordId) {
        vocabularyService.deleteWord(wordId);
    }

    @PostMapping("/lesson-days/{lessonDayId}/generate-words")
    public GeneratedDayWordsDto generateDayWords(
            @PathVariable Long lessonDayId,
            @Valid @RequestBody GenerateDayWordsRequest request
    ) {
        return vocabularyService.generateDayWords(lessonDayId, request);
    }
}
