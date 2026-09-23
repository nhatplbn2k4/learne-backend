package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.grammar.dto.GrammarPointDto;
import com.learn.learnE_Backend.grammar.dto.GrammarRefDto;
import com.learn.learnE_Backend.sentence.dto.SentenceFeedbackDto;
import com.learn.learnE_Backend.vocabulary.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The bridge between AI feedback and the grammar lessons: it hands the model a catalogue to choose
 * from, then turns whatever it chose back into a real lesson.
 *
 * <p>Kept apart from {@code GrammarService} because the sentence-translation side needs exactly
 * this and nothing else — it has no business knowing how lessons are listed or drilled.
 */
@Service
public class GrammarCatalogService {

    private static final Logger log = LoggerFactory.getLogger(GrammarCatalogService.class);

    private final GrammarLessonRepository lessonRepository;
    private final UserGrammarLessonProgressRepository progressRepository;
    private final GrammarGapService gapService;

    public GrammarCatalogService(
            GrammarLessonRepository lessonRepository,
            UserGrammarLessonProgressRepository progressRepository,
            GrammarGapService gapService
    ) {
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.gapService = gapService;
    }

    /** Every grammar point of one language that has a lesson, as (code, title) for the AI prompt. */
    @Transactional(readOnly = true)
    public List<GrammarPointDto> catalogue(Language language) {
        return lessonRepository.findByCourse_Topic_LanguageOrderBySortOrderAscIdAsc(language).stream()
                .map(lesson -> new GrammarPointDto(
                        lesson.getCode(), lesson.getTitle(), lesson.getFormula()))
                .toList();
    }

    /**
     * The grammar codes this learner has studied in one language — one query, because the
     * unlearned-grammar warning needs the same set for every sentence of a day.
     */
    @Transactional(readOnly = true)
    public Set<String> learnedCodes(Long userId, Language language) {
        return progressRepository.findByUser_IdAndLesson_Course_Topic_Language(userId, language).stream()
                .map(row -> row.getLesson().getCode())
                .collect(Collectors.toSet());
    }

    /** Resolves one code to a lesson, or null when nothing in the database matches. */
    @Transactional(readOnly = true)
    public GrammarRefDto resolve(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return lessonRepository.findByCode(code.trim())
                .map(GrammarRefDto::from)
                .orElse(null);
    }

    /** Resolves several codes at once, skipping any the database does not know. */
    @Transactional(readOnly = true)
    public List<GrammarRefDto> resolveAll(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<GrammarRefDto> refs = new ArrayList<>();
        for (String code : codes) {
            GrammarRefDto ref = resolve(code);
            if (ref != null) {
                refs.add(ref);
            }
        }
        return refs;
    }

    /**
     * Fills in the lesson pointer on every correction the AI flagged as a grammar mistake.
     *
     * <p>The code is looked up rather than trusted: a model asked to pick from a list will now and
     * then return something that was never on it, and a made-up code must read as "no lesson covers
     * this" instead of becoming a broken link.
     */
    public SentenceFeedbackDto attachGrammarRefs(SentenceFeedbackDto feedback) {
        return attachGrammarRefs(feedback, null, null, null);
    }

    /**
     * As above, and additionally reports any grammar point the AI named that no lesson covers.
     *
     * @param language must be given for a gap to be reportable; the prompt and answer go into the
     *                 report as the example an admin looks at.
     */
    public SentenceFeedbackDto attachGrammarRefs(
            SentenceFeedbackDto feedback, Language language, String promptVi, String answer) {
        if (feedback == null || feedback.corrections() == null || feedback.corrections().isEmpty()) {
            return feedback;
        }

        // One lookup per distinct code: a single answer can trip the same point more than once.
        Map<String, GrammarRefDto> byCode = new HashMap<>();
        List<SentenceFeedbackDto.SentenceCorrectionDto> resolved =
                new ArrayList<>(feedback.corrections().size());

        for (SentenceFeedbackDto.SentenceCorrectionDto correction : feedback.corrections()) {
            String code = correction.grammarCode();
            GrammarRefDto ref = (code == null || code.isBlank())
                    ? null
                    : byCode.computeIfAbsent(code.trim(), this::resolve);
            if (ref != null) {
                resolved.add(correction.withGrammar(ref));
                continue;
            }

            // No lesson matched. Either the catalogue is missing this point — worth telling the
            // admin and the learner — or the model labelled a word-choice slip as grammar, in which
            // case the label is dropped so no "Ngữ pháp:" line is shown for it.
            if (GrammarGapService.namesAGrammarPoint(correction.grammarName())) {
                resolved.add(correction);
                if (language != null) {
                    reportGap(language, correction, promptVi, answer);
                }
            } else {
                resolved.add(correction.withoutGrammar());
            }
        }
        return feedback.withCorrections(resolved);
    }

    /** Never let bookkeeping cost a learner their graded answer. */
    private void reportGap(
            Language language,
            SentenceFeedbackDto.SentenceCorrectionDto correction,
            String promptVi,
            String answer
    ) {
        try {
            gapService.report(
                    language, correction.grammarName(), promptVi, answer, correction.explanation());
        } catch (Exception ex) {
            log.warn("Khong ghi duoc yeu cau bo sung ngu phap '{}'", correction.grammarName(), ex);
        }
    }
}
