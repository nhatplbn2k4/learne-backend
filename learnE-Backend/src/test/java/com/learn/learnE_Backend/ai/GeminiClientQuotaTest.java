package com.learn.learnE_Backend.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The free tier caps each model separately and per day, so telling "no requests left until
 * tomorrow" apart from "too many requests this minute" decides whether waiting can ever help.
 */
class GeminiClientQuotaTest {

    /** A real free-tier 429: the daily cap on the main model is 20 requests. */
    private static final String PER_DAY_BODY = """
            {"error": {"code": 429, "status": "RESOURCE_EXHAUSTED",
              "message": "You exceeded your current quota.",
              "details": [
                {"@type": "type.googleapis.com/google.rpc.QuotaFailure",
                 "violations": [{
                   "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
                   "quotaId": "GenerateRequestsPerDayPerProjectPerModel-FreeTier",
                   "quotaValue": "20"}]},
                {"@type": "type.googleapis.com/google.rpc.RetryInfo", "retryDelay": "45s"}
              ]}}
            """;

    private static final String PER_MINUTE_BODY = """
            {"error": {"code": 429, "status": "RESOURCE_EXHAUSTED",
              "message": "You exceeded your current quota.",
              "details": [
                {"@type": "type.googleapis.com/google.rpc.QuotaFailure",
                 "violations": [{
                   "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
                   "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
                   "quotaValue": "15"}]},
                {"@type": "type.googleapis.com/google.rpc.RetryInfo", "retryDelay": "31s"}
              ]}}
            """;

    private static GeminiClient client() {
        return new GeminiClient(WebClient.builder(), "test-key", "flash", "flash-lite", "pro,legacy");
    }

    private static GeminiException perMinuteFailure() {
        return new GeminiException(
                "cham gioi han moi phut", null, 429, false, java.time.Duration.ofSeconds(45));
    }

    private static WebClientResponseException httpError(HttpStatus status, String body) {
        return WebClientResponseException.create(
                status.value(), status.getReasonPhrase(), null,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    // ---- telling the two 429s apart ----

    @Test
    void recognisesADailyQuotaFailure() {
        assertThat(client().isDailyQuota(httpError(HttpStatus.TOO_MANY_REQUESTS, PER_DAY_BODY))).isTrue();
    }

    @Test
    void perMinuteRateLimitIsNotADailyQuota() {
        assertThat(client().isDailyQuota(httpError(HttpStatus.TOO_MANY_REQUESTS, PER_MINUTE_BODY))).isFalse();
    }

    /** Without a recognisable violation we keep the old behaviour: wait and retry the same model. */
    @Test
    void unrecognisedBodyIsTreatedAsPerMinute() {
        assertThat(client().isDailyQuota(httpError(HttpStatus.TOO_MANY_REQUESTS, "not json at all"))).isFalse();
        assertThat(client().isDailyQuota(httpError(HttpStatus.TOO_MANY_REQUESTS, "{}"))).isFalse();
    }

    @Test
    void onlyA429CanBeAQuotaFailure() {
        assertThat(client().isDailyQuota(httpError(HttpStatus.SERVICE_UNAVAILABLE, PER_DAY_BODY))).isFalse();
        assertThat(client().isDailyQuota(new RuntimeException("connection refused"))).isFalse();
    }

    // ---- remembering which model is spent ----

    @Test
    void chainStartsWithTheRequestedModelThenTheConfiguredFallbacks() {
        assertThat(client().modelChain("flash"))
                .containsExactly("flash", "flash-lite", "pro", "legacy");
    }

    @Test
    void anExhaustedModelSinksToTheEndInsteadOfBeingDropped() {
        GeminiClient client = client();
        client.markDailyQuotaExhausted("flash");

        // Still present, so a marker set by mistake costs one wasted call rather than the feature.
        assertThat(client.modelChain("flash"))
                .containsExactly("flash-lite", "pro", "legacy", "flash");
    }

    @Test
    void theRemainingModelsKeepTheirConfiguredOrder() {
        GeminiClient client = client();
        client.markDailyQuotaExhausted("flash");
        client.markDailyQuotaExhausted("pro");

        assertThat(client.modelChain("flash"))
                .containsExactly("flash-lite", "legacy", "flash", "pro");
    }

    /**
     * A per-minute limit has to be remembered too. It used to be re-discovered on every request,
     * each one waiting out Google's suggested pause before failing over — which is what made
     * grading crawl once the main model started rate-limiting.
     */
    @Test
    void aPerMinuteLimitAlsoTakesTheModelOutOfPlay() {
        GeminiClient client = client();
        client.rememberFailure("flash", perMinuteFailure());

        assertThat(client.modelChain("flash")).containsExactly("flash-lite", "pro", "legacy", "flash");
    }

    /** The pause is short, so the model comes back on its own without anyone clearing it. */
    @Test
    void aPerMinuteLimitLiftsByItself() throws InterruptedException {
        GeminiClient client = client();
        client.rememberFailure("flash", new GeminiException(
                "cham gioi han", null, 429, false, java.time.Duration.ofMillis(120)));

        assertThat(client.modelChain("flash")).startsWith("flash-lite");
        Thread.sleep(200);
        assertThat(client.modelChain("flash")).startsWith("flash");
    }

    /** A failure that says nothing about waiting must not bench the model. */
    @Test
    void anOrdinaryFailureLeavesTheOrderAlone() {
        GeminiClient client = client();
        client.rememberFailure("flash", new GeminiException("mang loi", null, 0));

        assertThat(client.modelChain("flash")).startsWith("flash");
    }

    @Test
    void everyModelExhaustedStillTriesThemAll() {
        GeminiClient client = client();
        for (String model : client.modelChain("flash")) {
            client.markDailyQuotaExhausted(model);
        }

        assertThat(client.modelChain("flash")).containsExactlyInAnyOrder("flash", "flash-lite", "pro", "legacy");
    }
}
