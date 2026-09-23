package com.learn.learnE_Backend.skipahead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.learnE_Backend.ai.AiContentService;
import com.learn.learnE_Backend.ai.GeminiClient;
import com.learn.learnE_Backend.grammar.GrammarCatalogService;
import com.learn.learnE_Backend.ai.dto.GeneratedSentencesDto;
import com.learn.learnE_Backend.ai.dto.GradedSentencesDto;
import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.sentence.SentenceExercise;
import com.learn.learnE_Backend.sentence.SentenceExerciseRepository;
import com.learn.learnE_Backend.sentence.UserSentenceAttempt;
import com.learn.learnE_Backend.sentence.UserSentenceAttemptRepository;
import com.learn.learnE_Backend.sentence.dto.SentenceFeedbackDto;
import com.learn.learnE_Backend.skipahead.dto.*;
import com.learn.learnE_Backend.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Thi vượt": instead of waiting a day per lesson, a learner may sit a translation test covering
 * everything before a chosen day and jump straight there.
 *
 * <p>Questions are drawn from practice sentences the learner has never seen, topped up by the AI
 * when the pool runs short. Each question may be answered once, and nothing is graded or revealed
 * until the whole paper is handed in — grading the lot in one AI request.
 */
@Service
public class SkipAheadService {

    private static final Logger log = LoggerFactory.getLogger(SkipAheadService.class);

    public static final int REQUIRED_SENTENCES = 20;
    public static final double MIN_AVERAGE = 8.0;
    /** Breathing space after a failed sitting, so the test cannot be brute-forced. */
    public static final Duration RETRY_COOLDOWN = Duration.ofHours(3);

    /** Weighting of the question mix: recent material dominates, with some older revision. */
    private static final int RECENT_BAND_DAYS = 10;
    private static final int RECENT_QUOTA = 12;
    private static final int MIDDLE_QUOTA = 5;
    private static final int EARLY_QUOTA = 3;
    /** Vocabulary handed to the generator; enough for variety without a huge prompt. */
    private static final int MAX_PROMPT_WORDS_PER_BAND = 150;

    private final SkipAheadTestRepository testRepository;
    private final SentenceExerciseRepository exerciseRepository;
    private final UserSentenceAttemptRepository attemptRepository;
    private final LessonDayRepository lessonDayRepository;
    private final WordRepository wordRepository;
    private final CourseRepository courseRepository;
    private final UserCourseEnrollmentRepository enrollmentRepository;
    private final AiContentService aiContentService;
    private final GrammarCatalogService grammarCatalogService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SkipAheadService(
            SkipAheadTestRepository testRepository,
            SentenceExerciseRepository exerciseRepository,
            UserSentenceAttemptRepository attemptRepository,
            LessonDayRepository lessonDayRepository,
            WordRepository wordRepository,
            CourseRepository courseRepository,
            UserCourseEnrollmentRepository enrollmentRepository,
            AiContentService aiContentService,
            GrammarCatalogService grammarCatalogService,
            GeminiClient geminiClient
    ) {
        this.testRepository = testRepository;
        this.exerciseRepository = exerciseRepository;
        this.attemptRepository = attemptRepository;
        this.lessonDayRepository = lessonDayRepository;
        this.wordRepository = wordRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.aiContentService = aiContentService;
        this.grammarCatalogService = grammarCatalogService;
        this.geminiClient = geminiClient;
    }

    // ---- Eligibility ----

