package com.learn.learnE_Backend.vocabulary;

import com.learn.learnE_Backend.ai.AiContentService;
import com.learn.learnE_Backend.ai.dto.GeneratedDayWordsDto;
import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.sentence.SentenceService;
import com.learn.learnE_Backend.sentence.dto.DaySentenceStatsDto;
import com.learn.learnE_Backend.vocabulary.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VocabularyService {

    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;
    private final LessonDayRepository lessonDayRepository;
    private final WordRepository wordRepository;
    private final UserCourseEnrollmentRepository enrollmentRepository;
    private final UserLessonDayProgressRepository dayProgressRepository;
    private final UserWordProgressRepository wordProgressRepository;
    private final AiContentService aiContentService;
    private final SentenceService sentenceService;

    public VocabularyService(
            TopicRepository topicRepository,
            CourseRepository courseRepository,
            LessonDayRepository lessonDayRepository,
            WordRepository wordRepository,
            UserCourseEnrollmentRepository enrollmentRepository,
            UserLessonDayProgressRepository dayProgressRepository,
            UserWordProgressRepository wordProgressRepository,
            AiContentService aiContentService,
            SentenceService sentenceService
    ) {
        this.aiContentService = aiContentService;
        this.sentenceService = sentenceService;
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
        this.lessonDayRepository = lessonDayRepository;
        this.wordRepository = wordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.dayProgressRepository = dayProgressRepository;
        this.wordProgressRepository = wordProgressRepository;
    }

    /**
     * @param language optional filter; {@code null} returns every language.
     * @param kind     which kind of course to list; never null, so grammar courses cannot leak
     *                 into a caller that only knows about vocabulary.
     */
    @Transactional(readOnly = true)
    public List<CourseSummaryDto> listCourses(Long userId, Language language, CourseKind kind) {
        Set<Long> enrolledCourseIds = enrollmentRepository.findByUser_IdAndCompletedAtIsNull(userId).stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());

        return courseRepository.findAll().stream()
                .filter(course -> course.getKind() == kind)
                .filter(course -> language == null || course.getLanguage() == language)
                .sorted(java.util.Comparator
                        .comparing(Course::getSortOrder)
                        .thenComparing(Course::getId))
                .map(course -> CourseSummaryDto.from(course, enrolledCourseIds.contains(course.getId())))
                .toList();
    }

    /**
     * One course on its own, for screens that arrive holding nothing but an id.
     *
     * <p>The day list reaches the learner from two places — the course list and the dashboard's
     * "continue" card — and only one of them has the course to hand. Fetching by id keeps both
     * entry points identical instead of threading the course's settings through every screen.
     */
    @Transactional(readOnly = true)
    public CourseSummaryDto getCourse(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoá học"));
        boolean enrolled = enrollmentRepository.findByUser_IdAndCourse_Id(userId, courseId)
                .filter(e -> e.getCompletedAt() == null)
                .isPresent();
        return CourseSummaryDto.from(course, enrolled);
    }

    @Transactional
    public EnrollmentDto enroll(User user, Long courseId) {
        return enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), courseId)
                .map(EnrollmentDto::from)
                .orElseGet(() -> {
                    Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
                    UserCourseEnrollment enrollment = UserCourseEnrollment.builder()
                            .user(user)
                            .course(course)
                            .build();
                    return EnrollmentDto.from(enrollmentRepository.save(enrollment));
                });
    }

    /** @param language optional filter; {@code null} returns every language. */
    @Transactional(readOnly = true)
    public List<EnrollmentSessionDto> getToday(User user, Language language) {
        LocalDate today = LocalDate.now();
        List<UserCourseEnrollment> enrollments = enrollmentRepository.findByUser_IdAndCompletedAtIsNull(user.getId());

        return enrollments.stream()
                .filter(e -> language == null || e.getCourse().getLanguage() == language)
                .map(enrollment -> buildSession(user, enrollment, today))
                .toList();
    }

    private EnrollmentSessionDto buildSession(User user, UserCourseEnrollment enrollment, LocalDate today) {
        Course course = enrollment.getCourse();
        List<CourseDayDto> days = listCourseDays(user, course.getId());
        int totalWords = days.stream().mapToInt(CourseDayDto::wordCount).sum();
        int masteredWords = days.stream().mapToInt(CourseDayDto::masteredWordCount).sum();

        var currentDayOpt = days.stream()
                .filter(d -> d.dayNumber() == enrollment.getCurrentDayNumber())
                .findFirst();

        if (currentDayOpt.isEmpty()) {
            return new EnrollmentSessionDto(
                    enrollment.getId(), course.getId(), course.getTitle(),
                    enrollment.getCurrentDayNumber(), null, true, true, false, null,
                    enrollment.currentStreak(LocalDate.now()), 0, 0, masteredWords, totalWords
            );
        }

        CourseDayDto currentDay = currentDayOpt.get();
        Set<Long> currentDayWordIds = wordRepository.findByLessonDay_Id(currentDay.lessonDayId()).stream()
                .map(Word::getId)
                .collect(Collectors.toSet());

        // Only this course's language counts, so the two learning tracks stay independent.
        Language language = course.getLanguage();
        int dueReviewCount = (int) wordProgressRepository
                .findByUser_IdAndNextReviewDateLessThanEqual(user.getId(), today).stream()
                .map(UserWordProgress::getWord)
                .filter(word -> word.getLanguage() == language)
                .filter(word -> !currentDayWordIds.contains(word.getId()))
                .count();

        return new EnrollmentSessionDto(
                enrollment.getId(), course.getId(), course.getTitle(),
                currentDay.dayNumber(), currentDay.title(), false, currentDay.completed(),
                currentDay.unlocked(), currentDay.lockedReason(),
                enrollment.currentStreak(LocalDate.now()),
                currentDay.wordCount() - currentDay.masteredWordCount(), dueReviewCount,
                masteredWords, totalWords
        );
    }

    @Transactional
    public ReviewResponse submitReview(User user, ReviewRequest request) {
        Word word = wordRepository.findById(request.wordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));

        // Locked days must not be practisable through the API either.
        String lockedReason = lockReason(user.getId(), word.getLessonDay());
        if (lockedReason != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, lockedReason);
        }

        UserWordProgress progress = wordProgressRepository.findByUser_IdAndWord_Id(user.getId(), word.getId())
                .orElseGet(() -> UserWordProgress.builder()
                        .user(user)
                        .word(word)
                        .nextReviewDate(LocalDate.now())
                        .build());

        Sm2Calculator.Result result = Sm2Calculator.compute(
                progress.getEaseFactor(), progress.getIntervalDays(), progress.getRepetitions(), request.quality().score()
        );

        progress.setEaseFactor(result.easeFactor());
        progress.setIntervalDays(result.intervalDays());
        progress.setRepetitions(result.repetitions());
        progress.setNextReviewDate(LocalDate.now().plusDays(result.intervalDays()));
        progress.setLastReviewedAt(java.time.Instant.now());

        // Mastery must be earned inside a single session, so ticks from an earlier run don't carry over.
        if (!request.sessionId().equals(progress.getMasterySessionId())) {
            progress.startSession(request.sessionId());
        }

        // Anything other than "forgot/wrong" still counts as recalling the word correctly.
        boolean answeredCorrectly = request.quality() != ReviewQuality.AGAIN;
        progress.setModeMastered(request.mode(), answeredCorrectly);
        Course course = word.getLessonDay().getCourse();
        if (progress.isAllModesCorrectThisSession(LanguageModes.requiredFor(course))) {
            progress.setMastered(true);
        }

        wordProgressRepository.save(progress);

        boolean justCompleted = tryCompleteLessonDay(user, word.getLessonDay());

        return new ReviewResponse(word.getId(), result.intervalDays(), progress.getNextReviewDate(), justCompleted);
    }

    private boolean tryCompleteLessonDay(User user, LessonDay lessonDay) {
        Course course = lessonDay.getCourse();
        var enrollmentOpt = enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), course.getId());
        if (enrollmentOpt.isEmpty()) {
            return false;
        }

        UserCourseEnrollment enrollment = enrollmentOpt.get();
        // Re-studying a finished day is always allowed, but it doesn't re-trigger completion.
        if (dayProgressRepository.existsByUser_IdAndLessonDay_Id(user.getId(), lessonDay.getId())) {
            return false;
        }

        long totalWords = wordRepository.countByLessonDay_Id(lessonDay.getId());
        long doneWords = wordProgressRepository.countByUser_IdAndWord_LessonDay_Id(user.getId(), lessonDay.getId());
        if (doneWords < totalWords) {
            return false;
        }

        dayProgressRepository.save(UserLessonDayProgress.builder()
                .user(user)
                .lessonDay(lessonDay)
                .build());

        LocalDate today = LocalDate.now();
        LocalDate lastStudyDate = enrollment.getLastStudyDate();
        if (lastStudyDate == null || lastStudyDate.equals(today.minusDays(1))) {
            enrollment.setStreakCount(enrollment.getStreakCount() + 1);
        } else if (!lastStudyDate.equals(today)) {
            enrollment.setStreakCount(1);
        }
        enrollment.setLastStudyDate(today);
        // The record only ever rises, so a broken run never costs the learner their best.
        enrollment.setLongestStreak(Math.max(enrollment.getLongestStreak(), enrollment.getStreakCount()));
        if (enrollment.getCurrentDayNumber() == lessonDay.getDayNumber()) {
            enrollment.setCurrentDayNumber(enrollment.getCurrentDayNumber() + 1);
        }
        enrollmentRepository.save(enrollment);

        return true;
    }

    // ---- Course day map (unlocking + mastery stats) ----

    @Transactional(readOnly = true)
    public List<CourseDayDto> listCourseDays(User user, Long courseId) {
        enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa đăng ký khoá học này"));

        List<LessonDay> days = lessonDayRepository.findByCourse_IdOrderByDayNumberAsc(courseId);
        LocalDate today = LocalDate.now();
        int skipAheadDay = skipAheadDayNumber(user.getId(), courseId);
        // Fetched once for the whole course rather than per day; see SentenceService.statsForCourse.
        var sentenceStats = sentenceService.statsForCourse(user.getId(), courseId);

        List<CourseDayDto> result = new java.util.ArrayList<>();

        for (LessonDay day : days) {
            List<Word> words = wordRepository.findByLessonDay_Id(day.getId());
            int masteredCount = countMasteredWords(user.getId(), words);

            var dayProgress = dayProgressRepository.findByUser_IdAndLessonDay_Id(user.getId(), day.getId());
            boolean completed = dayProgress.isPresent();
            LocalDate completedOn = dayProgress.map(p -> toLocalDate(p.getCompletedAt())).orElse(null);

            String lockedReason = lockReason(user.getId(), day, skipAheadDay);

            boolean needsReview = completed
                    && completedOn != null
                    && completedOn.isBefore(today)
                    && masteredCount < words.size();

            result.add(new CourseDayDto(
                    day.getId(), day.getDayNumber(), day.getTitle(), words.size(), masteredCount,
                    completed, lockedReason == null, needsReview, completedOn, lockedReason,
                    sentenceStats.getOrDefault(day.getId(), DaySentenceStatsDto.EMPTY)
            ));
        }

        return result;
    }

    /**
     * A day opens once the previous one has been finished at least once AND the calendar has moved on.
     * Days already finished stay open forever so they can be re-studied.
     *
     * <p>Chinese courses have a way past the overnight wait: acing the previous day's translation
     * practice. See {@link #skipAheadLockReason}.
     */
    private String lockReason(Long userId, LessonDay day) {
        return lockReason(userId, day, skipAheadDayNumber(userId, day.getCourse().getId()));
    }

    /** Highest day opened by passing a skip-ahead test, 0 when there is none. */
    private int skipAheadDayNumber(Long userId, Long courseId) {
        return enrollmentRepository.findByUser_IdAndCourse_Id(userId, courseId)
                .map(UserCourseEnrollment::getSkipAheadDayNumber)
                .orElse(0);
    }

    private String lockReason(Long userId, LessonDay day, int skipAheadDayNumber) {
        // A course can open every day at once: nothing to finish first, no waiting for tomorrow.
        if (day.getCourse().isDaysAlwaysUnlocked()) {
            return null;
        }
        if (day.getDayNumber() <= 1) {
            return null;
        }
        // Passing a skip-ahead test opens the target day and everything before it, so the days
        // that were jumped over can still be studied.
        if (day.getDayNumber() <= skipAheadDayNumber) {
            return null;
        }
        if (dayProgressRepository.existsByUser_IdAndLessonDay_Id(userId, day.getId())) {
            return null;
        }

        var previousDay = lessonDayRepository.findByCourse_IdAndDayNumber(day.getCourse().getId(), day.getDayNumber() - 1);
        if (previousDay.isEmpty()) {
            return null;
        }

        var previousProgress = dayProgressRepository.findByUser_IdAndLessonDay_Id(userId, previousDay.get().getId());
        if (previousProgress.isEmpty()) {
            return "Cần hoàn thành ngày trước đó ít nhất một lần";
        }
        if (!toLocalDate(previousProgress.get().getCompletedAt()).isBefore(LocalDate.now())) {
            if (day.getCourse().getLanguage() == Language.CHINESE) {
                return skipAheadLockReason(userId, previousDay.get());
            }
            return "Ngày mai mới mở — hôm nay bạn đã học xong ngày trước rồi";
        }
        return null;
    }

    /**
     * Chinese only: finishing the previous day's translation set to a high standard unlocks the next
     * day immediately. Returns {@code null} once the bar is cleared, otherwise a message saying
     * exactly how far off it is.
     */
    private String skipAheadLockReason(Long userId, LessonDay previousDay) {
        var progress = sentenceService.skipAheadProgress(userId, previousDay.getId());
        if (progress.eligible()) {
            return null;
        }

        String base = "Ngày mai mới mở — hoặc học vượt ngay bằng bài luyện dịch ngày %d"
                .formatted(previousDay.getDayNumber());

        if (progress.available() < SentenceService.SKIP_AHEAD_SENTENCES) {
            return "%s, nhưng ngày đó mới có %d/%d câu luyện dịch".formatted(
                    base, progress.available(), SentenceService.SKIP_AHEAD_SENTENCES);
        }
        String scoreSoFar = progress.average() == null
                ? "chưa chấm câu nào"
                : "trung bình %.1f/10".formatted(progress.average());
        return "%s: cần %d câu đạt trung bình %.0f/10 (hiện %d câu, %s)".formatted(
                base,
                SentenceService.SKIP_AHEAD_SENTENCES,
                SentenceService.SKIP_AHEAD_MIN_AVERAGE,
                progress.answered(),
                scoreSoFar);
    }

    private static LocalDate toLocalDate(java.time.Instant instant) {
        return instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private int countMasteredWords(Long userId, List<Word> words) {
        int mastered = 0;
        for (Word word : words) {
            if (wordProgressRepository.findByUser_IdAndWord_Id(userId, word.getId())
                    .map(UserWordProgress::isFullyMastered)
                    .orElse(false)) {
                mastered++;
            }
        }
        return mastered;
    }

    @Transactional(readOnly = true)
    public DaySessionDto getDaySession(User user, Long lessonDayId) {
        LessonDay lessonDay = lessonDayRepository.findById(lessonDayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ngày học"));

        CourseDayDto dayInfo = listCourseDays(user, lessonDay.getCourse().getId()).stream()
                .filter(d -> d.lessonDayId().equals(lessonDayId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ngày học"));

        if (!dayInfo.unlocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, dayInfo.lockedReason());
        }

        // Resolve the language and the course's mode list once for the whole day rather than
        // per word. The same list goes to the client, so the questions it asks and the mastery
        // check below can never drift apart.
        Course course = lessonDay.getCourse();
        Language language = course.getLanguage();
        List<PracticeMode> modes = LanguageModes.requiredFor(course);

        List<PracticeWordDto> words = wordRepository.findByLessonDay_Id(lessonDayId).stream()
                .map(word -> PracticeWordDto.from(word, pendingModesFor(user.getId(), word.getId(), modes)))
                .toList();

        return new DaySessionDto(
                lessonDay.getId(), lessonDay.getDayNumber(), lessonDay.getTitle(),
                course.getTitle(), language, modes, course.isSentenceTranslationEnabled(), words,
                sentenceService.statsForDay(user.getId(), lessonDayId)
        );
    }

    /**
     * Mastery is per session, so an unmastered word always needs the full set of modes again —
     * ticks earned in an earlier session no longer shorten the list.
     */
    private List<PracticeMode> pendingModesFor(Long userId, Long wordId, List<PracticeMode> courseModes) {
        boolean mastered = wordProgressRepository.findByUser_IdAndWord_Id(userId, wordId)
                .map(UserWordProgress::isFullyMastered)
                .orElse(false);
        return mastered ? List.of() : courseModes;
    }

    // ---- Admin content management ----

    @Transactional(readOnly = true)
    public List<TopicDto> listTopics() {
        return topicRepository.findAll().stream().map(TopicDto::from).toList();
    }

    @Transactional
    public TopicDto createTopic(CreateTopicRequest request) {
        if (topicRepository.findAll().stream().anyMatch(t -> t.getSlug().equalsIgnoreCase(request.slug()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic slug already exists");
        }
        Topic topic = Topic.builder()
                .slug(request.slug())
                .name(request.name())
                .description(request.description())
                .language(request.language() == null ? Language.ENGLISH : request.language())
                .build();
        return TopicDto.from(topicRepository.save(topic));
    }

    @Transactional(readOnly = true)
    public List<AdminCourseDto> listAllCoursesAdmin() {
        return courseRepository.findAll().stream()
                .map(course -> AdminCourseDto.from(course, (int) lessonDayRepository.countByCourse_Id(course.getId())))
                .toList();
    }

    @Transactional
    public AdminCourseDto createCourse(CreateCourseRequest request) {
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        Course course = Course.builder()
                .topic(topic)
                .level(request.level())
                .kind(request.kindOrDefault())
                .levelLabel(blankToNull(request.levelLabel()))
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .title(request.title())
                .description(request.description())
                .practiceModes(request.practiceModesOrEmpty())
                .sentenceTranslationEnabled(request.sentenceTranslationEnabledOrDefault())
                .daysAlwaysUnlocked(request.daysAlwaysUnlockedOrDefault())
                .build();
        Course saved = courseRepository.save(course);
        return AdminCourseDto.from(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<LessonDayDto> listLessonDays(Long courseId) {
        return lessonDayRepository.findByCourse_IdOrderByDayNumberAsc(courseId).stream()
                .map(this::toLessonDayDto)
                .toList();
    }

    @Transactional
    public LessonDayDto createLessonDay(Long courseId, CreateLessonDayRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Set<Integer> existingDayNumbers = lessonDayRepository.findByCourse_IdOrderByDayNumberAsc(courseId).stream()
                .map(LessonDay::getDayNumber)
                .collect(Collectors.toSet());

        for (int day = 1; day < request.dayNumber(); day++) {
            if (!existingDayNumbers.contains(day)) {
                lessonDayRepository.save(LessonDay.builder().course(course).dayNumber(day).build());
            }
        }

        LessonDay lessonDay = lessonDayRepository.findByCourse_IdAndDayNumber(courseId, request.dayNumber())
                .orElseGet(() -> LessonDay.builder().course(course).dayNumber(request.dayNumber()).build());
        lessonDay.setTitle(blankToNull(request.title()));
        lessonDay.setSourceLessonLabel(blankToNull(request.sourceLessonLabel()));
        lessonDay.setPartIndex(request.partIndex());

        LessonDay saved = lessonDayRepository.save(lessonDay);
        return toLessonDayDto(saved);
    }

    @Transactional(readOnly = true)
    public List<WordDto> listWords(Long lessonDayId) {
        return wordRepository.findByLessonDay_Id(lessonDayId).stream().map(WordDto::from).toList();
    }

    @Transactional
    public WordDto createWord(Long lessonDayId, CreateWordRequest request) {
        LessonDay lessonDay = lessonDayRepository.findById(lessonDayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson day not found"));
        Word word = toWordEntity(lessonDay, request);
        return WordDto.from(wordRepository.save(word));
    }

    @Transactional
    public List<WordDto> createWordsBatch(Long lessonDayId, CreateWordsBatchRequest request) {
        LessonDay lessonDay = lessonDayRepository.findById(lessonDayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson day not found"));

        if (request.dayTitle() != null && !request.dayTitle().isBlank()) {
            lessonDay.setTitle(request.dayTitle().trim());
            lessonDayRepository.save(lessonDay);
        }

        // A prompt cannot guarantee this: the model has repeated words that were in its exclusion
        // list. Duplicates within one day break multiple-choice questions (the copy becomes a second
        // correct option), so they are dropped here rather than trusted away.
        Set<String> existing = wordRepository.findByLessonDay_Id(lessonDayId).stream()
                .map(word -> word.getTerm().trim().toLowerCase())
                .collect(Collectors.toCollection(java.util.HashSet::new));

        List<Word> words = new java.util.ArrayList<>();
        for (CreateWordRequest incoming : request.words()) {
            // add() returning false means the term is already in the day, or repeated in this batch.
            if (existing.add(incoming.term().trim().toLowerCase())) {
                words.add(toWordEntity(lessonDay, incoming));
            }
        }
        return wordRepository.saveAll(words).stream().map(WordDto::from).toList();
    }

    /**
     * Asks the AI for one day's worth of vocabulary, feeding it every title and word already used in
     * the course so it picks a fresh sub-theme instead of repeating earlier days.
     */
    @Transactional(readOnly = true)
    public GeneratedDayWordsDto generateDayWords(Long lessonDayId, GenerateDayWordsRequest request) {
        LessonDay lessonDay = lessonDayRepository.findById(lessonDayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson day not found"));
        Course course = lessonDay.getCourse();

        List<LessonDay> courseDays = lessonDayRepository.findByCourse_IdOrderByDayNumberAsc(course.getId());

        List<String> usedTitles = new java.util.ArrayList<>(courseDays.stream()
                .filter(d -> !d.getId().equals(lessonDayId))
                .map(LessonDay::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .toList());
        if (request.excludeTitles() != null) {
            usedTitles.addAll(request.excludeTitles());
        }

        // The day's own words are the hard constraint — a repeat there produces two entries of the
        // same word in one lesson, which then show up as two "correct" options in a quiz. Words from
        // other days are only a soft preference: textbooks legitimately revisit a word.
        List<String> wordsInThisDay = new java.util.ArrayList<>(
                wordRepository.findByLessonDay_Id(lessonDayId).stream().map(Word::getTerm).toList());
        if (request.excludeWords() != null) {
            // Days generated earlier in the same bulk run are not saved yet, so the client passes
            // them along; treat them as forbidden too.
            wordsInThisDay.addAll(request.excludeWords());
        }

        List<String> wordsElsewhere = courseDays.stream()
                .filter(d -> !d.getId().equals(lessonDayId))
                .flatMap(d -> wordRepository.findByLessonDay_Id(d.getId()).stream())
                .map(Word::getTerm)
                .toList();

        return aiContentService.generateDayWords(
                course.getLanguage(),
                course.getTopic().getName(),
                course.getLevelLabel() != null ? course.getLevelLabel() : course.getLevel().name(),
                lessonDay.getDayNumber(),
                lessonDay.getTitle(),
                usedTitles,
                wordsInThisDay,
                wordsElsewhere,
                request.count()
        );
    }

    @Transactional
    public LessonDayDto updateLessonDay(Long lessonDayId, UpdateLessonDayRequest request) {
        LessonDay lessonDay = lessonDayRepository.findById(lessonDayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson day not found"));
        lessonDay.setTitle(request.title() == null || request.title().isBlank() ? null : request.title().trim());
        LessonDay saved = lessonDayRepository.save(lessonDay);
        return toLessonDayDto(saved);
    }

    @Transactional
    public void deleteLessonDay(Long lessonDayId) {
        deleteLessonDays(List.of(lessonDayId));
    }

    /**
     * Deletes days together with their words and everyone's progress on them, then closes the gaps by
     * renumbering the course from 1 and moving learners along with the shift.
     */
    @Transactional
    public void deleteLessonDays(List<Long> lessonDayIds) {
        List<LessonDay> days = lessonDayRepository.findAllById(lessonDayIds);
        if (days.isEmpty()) {
            return;
        }

        Map<Long, List<Integer>> removedNumbersByCourse = days.stream().collect(Collectors.groupingBy(
                day -> day.getCourse().getId(),
                Collectors.mapping(LessonDay::getDayNumber, Collectors.toList())));

        for (LessonDay day : days) {
            // Sentence exercises reference the day, so they have to go before it does.
            sentenceService.deleteAll(day.getId());
            wordProgressRepository.deleteByWord_LessonDay_Id(day.getId());
            wordRepository.deleteByLessonDay_Id(day.getId());
            dayProgressRepository.deleteByLessonDay_Id(day.getId());
        }
        lessonDayRepository.deleteAll(days);
        // Free the day numbers before reusing them, otherwise the (course, day_number) unique index trips.
        lessonDayRepository.flush();

        removedNumbersByCourse.forEach((courseId, removedNumbers) -> {
            renumberCourseDays(courseId);
            shiftEnrollments(courseId, removedNumbers);
        });
    }

    private void renumberCourseDays(Long courseId) {
        int expected = 1;
        for (LessonDay day : lessonDayRepository.findByCourse_IdOrderByDayNumberAsc(courseId)) {
            if (day.getDayNumber() != expected) {
                day.setDayNumber(expected);
                lessonDayRepository.save(day);
                lessonDayRepository.flush();
            }
            expected++;
        }
    }

    /** Keeps learners on the same lesson content after days before them were removed. */
    private void shiftEnrollments(Long courseId, List<Integer> removedDayNumbers) {
        int remainingDays = (int) lessonDayRepository.countByCourse_Id(courseId);

        enrollmentRepository.findAll().stream()
                .filter(e -> e.getCourse().getId().equals(courseId))
                .forEach(enrollment -> {
                    long removedBefore = removedDayNumbers.stream()
                            .filter(number -> number < enrollment.getCurrentDayNumber())
                            .count();
                    int shifted = (int) Math.max(1, enrollment.getCurrentDayNumber() - removedBefore);
                    // remainingDays + 1 means "course finished".
                    enrollment.setCurrentDayNumber(Math.min(shifted, Math.max(1, remainingDays + 1)));
                    enrollmentRepository.save(enrollment);
                });
    }

    @Transactional
    public void deleteAllWordsInDay(Long lessonDayId) {
        if (!lessonDayRepository.existsById(lessonDayId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson day not found");
        }
        wordProgressRepository.deleteByWord_LessonDay_Id(lessonDayId);
        wordRepository.deleteByLessonDay_Id(lessonDayId);
    }

    @Transactional
    public WordDto updateWord(Long wordId, CreateWordRequest request) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
        word.setTerm(request.term());
        word.setPhonetic(request.phonetic());
        word.setPartOfSpeech(request.partOfSpeech());
        word.setVietnameseMeaning(request.vietnameseMeaning());
        word.setHanViet(request.hanViet());
        word.setUsageNote(request.usageNote());
        word.setExampleSentenceTarget(request.exampleSentenceTarget());
        word.setExampleSentenceVi(request.exampleSentenceVi());
        return WordDto.from(wordRepository.save(word));
    }

    @Transactional
    public void deleteWord(Long wordId) {
        deleteWords(List.of(wordId));
    }

    @Transactional
    public void deleteWords(List<Long> wordIds) {
        List<Word> words = wordRepository.findAllById(wordIds);
        if (words.isEmpty()) {
            return;
        }
        words.forEach(word -> wordProgressRepository.deleteByWord_Id(word.getId()));
        wordRepository.deleteAll(words);
    }

    private LessonDayDto toLessonDayDto(LessonDay day) {
        return LessonDayDto.from(
                day,
                (int) wordRepository.countByLessonDay_Id(day.getId()),
                (int) sentenceService.countFor(day.getId()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Word toWordEntity(LessonDay lessonDay, CreateWordRequest request) {
        return Word.builder()
                .lessonDay(lessonDay)
                .term(request.term())
                .phonetic(request.phonetic())
                .partOfSpeech(request.partOfSpeech())
                .vietnameseMeaning(request.vietnameseMeaning())
                .hanViet(request.hanViet())
                .usageNote(request.usageNote())
                .exampleSentenceTarget(request.exampleSentenceTarget())
                .exampleSentenceVi(request.exampleSentenceVi())
                .build();
    }
}
