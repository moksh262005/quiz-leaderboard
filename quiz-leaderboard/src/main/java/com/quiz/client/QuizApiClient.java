package com.quiz.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.model.PollResponse;
import com.quiz.model.SubmitRequest;
import com.quiz.model.SubmitResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Low-level HTTP client that wraps the two quiz endpoints:
 *   GET  /quiz/messages  — fetch events for a single poll index
 *   POST /quiz/submit    — submit the final leaderboard (called exactly once)
 */
public class QuizApiClient {

    private static final String BASE_URL      = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private static final int    TOTAL_POLLS   = 10;
    private static final long   POLL_DELAY_MS = 5_000; // 5 seconds between polls

    private final String     regNo;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public QuizApiClient(String regNo) {
        this.regNo = regNo;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // Polling
    // -------------------------------------------------------------------------

    /**
     * Polls the API exactly {@code TOTAL_POLLS} times (poll indices 0–9),
     * maintaining the mandatory 5-second delay between each call.
     *
     * @return array of PollResponse objects, one per poll index
     */
    public PollResponse[] fetchAllPolls() throws IOException, InterruptedException {
        PollResponse[] responses = new PollResponse[TOTAL_POLLS];

        for (int i = 0; i < TOTAL_POLLS; i++) {
            System.out.printf("[Poll %d/%d] Fetching poll index %d ...%n", i + 1, TOTAL_POLLS, i);
            responses[i] = fetchSinglePoll(i);
            System.out.printf("  → Received %d event(s) in poll %d%n",
                    responses[i].getEvents() == null ? 0 : responses[i].getEvents().size(), i);

            // Mandatory 5-second delay between polls (not needed after the last one)
            if (i < TOTAL_POLLS - 1) {
                System.out.println("  → Waiting 5 seconds before next poll...");
                Thread.sleep(POLL_DELAY_MS);
            }
        }

        return responses;
    }

    /**
     * Fetches a single poll from the API.
     *
     * @param pollIndex 0–9
     */
    public PollResponse fetchSinglePoll(int pollIndex) throws IOException, InterruptedException {
        String url = String.format("%s/quiz/messages?regNo=%s&poll=%d", BASE_URL, regNo, pollIndex);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        int maxRetries = 5;
        long delay = 2000; // start with 2 sec

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return mapper.readValue(response.body(), PollResponse.class);
                }

                throw new IOException("HTTP " + response.statusCode());

            } catch (Exception e) {
                System.out.println("Retry " + attempt + " for poll " + pollIndex + " (waiting " + delay/1000 + "s)");

                if (attempt == maxRetries) {
                    throw e;
                }

                Thread.sleep(delay);
                delay *= 2; // exponential backoff (2s → 4s → 8s → ...)
            }
        }

        throw new IOException("Failed after retries");
    }

    // -------------------------------------------------------------------------
    // Submission
    // -------------------------------------------------------------------------

    /**
     * Submits the final leaderboard exactly once.
     *
     * @param submitRequest the leaderboard payload
     * @return the validator's response
     */
    public SubmitResponse submitLeaderboard(SubmitRequest submitRequest)
            throws IOException, InterruptedException {

        String body = mapper.writeValueAsString(submitRequest);
        System.out.println("Payload JSON: " + body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        System.out.println("[Submit] Posting leaderboard to /quiz/submit ...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        validateStatus(response, "POST /quiz/submit");

        return mapper.readValue(response.body(), SubmitResponse.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateStatus(HttpResponse<String> response, String endpoint) throws IOException {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException(
                    String.format("HTTP %d from %s: %s", status, endpoint, response.body()));
        }
    }
}
