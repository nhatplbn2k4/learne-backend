package com.learn.learnE_Backend.grammar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.learnE_Backend.ai.AiContentService;
import com.learn.learnE_Backend.ai.dto.GeneratedSentencesDto;
import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.common.CjkText;
import com.learn.learnE_Backend.grammar.dto.*;
import com.learn.learnE_Backend.sentence.dto.NewWordHintDto;
import com.learn.learnE_Backend.sentence.dto.SentenceAttemptDto;
import com.learn.learnE_Backend.sentence.dto.SentenceFeedbackDto;
import com.learn.learnE_Backend.vocabulary.Course;
import com.learn.learnE_Backend.vocabulary.CourseKind;
import com.learn.learnE_Backend.vocabulary.CourseRepository;
import com.learn.learnE_Backend.vocabulary.Language;
import com.learn.learnE_Backend.vocabulary.UserWordProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Grammar lessons: a pattern explained, then drilled by translating sentences.
 *
 * <p>Unlike vocabulary days, lessons are never locked. They are ordered basic-to-hard as a
 * suggestion, but a learner who meets an unfamiliar pattern mid-practice must be able to open its
 * lesson straight away — that is the whole point of the cross-references in the grading feedback.
 */
@Service
public class GrammarService {

    private static final Logger log = LoggerFactory.getLogger(GrammarService.class);

    private final GrammarLessonRepository lessonRepository;
    private final GrammarExerciseRepository exerciseRepository;
    private final UserGrammarAttemptRepository attemptRepository;
    private final UserGrammarLessonProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final UserWordProgressRepository wordProgressRepository;
    private final AiContentService aiContentService;
    private final GrammarCatalogService grammarCatalogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GrammarService(
            GrammarLessonRepository lessonRepository,
            GrammarExerciseRepository exerciseRepository,
            UserGrammarAttemptRepository attemptRepository,
            UserGrammarLessonProgressRepository progressRepository,
            CourseRepository courseRepository,
            UserWordProgressRepository wordProgressRepository,
            AiContentService aiContentService,
            GrammarCatalogService grammarCatalogService
    ) {
        this.lessonRepository = lessonRepository;
        this.exerciseRepository = exerciseRepository;
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.courseRepository = courseRepository;
        this.wordProgressRepository = wordProgressRepository;
        this.aiContentService = aiContentService;
        this.grammarCatalogService = grammarCatalogService;
    }

    // ---- Reading ----

