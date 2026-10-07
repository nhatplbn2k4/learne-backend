package com.learn.learnE_Backend.grammar;

import com.learn.learnE_Backend.ai.AiContentService;
import com.learn.learnE_Backend.ai.dto.GeneratedGrammarLessonDto;
import com.learn.learnE_Backend.common.CjkText;
import com.learn.learnE_Backend.grammar.dto.GrammarContentDto;
import com.learn.learnE_Backend.grammar.dto.GrammarGapRequestDto;
import com.learn.learnE_Backend.grammar.dto.GrammarPointDto;
import com.learn.learnE_Backend.vocabulary.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Gaps in the grammar catalogue, reported by grading and worked through by an admin.
 *
 * <p>There is no notification system in this app, so this follows the one precedent that exists —
 * the pending-accounts badge: a table plus an admin tab plus a count on the sidebar.
 */
@Service
public class GrammarGapService {

    private static final Logger log = LoggerFactory.getLogger(GrammarGapService.class);

    /** Long names are almost always the AI explaining rather than naming; keep the column honest. */
    private static final int MAX_NAME_LENGTH = 200;

    /**
     * Words that mean the model described a KIND of mistake instead of naming a grammar point.
     *
     * <p>Matched as whole words anywhere in the name rather than as a prefix: "Dùng sai từ",
     * "Dùng từ sai" and "Lỗi dịch sai nghĩa" are all the same non-answer, and a prefix rule only
     * catches whichever order it was written in. A real point is named after the pattern —
     * "Trợ từ 了", "Bổ ngữ kết quả" — and never contains any of these.
     */
    private static final Set<String> MISTAKE_WORDS = Set.of("loi", "sai", "thieu", "thua");

    /** The same idea for names that are a phrase rather than one telling word. */
    private static final List<String> MISTAKE_PHRASES = List.of(
            "chinh ta", "dien dat", "tu vung", "dung tu", "tu nhien", "nghia cua tu");

    private final GrammarGapRequestRepository gapRepository;
    private final GrammarLessonRepository lessonRepository;
    private final AiContentService aiContentService;

    public GrammarGapService(
            GrammarGapRequestRepository gapRepository,
            GrammarLessonRepository lessonRepository,
            AiContentService aiContentService
    ) {
        this.gapRepository = gapRepository;
        this.lessonRepository = lessonRepository;
        this.aiContentService = aiContentService;
    }

    /**
     * Drafts the missing lesson with AI, using the reported mistake as the brief.
     *
     * <p>Returned for review rather than saved: this lesson has no slide behind it to check the
     * examples against, so a person signs it off before learners see it.
     */
    @Transactional(readOnly = true)
    public GeneratedGrammarLessonDto draftLesson(Long gapId) {
        GrammarGapRequest gap = require(gapId);
        GeneratedGrammarLessonDto draft = aiContentService.generateGrammarLesson(
                gap.getLanguage(),
                gap.getGrammarName(),
                gap.getExamplePrompt(),
                gap.getExampleAnswer(),
                gap.getExplanation(),
                catalogueFor(gap.getLanguage()));
        return gap.getLanguage() == Language.CHINESE ? withUsableExamples(draft) : draft;
    }

    /**
     * Drops worked examples whose Chinese line carries no Han character.
     *
     * <p>Ten lessons already shipped with the Vietnamese prompt sitting in the Chinese field under
     * invented pinyin, and an admin reviewing a draft reads the Vietnamese first and sees nothing
     * wrong. Checked here rather than asked for in the prompt, because this is the kind of rule a
     * model can agree to and still break.
     */
    private static GeneratedGrammarLessonDto withUsableExamples(GeneratedGrammarLessonDto draft) {
        List<GrammarContentDto.ExampleDto> examples = draft.examples() == null
                ? List.of()
                : draft.examples().stream().filter(e -> CjkText.hasHan(e.target())).toList();

        if (examples.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI tra ve ban nhap khong co vi du tieng Trung nao dung - hay sinh lai");
        }
        return new GeneratedGrammarLessonDto(
                draft.code(), draft.title(), draft.summary(), draft.formula(),
                draft.components(), examples, draft.notes(), draft.difficulty());
    }