    @Transactional(readOnly = true)
    public SkipAheadStatusDto status(User user, Long courseId, int targetDayNumber) {
        requireChineseCourse(courseId);
        // Sitting an exam for a course you never enrolled in makes no sense, and starting one can
        // spend an AI call, so the check belongs here rather than only at the point of unlocking.
        enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Bạn chưa đăng ký khoá học này"));
        List<LessonDay> priorDays = daysBefore(courseId, targetDayNumber);
        int pool = unseenPool(user.getId(), priorDays).size();

        var open = testRepository.findByUser_IdAndFinishedAtIsNull(user.getId());
        var last = testRepository
                .findFirstByUser_IdAndCourse_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(user.getId(), courseId);

        Instant retryAfter = last.filter(t -> !Boolean.TRUE.equals(t.getPassed()))
                .map(t -> t.getFinishedAt().plus(RETRY_COOLDOWN))
                .filter(Instant.now()::isBefore)
                .orElse(null);

        String reason = null;
        if (priorDays.isEmpty()) {
            reason = "Ngày này không có ngày học nào phía trước để kiểm tra";
        } else if (open.isPresent()) {
            reason = open.get().getTargetDayNumber() == targetDayNumber
                    ? "Bạn đang có một bài thi dở — hãy hoàn thành nó trước"
                    : "Bạn đang thi vượt tới ngày %d — hoàn thành bài đó trước"
                            .formatted(open.get().getTargetDayNumber());
        } else if (retryAfter != null) {
            reason = "Vừa thi trượt, phải chờ %d giờ mới được thi lại".formatted(RETRY_COOLDOWN.toHours());
        } else if (!geminiClient.isConfigured() && pool < REQUIRED_SENTENCES) {
            reason = "Chưa đủ %d câu trong kho (%d) và chưa cấu hình AI để sinh thêm"
                    .formatted(REQUIRED_SENTENCES, pool);
        }

        return new SkipAheadStatusDto(
                courseId,
                targetDayNumber,
                reason == null,
                reason,
                retryAfter,
                open.map(SkipAheadTest::getId).orElse(null),
                REQUIRED_SENTENCES,
                MIN_AVERAGE,
                pool,
                last.map(SkipAheadTest::getPassed).orElse(null),
                last.map(t -> toDouble(t.getAverageScore())).orElse(null)
        );
    }

    // ---- Sitting the test ----

    @Transactional
    public SkipAheadTestDto start(User user, Long courseId, int targetDayNumber) {
        // An unfinished paper for this very target is resumed rather than replaced, so a refresh
        // does not cost the answers already given.
        var open = testRepository.findByUser_IdAndFinishedAtIsNull(user.getId());
        if (open.isPresent()
                && open.get().getCourse().getId().equals(courseId)
                && open.get().getTargetDayNumber() == targetDayNumber) {
            return toDto(open.get());
        }

        SkipAheadStatusDto status = status(user, courseId, targetDayNumber);
        if (!status.canStart()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, status.reason());
        }

        Course course = requireChineseCourse(courseId);
        List<LessonDay> priorDays = daysBefore(courseId, targetDayNumber);

        SkipAheadTest test = SkipAheadTest.builder()
                .user(user)
                .course(course)
                .targetDayNumber(targetDayNumber)
                .requiredSentences(REQUIRED_SENTENCES)
                .minAverage(BigDecimal.valueOf(MIN_AVERAGE))
                .build();

