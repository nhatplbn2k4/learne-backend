package com.learn.learnE_Backend.sentence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.learnE_Backend.ai.AiContentService;
import com.learn.learnE_Backend.ai.dto.TaggedSentencesDto;
import com.learn.learnE_Backend.grammar.GrammarCatalogService;
import com.learn.learnE_Backend.grammar.GrammarFormula;
import com.learn.learnE_Backend.grammar.dto.GrammarPointDto;
import com.learn.learnE_Backend.grammar.dto.GrammarRefDto;
import com.learn.learnE_Backend.ai.dto.GeneratedSentencesDto;
import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.common.CjkText;
import com.learn.learnE_Backend.sentence.dto.*;
import com.learn.learnE_Backend.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SentenceService {

    private static final Logger log = LoggerFactory.getLogger(SentenceService.class);

    /** How many earlier words to feed the generator — enough for variety, short enough to stay cheap. */
    private static final int MAX_EARLIER_WORDS = 120;

    /** Sentences per tagging request: big enough to be cheap, small enough not to be truncated. */
    private static final int TAG_BATCH_SIZE = 20;

    /** Shortest quotation accepted as proof of a grammar tag; one character proves nothing. */
    private static final int MIN_TAG_EVIDENCE_CHARS = 2;

    /** Bar for skipping the overnight wait on a Chinese course: 20 sentences averaging 8/10. */
    public static final int SKIP_AHEAD_SENTENCES = 20;
    public static final double SKIP_AHEAD_MIN_AVERAGE = 8.0;

    private final SentenceExerciseRepository exerciseRepository;
    private final UserSentenceAttemptRepository attemptRepository;
    private final LessonDayRepository lessonDayRepository;
    private final WordRepository wordRepository;
    private final UserWordProgressRepository wordProgressRepository;
    private final AiContentService aiContentService;
    private final GrammarCatalogService grammarCatalogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SentenceService(
            SentenceExerciseRepository exerciseRepository,
            UserSentenceAttemptRepository attemptRepository,
            LessonDayRepository lessonDayRepository,
            WordRepository wordRepository,
            UserWordProgressRepository wordProgressRepository,
            AiContentService aiContentService,
            GrammarCatalogService grammarCatalogService
    ) {
        this.exerciseRepository = exerciseRepository;
        this.attemptRepository = attemptRepository;
        this.lessonDayRepository = lessonDayRepository;
        this.wordRepository = wordRepository;
        this.wordProgressRepository = wordProgressRepository;
        this.aiContentService = aiContentService;
        this.grammarCatalogService = grammarCatalogService;
    }

    // ---- Reading ----

    @Transactional(readOnly = true)
    public SentenceSetDto getDaySentences(User user, Long lessonDayId) {
        LessonDay lessonDay = requireDay(lessonDayId);

        int totalWords = (int) wordRepository.countByLessonDay_Id(lessonDayId);
        int masteredWords = (int) wordProgressRepository
                .countByUser_IdAndWord_LessonDay_IdAndMasteredTrue(user.getId(), lessonDayId);
        String lockedReason = lockReason(totalWords, masteredWords);

        // Resolved once per day rather than per sentence: both sets are the same for all of them.
        Language language = lessonDay.getCourse().getLanguage();
        Set<String> learnedCodes = grammarCatalogService.learnedCodes(user.getId(), language);
        Set<String> studied = wordProgressRepository.findStudiedTerms(user.getId(), language).stream()
                .map(String::trim)
                .collect(Collectors.toSet());

        // Locked days still report their progress so the UI can show how far away the unlock is.
        List<SentenceExerciseDto> exercises = lockedReason != null
                ? List.of()
                : exerciseRepository.findByLessonDay_IdOrderBySortOrderAscIdAsc(lessonDayId).stream()
                        .map(exercise -> toDto(user.getId(), exercise, learnedCodes, studied))
                        .toList();

        return new SentenceSetDto(
                lessonDay.getId(),
                lessonDay.getDayNumber(),
                lessonDay.getTitle(),
                lessonDay.getCourse().getTitle(),
                lessonDay.getCourse().getLanguage(),
                lockedReason == null,
                lockedReason,
                masteredWords,
                totalWords,
                statsForDay(user.getId(), lessonDayId),
                skipAheadProgress(user.getId(), lessonDayId),
                exercises
        );
    }

    /** Translation opens only once every word of the day is fully mastered. */
    private static String lockReason(int totalWords, int masteredWords) {
        if (totalWords == 0) {
            return "Ngày này chưa có từ vựng nào";
        }
        if (masteredWords < totalWords) {
            return "Cần thuộc lòng đủ %d/%d từ của ngày này trước khi luyện dịch (hiện %d)"
                    .formatted(totalWords, totalWords, masteredWords);
        }
        return null;
    }

    private SentenceExerciseDto toDto(
            Long userId, SentenceExercise exercise, Set<String> learnedCodes, Set<String> studied) {
        List<SentenceAttemptDto> attempts = attemptRepository
                .findByUser_IdAndExercise_LessonDay_IdOrderBySubmittedAtDesc(userId, exercise.getLessonDay().getId())
                .stream()
                .filter(attempt -> attempt.getExercise().getId().equals(exercise.getId()))
                .map(this::toAttemptDto)
                .toList();

        SentenceAttemptDto lastAttempt = attempts.isEmpty() ? null : attempts.get(0);
        Integer bestScore = attempts.stream()
                .map(SentenceAttemptDto::score)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        // The model answer stays hidden until the learner has actually tried.
        return toDto(exercise, lastAttempt, bestScore, lastAttempt != null, learnedCodes, studied);
    }

    private SentenceExerciseDto toDto(
            SentenceExercise exercise,
            SentenceAttemptDto lastAttempt,
            Integer bestScore,
            boolean revealAnswer,
            Set<String> learnedCodes,
            Set<String> studied
    ) {
        return new SentenceExerciseDto(
                exercise.getId(),
                exercise.getSortOrder(),
                exercise.getPromptVi(),
                unknownWords(exercise.getNewWordsJson(), studied),
                unlearnedGrammar(exercise, learnedCodes),
                revealAnswer ? exercise.getAnswerTarget() : null,
                revealAnswer ? exercise.getAnswerPhonetic() : null,
                lastAttempt,
                bestScore
        );
    }

    /**
     * Which of this sentence's grammar tags the learner has not studied.
     *
     * <p>A tag naming a lesson that no longer exists simply drops out — codes are resolved against
     * the catalogue, never trusted on their own.
     */
    /**
     * Hints only for words this learner has not met.
     *
     * <p>The list was worked out when the sentences were generated, from where the day sits in the
     * course — so a word the learner has since studied, or studied out of order, still shows up as
     * new. Their own progress is the only thing that actually answers the question.
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

    private List<GrammarRefDto> unlearnedGrammar(SentenceExercise exercise, Set<String> learnedCodes) {
        if (learnedCodes == null) {
            return List.of();
        }
        List<String> tagged = readGrammarCodes(exercise.getGrammarCodes());
        if (tagged.isEmpty()) {
            return List.of();
        }
        return grammarCatalogService.resolveAll(
                tagged.stream().filter(code -> !learnedCodes.contains(code)).toList());
    }

    /** Admin view: no unlock gate and the reference answer is always visible for proof-reading. */
    @Transactional(readOnly = true)
    public List<SentenceExerciseDto> listForAdmin(Long lessonDayId) {
        return exerciseRepository.findByLessonDay_IdOrderBySortOrderAscIdAsc(lessonDayId).stream()
                .map(exercise -> toDto(exercise, null, null, true, null, null))
                .toList();
    }

    /** Translation progress for one day. */
    @Transactional(readOnly = true)
    public DaySentenceStatsDto statsForDay(Long userId, Long lessonDayId) {
        return buildStats(
                (int) exerciseRepository.countByLessonDay_Id(lessonDayId),
                attemptRepository.findByUser_IdAndExercise_LessonDay_IdOrderBySubmittedAtDesc(userId, lessonDayId));
    }

    /**
     * The same figures for every day of a course, in two queries rather than two per day — the day
     * map renders all of them at once and a long course would otherwise issue hundreds.
     */
    @Transactional(readOnly = true)
    public Map<Long, DaySentenceStatsDto> statsForCourse(Long userId, Long courseId) {
        Map<Long, Integer> totalByDay = new HashMap<>();
        for (SentenceExercise exercise : exerciseRepository.findByLessonDay_Course_Id(courseId)) {
            totalByDay.merge(exercise.getLessonDay().getId(), 1, Integer::sum);
        }

        Map<Long, List<UserSentenceAttempt>> attemptsByDay = attemptRepository
                .findByUser_IdAndExercise_LessonDay_Course_Id(userId, courseId).stream()
                .collect(Collectors.groupingBy(a -> a.getExercise().getLessonDay().getId()));

        Map<Long, DaySentenceStatsDto> stats = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : totalByDay.entrySet()) {
            stats.put(entry.getKey(), buildStats(
                    entry.getValue(), attemptsByDay.getOrDefault(entry.getKey(), List.of())));
        }
        return stats;
    }

    private static DaySentenceStatsDto buildStats(int total, List<UserSentenceAttempt> attempts) {
        Map<Long, Integer> bestPerSentence = new HashMap<>();
        for (UserSentenceAttempt attempt : attempts) {
            if (attempt.getScore() != null) {
                bestPerSentence.merge(attempt.getExercise().getId(), attempt.getScore(), Math::max);
            }
        }
        Double average = bestPerSentence.isEmpty() ? null
                : bestPerSentence.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        return new DaySentenceStatsDto(total, bestPerSentence.size(), average);
    }

    @Transactional(readOnly = true)
    public long countFor(Long lessonDayId) {
        return exerciseRepository.countByLessonDay_Id(lessonDayId);
    }

    /**
     * How far along the learner is towards skipping ahead past {@code lessonDayId}.
     *
     * <p>Scores are taken as the best attempt per sentence, so retrying a sentence until it is right
     * counts as having learnt it. Repeating the same sentence cannot inflate {@code answered}
     * either, because that counts distinct sentences rather than attempts.
     */
    @Transactional(readOnly = true)
    public SkipAheadDto skipAheadProgress(Long userId, Long lessonDayId) {
        Map<Long, Integer> bestPerSentence = new HashMap<>();
        for (UserSentenceAttempt attempt : attemptRepository
                .findByUser_IdAndExercise_LessonDay_IdOrderBySubmittedAtDesc(userId, lessonDayId)) {
            if (attempt.getScore() == null) {
                continue;
            }
            bestPerSentence.merge(attempt.getExercise().getId(), attempt.getScore(), Math::max);
        }

        int available = (int) exerciseRepository.countByLessonDay_Id(lessonDayId);
        int answered = bestPerSentence.size();
        Double average = bestPerSentence.isEmpty() ? null
                : bestPerSentence.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        boolean eligible = answered >= SKIP_AHEAD_SENTENCES
                && average != null && average >= SKIP_AHEAD_MIN_AVERAGE;

        return new SkipAheadDto(
                available, answered, SKIP_AHEAD_SENTENCES, average, SKIP_AHEAD_MIN_AVERAGE, eligible);
    }

    private SentenceAttemptDto toAttemptDto(UserSentenceAttempt attempt) {
        return new SentenceAttemptDto(
                attempt.getId(),
                attempt.getExercise().getId(),
                attempt.getAnswerTarget(),
                attempt.getScore(),
                readFeedback(attempt.getFeedbackJson()),
                attempt.getSubmittedAt()
        );
    }

    // ---- Answering ----

    @Transactional
    public SentenceAttemptDto submit(User user, Long exerciseId, SubmitSentenceRequest request) {
        SentenceExercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu luyện dịch"));

        Long lessonDayId = exercise.getLessonDay().getId();
        int totalWords = (int) wordRepository.countByLessonDay_Id(lessonDayId);
        int masteredWords = (int) wordProgressRepository
                .countByUser_IdAndWord_LessonDay_IdAndMasteredTrue(user.getId(), lessonDayId);
        String lockedReason = lockReason(totalWords, masteredWords);
        if (lockedReason != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, lockedReason);
        }

        SentenceFeedbackDto feedback = gradeWithAi(exercise, request.answerTarget());

        UserSentenceAttempt attempt = attemptRepository.save(UserSentenceAttempt.builder()
                .user(user)
                .exercise(exercise)
                .answerTarget(request.answerTarget().trim())
                .score(feedback.score())
                .feedbackJson(writeJson(feedback))
                .build());

        return toAttemptDto(attempt);
    }

    /**
     * Grading now lives in {@link AiContentService#gradeOneSentence} so that grammar drills mark
     * answers exactly the same way, then the codes it returned are resolved into real lessons.
     */
    private SentenceFeedbackDto gradeWithAi(SentenceExercise exercise, String answer) {
        Language language = exercise.getLessonDay().getCourse().getLanguage();
        SentenceFeedbackDto feedback = aiContentService.gradeOneSentence(
                language,
                exercise.getPromptVi(),
                exercise.getAnswerTarget(),
                answer,
                grammarCatalogService.catalogue(language));
        return grammarCatalogService.attachGrammarRefs(
                feedback, language, exercise.getPromptVi(), answer);
    }

    /**
     * Tags the sentences of one day with the grammar they use, for sentences generated before
     * tagging existed. Done in batches so one AI call covers many sentences: the 225 already in the
     * database would otherwise be 225 requests, far past a day's free-tier quota.
     *
     * @return how many sentences got tags, and how many batches failed outright.
     */
    @Transactional
    public TagGrammarResultDto tagGrammar(Long lessonDayId) {
        LessonDay lessonDay = requireDay(lessonDayId);
        Language language = lessonDay.getCourse().getLanguage();
        List<GrammarPointDto> catalogue = grammarCatalogService.catalogue(language);
        if (catalogue.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chưa có bài ngữ pháp nào để gắn thẻ");
        }

        List<SentenceExercise> exercises = exerciseRepository
                .findByLessonDay_IdOrderBySortOrderAscIdAsc(lessonDayId);
        if (exercises.isEmpty()) {
            return new TagGrammarResultDto(0, 0, 0);
        }

        Map<String, String> formulaByCode = new HashMap<>();
        for (GrammarPointDto point : catalogue) {
            formulaByCode.put(point.code(), point.formula() == null ? "" : point.formula());
        }

        int tagged = 0;
        int failedBatches = 0;
        for (int from = 0; from < exercises.size(); from += TAG_BATCH_SIZE) {
            List<SentenceExercise> batch =
                    exercises.subList(from, Math.min(from + TAG_BATCH_SIZE, exercises.size()));
            int result = tagBatch(language, catalogue, batch, formulaByCode);
            if (result < 0) {
                failedBatches++;
            } else {
                tagged += result;
            }
        }
        exerciseRepository.saveAll(exercises);
        return new TagGrammarResultDto(tagged, exercises.size(), failedBatches);
    }

    /** @return how many sentences in the batch got tags, or -1 when the AI call itself failed. */
    private int tagBatch(
            Language language,
            List<GrammarPointDto> catalogue,
            List<SentenceExercise> batch,
            Map<String, String> formulaByCode
    ) {
        List<String> sentences = batch.stream().map(SentenceExercise::getAnswerTarget).toList();
        TaggedSentencesDto tagged;
        try {
            tagged = aiContentService.tagSentenceGrammar(language, sentences, catalogue);
        } catch (Exception ex) {
            log.warn("Khong gan duoc the ngu phap cho lo {} cau", batch.size(), ex);
            return -1;
        }

        int count = 0;
        for (TaggedSentencesDto.TaggedSentenceDto result : tagged.results()) {
            int index = result.index() == null ? -1 : result.index() - 1;
            if (index < 0 || index >= batch.size() || result.grammar() == null) {
                continue;
            }
            SentenceExercise exercise = batch.get(index);
            List<String> codes = result.grammar().stream()
                    .filter(tag -> tag != null && keepTag(tag, exercise.getAnswerTarget(), formulaByCode))
                    .map(tag -> tag.code().trim())
                    .distinct()
                    .toList();
            if (!codes.isEmpty()) {
                exercise.setGrammarCodes(writeGrammarCodes(codes));
                count++;
            }
        }
        return count;
    }

    /**
     * Whether a tag survives: the code must exist, and the quoted evidence must really appear in
     * the sentence.
     *
     * <p>Asked for codes alone the model over-tags — it reads any 了 as 太…了 and any mention of a
     * place as a place adverbial. Checking its quotation against the sentence throws those out,
     * and the two-character floor stops a single shared character passing as proof.
     */
    private static boolean keepTag(
            TaggedSentencesDto.TagDto tag, String sentence, Map<String, String> formulaByCode) {
        String code = tag.code() == null ? "" : tag.code().trim();
        if (!formulaByCode.containsKey(code)) {
            return false;
        }
        String evidence = tag.evidence() == null ? "" : tag.evidence().trim();
        if (evidence.length() < MIN_TAG_EVIDENCE_CHARS || !sentence.contains(evidence)) {
            return false;
        }
        // The quotation alone proves too little: "快了" really is inside 打电话就快了, and on that
        // basis the sentence was tagged 太…了 even though it uses 就. The sentence has to carry the
        // characters the pattern is built from.
        return GrammarFormula.matches(sentence, formulaByCode.get(code));
    }

    // ---- Admin generation ----

    @Transactional
    public List<SentenceExerciseDto> generate(Long lessonDayId, GenerateSentencesRequest request) {
        LessonDay lessonDay = requireDay(lessonDayId);
        Course course = lessonDay.getCourse();

        List<String> dayWords = wordRepository.findByLessonDay_Id(lessonDayId).stream()
                .map(SentenceService::describe)
                .toList();
        if (dayWords.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày này chưa có từ vựng nào để tạo câu");
        }

        // Words from the earlier days of the same course are fair game in the sentences.
        List<String> earlierWords = lessonDayRepository
                .findByCourse_IdOrderByDayNumberAsc(course.getId()).stream()
                .filter(day -> day.getDayNumber() < lessonDay.getDayNumber())
                .flatMap(day -> wordRepository.findByLessonDay_Id(day.getId()).stream())
                .map(SentenceService::describe)
                .toList();
        if (earlierWords.size() > MAX_EARLIER_WORDS) {
            earlierWords = earlierWords.subList(earlierWords.size() - MAX_EARLIER_WORDS, earlierWords.size());
        }

        String dayLabel = "Ngày %d%s".formatted(
                lessonDay.getDayNumber(), lessonDay.getTitle() == null ? "" : " · " + lessonDay.getTitle());

        GeneratedSentencesDto generated = aiContentService.generateSentences(
                course.getLanguage(), dayLabel, dayWords, earlierWords, request.countOrDefault(),
                grammarCatalogService.catalogue(course.getLanguage()));

        if (generated.sentences() == null || generated.sentences().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI không trả về câu nào");
        }

        if (request.replaceOrDefault()) {
            deleteAll(lessonDayId);
        }

        int nextOrder = (int) exerciseRepository.countByLessonDay_Id(lessonDayId);
        List<SentenceExercise> toSave = new ArrayList<>();
        for (var sentence : generated.sentences()) {
            if (sentence.promptVi() == null || sentence.promptVi().isBlank()
                    || sentence.answerTarget() == null || sentence.answerTarget().isBlank()) {
                continue; // skip a malformed row rather than failing the whole batch
            }
            toSave.add(SentenceExercise.builder()
                    .lessonDay(lessonDay)
                    .promptVi(sentence.promptVi().trim())
                    // The model spaces Chinese out like English; the learner must not have to match that.
                    .answerTarget(CjkText.tidy(sentence.answerTarget()))
                    .answerPhonetic(blankToNull(sentence.answerPhonetic()))
                    .newWordsJson(writeNewWords(sentence.newWords()))
                    .grammarCodes(writeGrammarCodes(sentence.grammarCodes()))
                    .sortOrder(nextOrder++)
                    .build());
        }

        return exerciseRepository.saveAll(toSave).stream()
                .map(exercise -> toDto(exercise, null, null, true, null, null))
                .toList();
    }

    /** "学生 (xuésheng) = học sinh" — enough for the model to use the word correctly. */
    private static String describe(Word word) {
        StringBuilder text = new StringBuilder(word.getTerm());
        if (word.getPhonetic() != null && !word.getPhonetic().isBlank()) {
            text.append(" (").append(word.getPhonetic()).append(")");
        }
        return text.append(" = ").append(word.getVietnameseMeaning()).toString();
    }

    @Transactional
    public void deleteAll(Long lessonDayId) {
        for (SentenceExercise exercise : exerciseRepository.findByLessonDay_IdOrderBySortOrderAscIdAsc(lessonDayId)) {
            attemptRepository.deleteByExercise_Id(exercise.getId());
        }
        exerciseRepository.deleteByLessonDay_Id(lessonDayId);
    }

    @Transactional
    public void delete(Long exerciseId) {
        attemptRepository.deleteByExercise_Id(exerciseId);
        exerciseRepository.deleteById(exerciseId);
    }

    // ---- Helpers ----

    private LessonDay requireDay(Long lessonDayId) {
        return lessonDayRepository.findById(lessonDayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ngày học"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String writeJson(SentenceFeedbackDto feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize sentence feedback", ex);
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

    private String writeGrammarCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (Exception ex) {
            log.warn("Could not serialise grammar codes", ex);
            return null;
        }
    }

    private List<String> readGrammarCodes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            log.warn("Could not read grammar codes", ex);
            return List.of();
        }
    }

    private String writeNewWords(List<GeneratedSentencesDto.GeneratedNewWordDto> newWords) {
        if (newWords == null || newWords.isEmpty()) return null;
        List<NewWordHintDto> hints = newWords.stream()
                .filter(w -> w.term() != null && !w.term().isBlank())
                .map(w -> new NewWordHintDto(w.term(), w.phonetic(), w.meaning()))
                .toList();
        if (hints.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(hints);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<NewWordHintDto> readNewWords(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<NewWordHintDto>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
}