    /**
     * Records that a learner hit a grammar point with no lesson behind it.
     *
     * <p>Runs in its own transaction so that a failure here — a clash on the unique key when two
     * answers are graded at once, say — rolls back only the report and never the learner's attempt.
     * The caller wraps the call in a catch for the same reason.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void report(Language language, String grammarName, String promptVi, String answer, String explanation) {
        String name = trimTo(grammarName, MAX_NAME_LENGTH);
        String normalized = normalize(name);
        if (normalized.isEmpty() || describesAMistakeKind(normalized)) {
            return;
        }

        GrammarGapRequest existing = gapRepository
                .findByLanguageAndNormalizedName(language, normalized)
                .orElse(null);

        if (existing != null) {
            // An ignored or already-resolved gap stays as the admin left it; only the tally moves.
            existing.setReportCount(existing.getReportCount() + 1);
            existing.setLastReportedAt(Instant.now());
            gapRepository.save(existing);
            return;
        }

        gapRepository.save(GrammarGapRequest.builder()
                .language(language)
                .grammarName(name)
                .normalizedName(normalized)
                .examplePrompt(promptVi)
                .exampleAnswer(answer)
                .explanation(explanation)
                .build());
        log.info("Ghi nhan thieu bai ngu phap: {} ({})", name, language);
    }

    /**
     * The existing points, so the draft does not reuse a code.
     *
     * <p>Read from the repository rather than through {@code GrammarCatalogService}: that service
     * depends on this one to report gaps, and injecting it back would make the two beans circular.
     */
    private List<GrammarPointDto> catalogueFor(Language language) {
        return lessonRepository.findByCourse_Topic_LanguageOrderBySortOrderAscIdAsc(language).stream()
                .map(lesson -> new GrammarPointDto(
                        lesson.getCode(), lesson.getTitle(), lesson.getFormula()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrammarGapRequestDto> list(Language language) {
        return gapRepository.findByLanguageOrderByStatusAscReportCountDescLastReportedAtDesc(language)
                .stream()
                .map(GrammarGapRequestDto::from)
                .toList();
    }

    /** Count for the sidebar badge, mirroring how accounts awaiting approval are surfaced. */
    @Transactional(readOnly = true)
    public long openCount() {
        return gapRepository.countByStatus(GrammarGapStatus.OPEN);
    }

    /** Marks a gap closed, optionally pointing at the lesson that now covers it. */
    @Transactional
    public GrammarGapRequestDto resolve(Long gapId, Long lessonId) {
        GrammarGapRequest gap = require(gapId);
        gap.setStatus(GrammarGapStatus.RESOLVED);
        gap.setResolvedLesson(lessonId == null ? null : lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài ngữ pháp")));
        return GrammarGapRequestDto.from(gapRepository.save(gap));
    }

    @Transactional
    public GrammarGapRequestDto ignore(Long gapId) {
        GrammarGapRequest gap = require(gapId);
        gap.setStatus(GrammarGapStatus.IGNORED);
        return GrammarGapRequestDto.from(gapRepository.save(gap));
    }

    @Transactional
    public void delete(Long gapId) {
        gapRepository.deleteById(gapId);
    }

    /**
     * Whether this is the name of a grammar point at all, as opposed to a kind of mistake.
     *
     * <p>The single answer to that question: it decides both whether the gap is worth an admin's
     * attention and whether the learner is shown a "Ngữ pháp:" line. Answering it in two places is
     * how a correction ended up reading "Ngữ pháp: Dùng sai từ".
     */
    public static boolean namesAGrammarPoint(String rawName) {
        String normalized = normalize(rawName);
        return !normalized.isEmpty() && !describesAMistakeKind(normalized);
    }

    static boolean describesAMistakeKind(String normalizedName) {
        for (String word : normalizedName.split(" ")) {
            if (MISTAKE_WORDS.contains(word)) {
                return true;
            }
        }
        return MISTAKE_PHRASES.stream().anyMatch(normalizedName::contains);
    }

    private GrammarGapRequest require(Long gapId) {
        return gapRepository.findById(gapId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"));
    }

    /**
     * Folds the spellings the AI uses for the same point onto one key: "Trợ từ 的", "tro tu 的" and
     * "Trợ từ '的'" all become "tro tu 的". Diacritics go, case goes, punctuation goes; the Chinese
     * characters stay, because they are what actually identifies the pattern.
     */
    static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String folded = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase();
        return folded.replaceAll("[\\p{Punct}\\p{IsPunctuation}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String trimTo(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
