package com.learn.learnE_Backend.ai;

import com.learn.learnE_Backend.ai.dto.GenerateReadingRequest;
import com.learn.learnE_Backend.ai.dto.GenerateWritingPromptRequest;
import com.learn.learnE_Backend.grammar.dto.GrammarPointDto;
import com.learn.learnE_Backend.vocabulary.CourseLevel;
import com.learn.learnE_Backend.vocabulary.Language;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every prompt is a format string with a matching argument list, and the two drift apart silently:
 * nothing fails until somebody clicks the button, and then it fails with
 * "IllegalFormatConversionException: d != java.lang.String" — which is what happened when a
 * grammar catalogue was inserted into the middle of the sentence-generation arguments.
 *
 * <p>These tests build each prompt with no API key configured. The prompt is formatted before the
 * key is checked, so a mismatched argument list blows up here while a correct one gets as far as
 * the "not configured" refusal. No network, no key, no quota.
 */
class AiPromptFormatTest {

    private static final List<GrammarPointDto> CATALOGUE = List.of(
            new GrammarPointDto("tai-le", "太…了", "太 + tính từ + 了"),
            new GrammarPointDto("ba-ju", "把字句", "把 + tân ngữ + V"));

    private static final List<String> WORDS = List.of("学生 (xuésheng) = học sinh");

    /** No api-key property, so isConfigured() is false and nothing leaves the machine. */
    private static AiContentService service() {
        return new AiContentService(
                new GeminiClient(WebClient.builder(), "", "flash", "flash-lite", ""));
    }

    /** Formatting ran and the only thing left to stop us was the missing key. */
    private static void assertPromptBuilds(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    void dayWordsPromptFormats() {
        for (Language language : Language.values()) {
            assertPromptBuilds(() -> service().generateDayWords(
                    language, "Đời sống", "BEGINNER", 3, "Gia đình",
                    List.of("Chào hỏi"), WORDS, WORDS, 10));
        }
    }

    /** The one that broke: a %d for the sentence count sits between %s arguments. */
    @Test
    void sentencePromptFormats() {
        for (Language language : Language.values()) {
            assertPromptBuilds(() -> service().generateSentences(
                    language, "Ngày 3", WORDS, WORDS, 20, CATALOGUE));
            assertPromptBuilds(() -> service().generateSentences(
                    language, "Ngày 3", WORDS, WORDS, 20, List.of()));
        }
    }

    @Test
    void testSentencePromptFormats() {
        for (Language language : Language.values()) {
            assertPromptBuilds(() -> service().generateTestSentences(
                    language, "Ngày 1-10", WORDS, WORDS, 20));
        }
    }

    @Test
    void grammarExercisePromptFormats() {
        for (Language language : Language.values()) {
            assertPromptBuilds(() -> service().generateGrammarExercises(
                    language, "太…了", "太 + tính từ + 了", "太 = quá", "今天太冷了。", 15));
        }
    }

    @Test
    void grammarLessonPromptFormats() {
        for (Language language : Language.values()) {
            assertPromptBuilds(() -> service().generateGrammarLesson(
                    language, "Trợ từ 了", "Hôm qua tôi đi học.", "昨天我去上课了了。",
                    "Thừa một chữ 了.", CATALOGUE));
            // The reported mistake is optional, and the prompt has to format without it too.
            assertPromptBuilds(() -> service().generateGrammarLesson(
                    language, "Trợ từ 了", null, null, null, List.of()));
        }
    }

    @Test
    void grammarTaggingPromptFormats() {
        for (Language language : Language.values()) {
            assertPromptBuilds(() -> service().tagSentenceGrammar(
                    language, List.of("今天太冷了。", "我在家看书。"), CATALOGUE));
        }
    }

    @Test
    void batchGradingPromptFormats() {
        for (Language language : Language.values()) {
            assertPromptBuilds(() -> service().gradeSentenceBatch(
                    language, "1. Đề: ... / Bài làm: ...", CATALOGUE));
        }
    }

    @Test
    void readingAndWritingPromptsFormat() {
        assertPromptBuilds(() -> service().generateReadingPassage(
                new GenerateReadingRequest("Đời sống", CourseLevel.BEGINNER, "Một ngày của tôi")));
        assertPromptBuilds(() -> service().generateWritingPrompt(
                new GenerateWritingPromptRequest("Đời sống", CourseLevel.BEGINNER)));
    }
}