    /** The lesson list of one course, in two queries rather than two per lesson. */
    @Transactional(readOnly = true)
    public List<GrammarLessonSummaryDto> listCourseLessons(User user, Long courseId) {
        Map<Long, Integer> exerciseCounts = new HashMap<>();
        for (GrammarExercise exercise : exerciseRepository.findByLesson_Course_Id(courseId)) {
            exerciseCounts.merge(exercise.getLesson().getId(), 1, Integer::sum);
        }

        Map<Long, UserGrammarLessonProgress> progress = new HashMap<>();
        for (UserGrammarLessonProgress row : progressRepository
                .findByUser_IdAndLesson_Course_Id(user.getId(), courseId)) {
            progress.put(row.getLesson().getId(), row);
        }

        // Shown as "x/y câu", so it has to count sentences — the progress row counts submissions.
        Map<Long, List<Integer>> bestScores = new HashMap<>();
        for (LessonExerciseScore row : attemptRepository.bestScoresPerLesson(user.getId(), courseId)) {
            bestScores.computeIfAbsent(row.lessonId(), key -> new ArrayList<>()).add(row.bestScore());
        }

        return lessonRepository.findByCourse_IdOrderBySortOrderAscIdAsc(courseId).stream()
                .map(lesson -> {
                    UserGrammarLessonProgress row = progress.get(lesson.getId());
                    return new GrammarLessonSummaryDto(
                            lesson.getId(),
                            lesson.getCode(),
                            lesson.getTitle(),
                            lesson.getSummary(),
                            lesson.getFormula(),
                            lesson.getDifficulty(),
                            lesson.getSourceLessonLabel(),
                            exerciseCounts.getOrDefault(lesson.getId(), 0),
                            row != null,
                            bestScores.getOrDefault(lesson.getId(), List.of()).size(),
                            averageOf(bestScores.get(lesson.getId())));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public GrammarLessonDto getLesson(User user, Long lessonId) {
        GrammarLesson lesson = requireLesson(lessonId);

        List<UserGrammarAttempt> attempts = attemptRepository
                .findByUser_IdAndExercise_Lesson_IdOrderBySubmittedAtDesc(user.getId(), lessonId);

        // Resolved once for the lesson: the hints are checked against the same vocabulary.
        Set<String> studied = studiedTerms(user.getId(), lesson.getLanguage());

        List<GrammarExerciseDto> exercises = exerciseRepository
                .findByLesson_IdOrderBySortOrderAscIdAsc(lessonId).stream()
                .map(exercise -> toDto(exercise, attempts, studied))
                .toList();

        return new GrammarLessonDto(
                lesson.getId(),
                lesson.getCourse().getId(),
                lesson.getCourse().getTitle(),
                lesson.getLanguage(),
                lesson.getCode(),
                lesson.getTitle(),
                lesson.getSummary(),
                lesson.getFormula(),
                readComponents(lesson.getComponentsJson()),
                readExamples(lesson.getExamplesJson()),
                lesson.getNotes(),
                lesson.getSourceLessonLabel(),
                lesson.getDifficulty(),
                progressRepository.findByUser_IdAndLesson_Id(user.getId(), lessonId).isPresent(),
                exercises);
    }

    // ---- Practising ----

    /**
     * Grades one drill sentence and marks the lesson studied.
     *
     * <p>The lesson counts as studied on the first submitted answer whatever it scored: the point
     * of the mark is "this learner has met the pattern", which is what the unlearned-grammar
     * warning on vocabulary practice needs to know.
     */
    @Transactional
    public SentenceAttemptDto submit(User user, Long exerciseId, SubmitGrammarAnswerRequest request) {
        GrammarExercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu bài tập"));

        GrammarLesson lesson = exercise.getLesson();
        SentenceFeedbackDto feedback = grammarCatalogService.attachGrammarRefs(
                aiContentService.gradeOneSentence(
                        lesson.getLanguage(),
                        exercise.getPromptVi(),
                        exercise.getAnswerTarget(),
                        request.answerTarget(),
                        grammarCatalogService.catalogue(lesson.getLanguage())),
                lesson.getLanguage(), exercise.getPromptVi(), request.answerTarget());

        UserGrammarAttempt attempt = attemptRepository.save(UserGrammarAttempt.builder()
                .user(user)
                .exercise(exercise)
                .answerTarget(request.answerTarget().trim())
                .score(feedback.score())
                .feedbackJson(writeJson(feedback))
                .build());

        markStudied(user, lesson);
        return toAttemptDto(attempt);
    }

    private void markStudied(User user, GrammarLesson lesson) {
        UserGrammarLessonProgress progress = progressRepository
                .findByUser_IdAndLesson_Id(user.getId(), lesson.getId())
                .orElseGet(() -> UserGrammarLessonProgress.builder().user(user).lesson(lesson).build());
        progress.setExercisesDone(progress.getExercisesDone() + 1);
        progress.setLastPractisedAt(Instant.now());
        progressRepository.save(progress);
    }

    // ---- Admin ----

    @Transactional(readOnly = true)
    public List<GrammarLessonSummaryDto> listForAdmin(Long courseId) {
        Map<Long, Integer> exerciseCounts = new HashMap<>();
        for (GrammarExercise exercise : exerciseRepository.findByLesson_Course_Id(courseId)) {
            exerciseCounts.merge(exercise.getLesson().getId(), 1, Integer::sum);
        }
        return lessonRepository.findByCourse_IdOrderBySortOrderAscIdAsc(courseId).stream()
                .map(lesson -> new GrammarLessonSummaryDto(
                        lesson.getId(), lesson.getCode(), lesson.getTitle(), lesson.getSummary(),
                        lesson.getFormula(), lesson.getDifficulty(), lesson.getSourceLessonLabel(),
                        exerciseCounts.getOrDefault(lesson.getId(), 0), false, 0, null))
                .toList();
    }

    @Transactional
    public GrammarLessonSummaryDto create(Long courseId, SaveGrammarLessonRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoá học"));
        if (course.getKind() != CourseKind.GRAMMAR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ khoá học loại GRAMMAR mới chứa được bài ngữ pháp");
        }
        String code = request.code().trim();
        if (lessonRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã ngữ pháp '" + code + "' đã tồn tại");
        }

        GrammarLesson lesson = GrammarLesson.builder()
                .course(course)
                .code(code)
                .title(request.title().trim())
                .summary(blankToNull(request.summary()))
                .formula(request.formula().trim())
                .componentsJson(writeJson(request.components()))
                .examplesJson(writeJson(tidyExamples(request.examples())))
                .notes(blankToNull(request.notes()))
                .sourceLessonLabel(blankToNull(request.sourceLessonLabel()))
                .difficulty(request.difficultyOrDefault())
                .sortOrder(request.sortOrder() == null ? nextSortOrder(courseId) : request.sortOrder())
                .build();

        GrammarLesson saved = lessonRepository.save(lesson);
        return new GrammarLessonSummaryDto(
                saved.getId(), saved.getCode(), saved.getTitle(), saved.getSummary(), saved.getFormula(),
                saved.getDifficulty(), saved.getSourceLessonLabel(), 0, false, 0, null);
    }

    @Transactional
    public GrammarLessonSummaryDto update(Long lessonId, SaveGrammarLessonRequest request) {
        GrammarLesson lesson = requireLesson(lessonId);
        String code = request.code().trim();
        if (!code.equals(lesson.getCode()) && lessonRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã ngữ pháp '" + code + "' đã tồn tại");
        }

        lesson.setCode(code);
        lesson.setTitle(request.title().trim());
        lesson.setSummary(blankToNull(request.summary()));
        lesson.setFormula(request.formula().trim());
        lesson.setComponentsJson(writeJson(request.components()));
        lesson.setExamplesJson(writeJson(tidyExamples(request.examples())));
        lesson.setNotes(blankToNull(request.notes()));
        lesson.setSourceLessonLabel(blankToNull(request.sourceLessonLabel()));
        lesson.setDifficulty(request.difficultyOrDefault());
        if (request.sortOrder() != null) {
            lesson.setSortOrder(request.sortOrder());
        }

        GrammarLesson saved = lessonRepository.save(lesson);
        return new GrammarLessonSummaryDto(
                saved.getId(), saved.getCode(), saved.getTitle(), saved.getSummary(), saved.getFormula(),
                saved.getDifficulty(), saved.getSourceLessonLabel(),
                (int) exerciseRepository.countByLesson_Id(lessonId), false, 0, null);
    }

    /** Deletes a lesson with everything hanging off it: attempts, progress, then the drills. */
    @Transactional
    public void delete(Long lessonId) {
        attemptRepository.deleteByExercise_Lesson_Id(lessonId);
        exerciseRepository.deleteByLesson_Id(lessonId);
        progressRepository.deleteByLesson_Id(lessonId);
        lessonRepository.deleteById(lessonId);
    }

    /** Admin view: no attempts, and the reference answer always visible for proof-reading. */
    @Transactional(readOnly = true)
    public List<GrammarExerciseDto> listExercisesForAdmin(Long lessonId) {
        return exerciseRepository.findByLesson_IdOrderBySortOrderAscIdAsc(lessonId).stream()
                .map(exercise -> new GrammarExerciseDto(
                        exercise.getId(),
                        exercise.getSortOrder(),
                        exercise.getPromptVi(),
                        exercise.getCaseLabel(),
                        readNewWords(exercise.getNewWordsJson()),
                        exercise.getAnswerTarget(),
                        exercise.getAnswerPhonetic(),
                        null,
                        null))
                .toList();
    }

    /**
     * Generates drill sentences for one lesson. The count defaults to the lesson's own target, so
     * a harder pattern with more cases gets more practice — 5 sentences at difficulty 1, 25 at 5.
     */
    @Transactional
    public List<GrammarExerciseDto> generateExercises(Long lessonId, GenerateGrammarExercisesRequest request) {
        GrammarLesson lesson = requireLesson(lessonId);
        int count = request.count() == null ? lesson.targetExerciseCount() : request.count();

        GeneratedSentencesDto generated = aiContentService.generateGrammarExercises(
                lesson.getLanguage(),
                lesson.getTitle(),
                lesson.getFormula(),
                describeComponents(lesson),
                describeExamples(lesson),
                count);

        if (request.replaceOrDefault()) {
            attemptRepository.deleteByExercise_Lesson_Id(lessonId);
            exerciseRepository.deleteByLesson_Id(lessonId);
        }

        int nextOrder = request.replaceOrDefault() ? 0 : (int) exerciseRepository.countByLesson_Id(lessonId);
        List<GrammarExercise> rows = new ArrayList<>();
        for (GeneratedSentencesDto.GeneratedSentenceDto item : generated.sentences()) {
            if (isBlank(item.promptVi()) || isBlank(item.answerTarget())) {
                continue;
            }
            rows.add(GrammarExercise.builder()
                    .lesson(lesson)
                    .promptVi(item.promptVi().trim())
                    .answerTarget(CjkText.tidy(item.answerTarget()))
                    .answerPhonetic(blankToNull(item.answerPhonetic()))
                    .caseLabel(blankToNull(item.caseLabel()))
                    .newWordsJson(writeJson(item.newWords()))
                    .sortOrder(nextOrder++)
                    .build());
        }
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI không sinh được câu bài tập nào");
        }

        exerciseRepository.saveAll(rows);
        return listExercisesForAdmin(lessonId);
    }

    @Transactional
    public void deleteExercise(Long exerciseId) {
        attemptRepository.deleteByExercise_Id(exerciseId);
        exerciseRepository.deleteById(exerciseId);
    }

    // ---- Helpers ----

    private GrammarLesson requireLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài ngữ pháp"));
    }

    private int nextSortOrder(Long courseId) {
        return lessonRepository.findByCourse_IdOrderBySortOrderAscIdAsc(courseId).stream()
                .mapToInt(GrammarLesson::getSortOrder)
                .max()
                .orElse(-1) + 1;
    }

    /** The reference answer stays hidden until the learner has actually tried. */
    /** Null when nothing has a score yet — an ungraded answer (AI unavailable) has none. */
    private static Double averageOf(List<Integer> bestScores) {
        if (bestScores == null) {
            return null;
        }
        return bestScores.stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream().boxed().findFirst().orElse(null);
    }

    /** Words this learner has already practised, so hints about them can be dropped. */
    private Set<String> studiedTerms(Long userId, Language language) {
        return wordProgressRepository.findStudiedTerms(userId, language).stream()
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private GrammarExerciseDto toDto(
            GrammarExercise exercise, List<UserGrammarAttempt> lessonAttempts, Set<String> studied) {
        List<UserGrammarAttempt> mine = lessonAttempts.stream()
                .filter(attempt -> attempt.getExercise().getId().equals(exercise.getId()))
                .toList();

        SentenceAttemptDto last = mine.isEmpty() ? null : toAttemptDto(mine.get(0));
        Integer best = mine.stream()
                .map(UserGrammarAttempt::getScore)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new GrammarExerciseDto(
                exercise.getId(),
                exercise.getSortOrder(),
                exercise.getPromptVi(),
                exercise.getCaseLabel(),
                unknownWords(exercise.getNewWordsJson(), studied),
                last == null ? null : exercise.getAnswerTarget(),
                last == null ? null : exercise.getAnswerPhonetic(),
                last,
                best);
    }

    /**
     * The hints for words this learner has not met. Everything else is noise: the AI marked the
     * word hard when the exercise was written, without knowing whose screen it would appear on.
     */
    private List<NewWordHintDto> unknownWords(String newWordsJson, Set<String> studied) {
        List<NewWordHintDto> hints = readNewWords(newWordsJson);
        if (studied == null || studied.isEmpty()) {
            return hints;
        }
        return hints.stream()
                .filter(hint -> hint.term() == null || !studied.contains(hint.term().trim()))
                .toList();
    }

    private SentenceAttemptDto toAttemptDto(UserGrammarAttempt attempt) {
        return new SentenceAttemptDto(
                attempt.getId(),
                attempt.getExercise().getId(),
                attempt.getAnswerTarget(),
                attempt.getScore(),
                readFeedback(attempt.getFeedbackJson()),
                attempt.getSubmittedAt());
    }

    /**
     * Strips the spaces that creep between Chinese characters — the PDF export leaves them in
     * slide text, and the model copies that habit into sentences it writes itself.
     */
    private static List<GrammarContentDto.ExampleDto> tidyExamples(
            List<GrammarContentDto.ExampleDto> examples) {
        if (examples == null) {
            return List.of();
        }
        return examples.stream()
                .map(e -> new GrammarContentDto.ExampleDto(
                        CjkText.tidy(e.target()), e.phonetic(), e.vi(), e.note()))
                .toList();
    }

    private String describeComponents(GrammarLesson lesson) {
        List<GrammarContentDto.ComponentDto> components = readComponents(lesson.getComponentsJson());
        if (components.isEmpty()) {
            return "(không có)";
        }
        return components.stream()
                .map(c -> c.part() + " = " + c.meaning())
                .reduce((a, b) -> a + "; " + b)
                .orElse("(không có)");
    }

    private String describeExamples(GrammarLesson lesson) {
        List<GrammarContentDto.ExampleDto> examples = readExamples(lesson.getExamplesJson());
        if (examples.isEmpty()) {
            return "(không có)";
        }
        return examples.stream()
                .map(e -> e.target() + " — " + e.vi())
                .reduce((a, b) -> a + " | " + b)
                .orElse("(không có)");
    }

    // ---- JSON columns ----

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Could not serialise grammar JSON", ex);
            return null;
        }
    }

    private List<GrammarContentDto.ComponentDto> readComponents(String json) {
        return readList(json, new TypeReference<List<GrammarContentDto.ComponentDto>>() {});
    }

    private List<GrammarContentDto.ExampleDto> readExamples(String json) {
        return readList(json, new TypeReference<List<GrammarContentDto.ExampleDto>>() {});
    }

    private List<NewWordHintDto> readNewWords(String json) {
        return readList(json, new TypeReference<List<NewWordHintDto>>() {});
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("Could not read grammar JSON column", ex);
            return List.of();
        }
    }

    private SentenceFeedbackDto readFeedback(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SentenceFeedbackDto.class);
        } catch (Exception ex) {
            log.warn("Could not read grammar feedback JSON", ex);
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