        List<SkipAheadTestItem> items = buildQuestions(user, course, priorDays, targetDayNumber, test);
        if (items.size() < REQUIRED_SENTENCES) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chỉ dựng được %d/%d câu cho đề thi — thử lại sau".formatted(items.size(), REQUIRED_SENTENCES));
        }
        test.getItems().addAll(items);
        return toDto(testRepository.save(test));
    }

    /**
     * Hybrid sourcing: unseen practice sentences first (free, already proof-read), weighted towards
     * the days just before the target, then AI-generated questions to make up the number.
     */
    private List<SkipAheadTestItem> buildQuestions(
            User user, Course course, List<LessonDay> priorDays, int targetDayNumber, SkipAheadTest test) {

        List<SentenceExercise> pool = unseenPool(user.getId(), priorDays);
        int recentFrom = Math.max(1, targetDayNumber - RECENT_BAND_DAYS);
        int middleFrom = Math.max(1, recentFrom - Math.max(1, (recentFrom - 1) / 2));

        List<SentenceExercise> recent = bandOf(pool, recentFrom, targetDayNumber - 1);
        List<SentenceExercise> middle = bandOf(pool, middleFrom, recentFrom - 1);
        List<SentenceExercise> early = bandOf(pool, 1, middleFrom - 1);
        Collections.shuffle(recent);
        Collections.shuffle(middle);
        Collections.shuffle(early);

        List<SentenceExercise> picked = new ArrayList<>();
        takeInto(picked, recent, RECENT_QUOTA);
        takeInto(picked, middle, MIDDLE_QUOTA);
        takeInto(picked, early, EARLY_QUOTA);
        // A short band spills over to the hardest material still available, not the easiest.
        takeInto(picked, recent, REQUIRED_SENTENCES - picked.size());
        takeInto(picked, middle, REQUIRED_SENTENCES - picked.size());
        takeInto(picked, early, REQUIRED_SENTENCES - picked.size());

        List<SkipAheadTestItem> items = new ArrayList<>();
        for (SentenceExercise exercise : picked) {
            items.add(SkipAheadTestItem.builder()
                    .test(test)
                    .promptVi(exercise.getPromptVi())
                    .answerTarget(exercise.getAnswerTarget())
                    .answerPhonetic(exercise.getAnswerPhonetic())
                    .sourceExerciseId(exercise.getId())
                    .build());
        }

        int shortfall = REQUIRED_SENTENCES - items.size();
        if (shortfall > 0) {
            items.addAll(generateQuestions(course, priorDays, recentFrom, targetDayNumber, shortfall, test));
        }

        Collections.shuffle(items);
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSortOrder(i);
        }
        return items;
    }

    private List<SkipAheadTestItem> generateQuestions(
            Course course, List<LessonDay> priorDays, int recentFrom, int targetDayNumber,
            int count, SkipAheadTest test) {

        List<String> recentWords = wordsOfDays(priorDays, recentFrom, targetDayNumber - 1);
        List<String> earlierWords = wordsOfDays(priorDays, 1, recentFrom - 1);
        String scope = "ngày %d của khoá %s".formatted(targetDayNumber, course.getTitle());

        GeneratedSentencesDto generated;
        try {
            generated = aiContentService.generateTestSentences(
                    course.getLanguage(), scope, recentWords, earlierWords, count);
        } catch (RuntimeException ex) {
            log.warn("Skip-ahead question generation failed", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không dựng được đề thi: " + ex.getMessage());
        }

        List<SkipAheadTestItem> items = new ArrayList<>();
        if (generated.sentences() == null) {
            return items;
        }
        for (var sentence : generated.sentences()) {
            if (sentence.promptVi() == null || sentence.promptVi().isBlank()
                    || sentence.answerTarget() == null || sentence.answerTarget().isBlank()) {
                continue;
            }
            items.add(SkipAheadTestItem.builder()
                    .test(test)
                    .promptVi(sentence.promptVi().trim())
                    .answerTarget(sentence.answerTarget().trim())
                    .answerPhonetic(blankToNull(sentence.answerPhonetic()))
                    .build());
            if (items.size() == count) {
                break;
            }
        }
        return items;
    }

    @Transactional(readOnly = true)
    public SkipAheadTestDto get(User user, Long testId) {
        return toDto(requireOwnTest(user, testId));
    }

    /** One shot per question: an answered item is never overwritten. */
    @Transactional
    public SkipAheadTestDto submitAnswer(User user, Long testId, Long itemId, String answer) {
        SkipAheadTest test = requireOwnTest(user, testId);
        if (test.isFinished()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài thi đã nộp rồi");
        }
        SkipAheadTestItem item = test.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi"));
        if (item.isAnswered()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Câu này đã trả lời, không sửa lại được");
        }

        item.setAnswerSubmitted(answer.trim());
        item.setSubmittedAt(Instant.now());
        return toDto(testRepository.save(test));
    }

    /** Grades every question in one AI call, then unlocks the target day if the paper passed. */
    @Transactional
    public SkipAheadTestDto finish(User user, Long testId) {
        SkipAheadTest test = requireOwnTest(user, testId);
        if (test.isFinished()) {
            return toDto(test);
        }
        long unanswered = test.getItems().stream().filter(i -> !i.isAnswered()).count();
        if (unanswered > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Còn %d câu chưa trả lời".formatted(unanswered));
        }

        grade(test);

        double average = test.getItems().stream()
                .map(SkipAheadTestItem::getScore)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        boolean passed = average >= MIN_AVERAGE;

        test.setAverageScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        test.setPassed(passed);
        test.setFinishedAt(Instant.now());

        if (passed) {
            unlockUpTo(user, test.getCourse().getId(), test.getTargetDayNumber());
        }
        return toDto(testRepository.save(test));
    }

    private void grade(SkipAheadTest test) {
        if (!geminiClient.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình GEMINI_API_KEY nên không chấm được bài thi");
        }

        StringBuilder paper = new StringBuilder();
        List<SkipAheadTestItem> items = test.getItems();
        for (int i = 0; i < items.size(); i++) {
            SkipAheadTestItem item = items.get(i);
            paper.append("Câu ").append(i + 1).append(":\n")
                    .append("  Đề: ").append(item.getPromptVi()).append("\n")
                    .append("  Tham khảo: ").append(item.getAnswerTarget()).append("\n")
                    .append("  Bài làm: ").append(item.getAnswerSubmitted()).append("\n\n");
        }

        GradedSentencesDto graded;
        try {
            Language language = test.getCourse().getLanguage();
            graded = aiContentService.gradeSentenceBatch(
                    language, paper.toString(), grammarCatalogService.catalogue(language));
        } catch (RuntimeException ex) {
            log.warn("Skip-ahead grading failed", ex);
            // The test stays open so the learner can hand it in again without losing their answers.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không chấm được bài thi: " + ex.getMessage() + " — bài làm vẫn được giữ, hãy thử nộp lại");
        }

        var results = graded.results() == null ? List.<GradedSentencesDto.GradedSentenceDto>of() : graded.results();
        for (int i = 0; i < items.size(); i++) {
            SkipAheadTestItem item = items.get(i);
            final int position = i + 1;
            final var byPosition = results.size() > i ? results.get(i) : null;
            var result = results.stream()
                    .filter(r -> r.index() != null && r.index() == position)
                    .findFirst()
                    .orElse(byPosition);

            if (result == null || result.score() == null) {
                // A missing verdict must not silently pass: treat it as zero and say so.
                item.setScore(0);
                item.setFeedbackJson(writeJson(SentenceFeedbackDto.unavailable("AI không trả về kết quả cho câu này")));
                continue;
            }
            item.setScore(Math.max(0, Math.min(10, result.score())));
            item.setFeedbackJson(writeJson(grammarCatalogService.attachGrammarRefs(
                    new SentenceFeedbackDto(
                    result.score(),
                    result.comment(),
                    result.corrections() == null ? List.of() : result.corrections().stream()
                            .map(c -> new SentenceFeedbackDto.SentenceCorrectionDto(
                                    c.original(), c.suggestion(), c.explanation(),
                                    c.grammarCode(), c.grammarName(), null))
                            .toList(),
                    blankToNull(result.betterVersion()),
                    null),
                    test.getCourse().getLanguage(), item.getPromptVi(), item.getAnswerSubmitted())));
        }
    }

    private void unlockUpTo(User user, Long courseId, int targetDayNumber) {
        var enrollment = enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa đăng ký khoá học này"));
        enrollment.setSkipAheadDayNumber(Math.max(enrollment.getSkipAheadDayNumber(), targetDayNumber));
        enrollment.setCurrentDayNumber(Math.max(enrollment.getCurrentDayNumber(), targetDayNumber));
        enrollmentRepository.save(enrollment);
    }

    // ---- Helpers ----

    private Course requireChineseCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoá học"));
        if (course.getLanguage() != Language.CHINESE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thi vượt chỉ áp dụng cho khoá tiếng Trung");
        }
        return course;
    }

    private List<LessonDay> daysBefore(Long courseId, int targetDayNumber) {
        return lessonDayRepository.findByCourse_IdOrderByDayNumberAsc(courseId).stream()
                .filter(d -> d.getDayNumber() < targetDayNumber)
                .toList();
    }

    /** Sentences from the given days that this learner has never attempted. */
    private List<SentenceExercise> unseenPool(Long userId, List<LessonDay> days) {
        if (days.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = attemptRepository.findByUser_IdOrderBySubmittedAtDesc(userId).stream()
                .map(UserSentenceAttempt::getExercise)
                .map(SentenceExercise::getId)
                .collect(Collectors.toCollection(HashSet::new));
        // Questions from an earlier sitting count as seen too, so a retake is not the same paper.
        seen.addAll(testRepository.findUsedSourceExerciseIds(userId));

        return exerciseRepository.findByLessonDay_IdIn(days.stream().map(LessonDay::getId).toList()).stream()
                .filter(e -> !seen.contains(e.getId()))
                .toList();
    }

    private static List<SentenceExercise> bandOf(List<SentenceExercise> pool, int fromDay, int toDay) {
        if (toDay < fromDay) {
            return new ArrayList<>();
        }
        return pool.stream()
                .filter(e -> {
                    int day = e.getLessonDay().getDayNumber();
                    return day >= fromDay && day <= toDay;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static void takeInto(List<SentenceExercise> target, List<SentenceExercise> source, int howMany) {
        for (int i = 0; i < howMany && !source.isEmpty(); i++) {
            target.add(source.remove(source.size() - 1));
        }
    }

    /** "学生 (xuésheng) = học sinh" for each word in the day range, newest first, capped. */
    private List<String> wordsOfDays(List<LessonDay> days, int fromDay, int toDay) {
        if (toDay < fromDay) {
            return List.of();
        }
        List<String> words = new ArrayList<>();
        for (LessonDay day : days) {
            if (day.getDayNumber() < fromDay || day.getDayNumber() > toDay) {
                continue;
            }
            for (Word word : wordRepository.findByLessonDay_Id(day.getId())) {
                StringBuilder text = new StringBuilder(word.getTerm());
                if (word.getPhonetic() != null && !word.getPhonetic().isBlank()) {
                    text.append(" (").append(word.getPhonetic()).append(")");
                }
                words.add(text.append(" = ").append(word.getVietnameseMeaning()).toString());
            }
        }
        // Keep the tail (closest to the target day) when the range is long.
        return words.size() <= MAX_PROMPT_WORDS_PER_BAND
                ? words
                : words.subList(words.size() - MAX_PROMPT_WORDS_PER_BAND, words.size());
    }

    private SkipAheadTest requireOwnTest(User user, Long testId) {
        SkipAheadTest test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài thi"));
        if (!test.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bài thi này không phải của bạn");
        }
        return test;
    }

    private SkipAheadTestDto toDto(SkipAheadTest test) {
        boolean finished = test.isFinished();
        List<SkipAheadItemDto> items = test.getItems().stream()
                .sorted(java.util.Comparator.comparingInt(SkipAheadTestItem::getSortOrder))
                // Scores, feedback and reference answers stay hidden until the paper is handed in.
                .map(item -> new SkipAheadItemDto(
                        item.getId(),
                        item.getSortOrder(),
                        item.getPromptVi(),
                        item.isAnswered(),
                        item.getAnswerSubmitted(),
                        finished ? item.getScore() : null,
                        finished ? readFeedback(item.getFeedbackJson()) : null,
                        finished ? item.getAnswerTarget() : null,
                        finished ? item.getAnswerPhonetic() : null))
                .toList();

        return new SkipAheadTestDto(
                test.getId(),
                test.getCourse().getId(),
                test.getTargetDayNumber(),
                test.getRequiredSentences(),
                test.getMinAverage().doubleValue(),
                test.getStartedAt(),
                finished,
                test.getPassed(),
                toDouble(test.getAverageScore()),
                (int) test.getItems().stream().filter(SkipAheadTestItem::isAnswered).count(),
                items
        );
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String writeJson(SentenceFeedbackDto feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize skip-ahead feedback", ex);
        }
    }

    private SentenceFeedbackDto readFeedback(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, SentenceFeedbackDto.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
