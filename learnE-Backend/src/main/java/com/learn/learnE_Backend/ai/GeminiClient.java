package com.learn.learnE_Backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.learn.learnE_Backend.ai.dto.ModelStatusDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin client for the Google Gemini "generateContent" REST API (free tier).
 * https://ai.google.dev/api/generate-content
 */
@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final int MAX_TRANSIENT_RETRIES = 4;
    private static final int MAX_RATE_LIMIT_RETRIES = 2;
    private static final Duration MAX_RATE_LIMIT_WAIT = Duration.ofSeconds(70);
    /** How long to skip a rate-limited model when Google does not say. */
    private static final Duration DEFAULT_RATE_LIMIT_PAUSE = Duration.ofSeconds(30);
    private static final Pattern RETRY_HINT = Pattern.compile("retry in ([0-9.]+)s");

    /**
     * Google's free-tier daily quotas reset at midnight Pacific Time, not local
     * midnight.
     */
    private static final ZoneId QUOTA_RESET_ZONE = ZoneId.of("America/Los_Angeles");

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String bulkModel;
    private final String gradingModel;
    private final List<String> extraFallbacks;

    /**
     * Models that are rate-limited right now, and when that lifts.
     *
     * <p>
     * Covers both free-tier limits, because both make the model the wrong one to
     * try next: a
     * day's quota lifts at midnight Pacific, a per-minute limit in the seconds
     * Google names. Only
     * the daily one used to be remembered, so every single request kept
     * rediscovering the
     * per-minute limit and waiting out its retry delay again.
     *
     * <p>
     * Kept in memory only: a restart costs one wasted call per model to rediscover.
     */
    private final Map<String, Instant> unavailableUntil = new ConcurrentHashMap<>();

    public GeminiClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-3.5-flash}") String model,
            @Value("${app.ai.gemini.bulk-model:gemini-3.5-flash-lite}") String bulkModel,
            @Value("${app.ai.gemini.grading-model:gemini-3.5-flash-lite}") String gradingModel,
            @Value("${app.ai.gemini.fallback-models:}") String fallbackModels) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.apiKey = apiKey;
        this.model = model;
        this.bulkModel = bulkModel;
        this.gradingModel = gradingModel;
        this.extraFallbacks = Arrays.stream(fallbackModels.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Which model each kind of request would use right now, and why any of them is
     * out of play.
     *
     * <p>
     * The chain reorders itself as limits are hit, and until now the only way to
     * see that was
     * to read the server log. Two questions about "which model is it on?" went
     * unanswered for want
     * of this.
     */
    public List<ModelStatusDto> modelStatus() {
        List<String> chain = modelChain(model);
        List<ModelStatusDto> status = new ArrayList<>();
        for (int i = 0; i < chain.size(); i++) {
            String candidate = chain.get(i);
            Instant until = unavailableUntil.get(candidate);
            boolean available = !isUnavailable(candidate);
            status.add(new ModelStatusDto(
                    candidate,
                    i == 0,
                    available,
                    available ? null : until,
                    available ? "san sang" : "dang bi gioi han, cho den " + until));
        }
        return status;
    }

    /**
     * Lighter model for high-volume work (bulk vocabulary), which has a far larger
     * free-tier quota.
     */
    public String bulkModel() {
        return bulkModel;
    }

    /**
     * Model used to mark a learner's answer.
     *
     * <p>
     * Separate from {@link #model} because grading is the one call a learner waits
     * on: the heavier
     * model writes deeper feedback but took around 8s against roughly 1.6s, and a
     * pause that long
     * between answering and seeing the score is felt on every single sentence.
     * Separate from
     * {@link #bulkModel} too, even though both default to the same lighter model,
     * so that changing
     * how vocabulary is generated in bulk never silently changes how answers are
     * marked.
     */
    public String gradingModel() {
        return gradingModel;
    }

    public String generateJson(String prompt) {
        return generateJson(prompt, model);
    }

    /**
     * Sends a prompt and asks Gemini to reply with raw JSON, falling through to the
     * next model when
     * the current one is out of quota.
     *
     * <p>
     * The free tier caps each model separately (the main one allows only 20
     * requests a day), so a
     * single exhausted model must not take the whole feature down. Only quota and
     * overload failures
     * move on: a malformed request would fail identically everywhere, and retrying
     * it would just
     * burn the remaining models' quota too.
     */
    public String generateJson(String prompt, String startModel) {
        List<String> chain = modelChain(startModel);
        GeminiException lastFailure = null;

        for (int i = 0; i < chain.size(); i++) {
            String candidate = chain.get(i);
            boolean hasAnother = i < chain.size() - 1;
            try {
                // With another model in hand, a rate limit is not worth waiting out in place.
                String json = callOnce(prompt, candidate, hasAnother);
                unavailableUntil.remove(candidate); // it answered, so any marker was stale
                return json;
            } catch (GeminiException ex) {
                rememberUnavailable(candidate, ex);
                if (!ex.isWorthAnotherModel() || !hasAnother) {
                    throw chain.size() > 1 ? withTriedModels(ex, chain, i) : ex;
                }
                log.warn("Model {} khong dung duoc ({}), chuyen sang {}",
                        candidate, ex.getStatus(), chain.get(i + 1));
                lastFailure = ex;
            }
        }
        throw lastFailure; // unreachable: the loop either returns or throws
    }

    /**
     * The requested model first, then the other configured one, then any extras
     * from config —
     * except that models known to be out of quota for today sink to the end of the
     * list.
     *
     * <p>
     * They are moved rather than dropped, so a marker that turns out to be wrong
     * costs one
     * wasted call at worst instead of taking the feature down. {@code List.sort} is
     * stable, so the
     * configured order still holds inside each group.
     */
    List<String> modelChain(String startModel) {
        List<String> chain = new ArrayList<>();
        for (String candidate : Stream.concat(
                Stream.of(startModel, model, gradingModel, bulkModel), extraFallbacks.stream()).toList()) {
            if (candidate != null && !candidate.isBlank() && !chain.contains(candidate)) {
                chain.add(candidate);
            }
        }
        chain.sort(Comparator.comparing(this::isUnavailable)); // false sorts before true
        return chain;
    }

    /**
     * Notes how long this model is out of play, when the failure said anything
     * about it.
     */
    void rememberFailure(String model, GeminiException ex) {
        rememberUnavailable(model, ex);
    }

    private void rememberUnavailable(String model, GeminiException ex) {
        if (ex.isDailyQuotaExhausted()) {
            markUnavailable(model,
                    LocalDate.now(QUOTA_RESET_ZONE).plusDays(1).atStartOfDay(QUOTA_RESET_ZONE).toInstant(),
                    "het quota ngay");
        } else if (ex.getRetryAfter() != null) {
            markUnavailable(model, Instant.now().plus(ex.getRetryAfter()), "cham gioi han moi phut");
        }
    }

    void markDailyQuotaExhausted(String model) {
        markUnavailable(model,
                LocalDate.now(QUOTA_RESET_ZONE).plusDays(1).atStartOfDay(QUOTA_RESET_ZONE).toInstant(),
                "het quota ngay");
    }

    private void markUnavailable(String model, Instant until, String reason) {
        unavailableUntil.put(model, until);
        log.warn("Model {} {} — uu tien model khac den {}", model, reason, until);
    }

    private boolean isUnavailable(String model) {
        Instant until = unavailableUntil.get(model);
        if (until == null) {
            return false;
        }
        if (Instant.now().isBefore(until)) {
            return true;
        }
        unavailableUntil.remove(model); // the limit has lifted since
        return false;
    }

    /**
     * Says which models were tried, so "hết quota" does not look like a
     * single-model problem.
     */
    private GeminiException withTriedModels(GeminiException ex, List<String> chain, int reached) {
        if (reached == 0) {
            return ex;
        }
        String tried = String.join(", ", chain.subList(0, reached + 1));
        return new GeminiException(
                ex.getMessage() + " Da thu lan luot cac model: " + tried + ".",
                ex, ex.getStatus(), ex.isDailyQuotaExhausted());
    }

    /**
     * One request to one model. Asks Gemini to reply with raw JSON (via
     * responseMimeType) and
     * returns that JSON as a string for the caller to parse into its own DTO.
     */
    @SuppressWarnings("unchecked")
    private String callOnce(String prompt, String model, boolean hasFallback) {
        if (!isConfigured()) {
            throw new GeminiException("Chưa cấu hình GEMINI_API_KEY trên server");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json"));

        Map<String, Object> response;
        try {
            response = webClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(90))
                    .retryWhen(retrySpec(hasFallback))
                    .block();
        } catch (Exception ex) {
            Throwable root = rootCause(ex);
            log.warn("Gemini request failed: {}", root.toString());
            int status = root instanceof WebClientResponseException apiError
                    ? apiError.getStatusCode().value()
                    : 0;
            boolean outOfDailyQuota = isDailyQuota(root);
            throw new GeminiException(describe(root, model, outOfDailyQuota), root, status,
                    outOfDailyQuota, rateLimitPause(root));
        }

        if (response == null) {
            throw new GeminiException("Gemini không trả về dữ liệu");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            log.warn("Gemini returned no candidates. Response: {}", response);
            throw new GeminiException("Gemini không trả về nội dung, có thể bị bộ lọc an toàn chặn ("
                    + response.get("promptFeedback") + ")");
        }

        Map<String, Object> candidate = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
        List<Map<String, Object>> parts = content == null ? null : (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            Object finishReason = candidate.get("finishReason");
            log.warn("Gemini returned empty content. finishReason={}, candidate={}", finishReason, candidate);
            if ("MAX_TOKENS".equals(finishReason)) {
                throw new GeminiException("Gemini bị cắt do vượt giới hạn độ dài đầu ra — hãy giảm số từ mỗi lần sinh");
            }
            throw new GeminiException("Gemini trả về nội dung rỗng (finishReason=" + finishReason + ")");
        }

        String text = (String) parts.get(0).get("text");
        if (text == null || text.isBlank()) {
            throw new GeminiException("Gemini trả về văn bản rỗng");
        }
        return text;
    }

    /**
     * Retries transient failures. For 429 the free tier tells us how long to wait
     * (often ~45s),
     * so we honour that instead of a short fixed backoff which would just burn the
     * quota further.
     */
    /**
     * @param hasFallback whether another model is queued behind this one. When
     *                    there is, a rate
     *                    limit is not waited out: switching takes a second, while
     *                    Google's
     *                    suggested pause is tens of seconds and applies to this
     *                    model alone.
     *                    Waiting only makes sense as a last resort, with nothing
     *                    else to try.
     */
    private Retry retrySpec(boolean hasFallback) {
        return Retry.from(signals -> signals.flatMap(signal -> {
            Throwable failure = signal.failure();
            long attempt = signal.totalRetries() + 1;
            boolean rateLimited = isRateLimited(failure);
            long maxAttempts = rateLimited ? MAX_RATE_LIMIT_RETRIES : MAX_TRANSIENT_RETRIES;

            // A day's quota does not come back by waiting at all.
            if (isDailyQuota(failure)) {
                log.warn("Gemini het quota ngay — bo qua retry, doi model ngay");
                return Mono.<Long>error(failure);
            }
            // Same for an overloaded model: 503 is about this endpoint, and another model
            // is a
            // second away while the backoff here runs to seconds. Timeouts and dropped
            // connections
            // are not in this list — those are the network, which every model shares.
            if (hasFallback && (rateLimited || isOverloaded(failure))) {
                log.warn("Gemini tu choi ({}) — doi model ngay thay vi cho",
                        rateLimited ? "cham gioi han" : "qua tai");
                return Mono.<Long>error(failure);
            }
            if (!isTransient(failure) || attempt > maxAttempts) {
                return Mono.<Long>error(failure);
            }

            Duration delay = retryDelayFor(failure, attempt);
            log.warn("Gemini call failed ({}), retry {}/{} after {}s",
                    failure.getClass().getSimpleName(), attempt, maxAttempts, delay.toSeconds());
            return Mono.delay(delay);
        }));
    }

    /**
     * How long a rate limit says to wait, or null when this is not a rate limit.
     */
    private Duration rateLimitPause(Throwable failure) {
        if (!isRateLimited(failure)) {
            return null;
        }
        Duration suggested = failure instanceof WebClientResponseException apiError
                ? parseRetryDelay(apiError.getResponseBodyAsString())
                : null;
        return suggested != null ? suggested : DEFAULT_RATE_LIMIT_PAUSE;
    }

    private Duration retryDelayFor(Throwable failure, long attempt) {
        if (isRateLimited(failure) && failure instanceof WebClientResponseException apiError) {
            Duration suggested = parseRetryDelay(apiError.getResponseBodyAsString());
            Duration delay = suggested != null ? suggested.plusSeconds(2) : Duration.ofSeconds(30);
            return delay.compareTo(MAX_RATE_LIMIT_WAIT) > 0 ? MAX_RATE_LIMIT_WAIT : delay;
        }
        return Duration.ofSeconds(Math.min(2L * attempt * attempt, 15));
    }

    /**
     * Google returns a RetryInfo detail plus a "Please retry in 45.7s." hint in the
     * message.
     */
    private Duration parseRetryDelay(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode error = objectMapper.readTree(responseBody).path("error");
            for (JsonNode detail : error.path("details")) {
                String retryDelay = detail.path("retryDelay").asText("");
                if (retryDelay.endsWith("s")) {
                    return Duration.ofSeconds((long) Math.ceil(Double.parseDouble(retryDelay.replace("s", ""))));
                }
            }
            Matcher matcher = RETRY_HINT.matcher(error.path("message").asText(""));
            if (matcher.find()) {
                return Duration.ofSeconds((long) Math.ceil(Double.parseDouble(matcher.group(1))));
            }
        } catch (Exception ignored) {
            // fall through to the default delay
        }
        return null;
    }

    /**
     * Whether a 429 means "nothing left today" rather than "too fast, slow down".
     * The two need
     * opposite handling, and Google says which one it hit in the QuotaFailure
     * violations it
     * returns, naming them like
     * {@code GenerateRequestsPerDayPerProjectPerModel-FreeTier} against
     * {@code ...PerMinute...}.
     *
     * <p>
     * Rather than depend on one field keeping that exact name, this scans every
     * text value of
     * the violation. It stays inside the structured violation on purpose: the
     * human-readable
     * message often mentions the daily limit even when the per-minute one is what
     * was hit.
     *
     * <p>
     * Anything unrecognised counts as per-minute, which merely keeps the old
     * waiting behaviour.
     * The opposite default would bench a model that still has quota left.
     */
    boolean isDailyQuota(Throwable ex) {
        if (!isRateLimited(ex)) {
            return false;
        }
        String body = ((WebClientResponseException) ex).getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return false;
        }
        try {
            for (JsonNode detail : objectMapper.readTree(body).path("error").path("details")) {
                for (JsonNode violation : detail.path("violations")) {
                    for (JsonNode value : violation) {
                        if (value.isTextual() && namesADailyQuota(value.asText())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // an unparseable body tells us nothing; fall through to "not daily"
        }
        return false;
    }

    private static boolean namesADailyQuota(String value) {
        return value.toLowerCase().replace(" ", "").replace("_", "").contains("perday");
    }

    private static boolean isOverloaded(Throwable ex) {
        return ex instanceof WebClientResponseException apiError
                && apiError.getStatusCode().value() == 503;
    }

    private static boolean isRateLimited(Throwable ex) {
        return ex instanceof WebClientResponseException apiError && apiError.getStatusCode().value() == 429;
    }

    /**
     * Turns the underlying transport/API failure into a message that says what
     * actually went wrong.
     */
    private String describe(Throwable ex, String usedModel, boolean outOfDailyQuota) {
        if (ex instanceof TimeoutException) {
            return "Gemini phản hồi quá lâu (quá 90 giây) — thử giảm số từ mỗi lần sinh";
        }
        if (ex instanceof WebClientRequestException) {
            return "Không kết nối được tới Gemini (lỗi mạng): " + ex.getMessage();
        }
        if (ex instanceof WebClientResponseException apiError) {
            int status = apiError.getStatusCode().value();
            String detail = extractApiMessage(apiError.getResponseBodyAsString());
            return switch (status) {
                case 429 -> outOfDailyQuota
                        ? "Model '" + usedModel + "' đã dùng hết quota miễn phí của hôm nay "
                                + "(reset vào nửa đêm giờ Thái Bình Dương)." + detail
                        : "Chạm giới hạn số request mỗi phút của Gemini cho model '" + usedModel
                                + "' — đã chờ và thử lại " + MAX_RATE_LIMIT_RETRIES
                                + " lần vẫn bị chặn." + detail;
                case 503 -> "Gemini đang quá tải (503) — đã thử lại " + MAX_TRANSIENT_RETRIES
                        + " lần vẫn không được, thử lại sau ít phút." + detail;
                case 500, 502, 504 -> "Gemini gặp lỗi máy chủ (" + status + ") — thử lại sau." + detail;
                case 400 -> "Gemini từ chối yêu cầu (400 - dữ liệu gửi lên không hợp lệ)." + detail;
                case 401, 403 -> "API key Gemini không hợp lệ hoặc không có quyền (" + status + ")." + detail;
                case 404 -> "Model '" + usedModel + "' không tồn tại hoặc không còn khả dụng." + detail;
                default -> "Gemini trả về lỗi HTTP " + status + "." + detail;
            };
        }
        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
    }

    /**
     * Google returns {"error": {"message": "..."}} — surface that text when
     * present.
     */
    private String extractApiMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            JsonNode message = objectMapper.readTree(responseBody).path("error").path("message");
            return message.isMissingNode() || message.asText().isBlank() ? "" : " Chi tiết: " + message.asText();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isTransient(Throwable ex) {
        if (ex instanceof TimeoutException) {
            return true;
        }
        // A reset or dropped connection says nothing about the request — retrying is
        // worth a try.
        if (ex instanceof java.net.SocketException || ex instanceof WebClientRequestException) {
            return true;
        }
        if (!(ex instanceof WebClientResponseException apiError)) {
            return false;
        }
        int status = apiError.getStatusCode().value();
        return status == 429 || status >= 500;
    }
}
