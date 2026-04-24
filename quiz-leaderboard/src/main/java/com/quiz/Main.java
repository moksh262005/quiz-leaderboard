package com.quiz;

import com.quiz.client.QuizApiClient;
import com.quiz.model.LeaderboardEntry;
import com.quiz.model.PollResponse;
import com.quiz.model.SubmitRequest;
import com.quiz.model.SubmitResponse;
import com.quiz.service.LeaderboardProcessor;

import java.util.List;

/**
 * Entry point for the Quiz Leaderboard System.
 *
 * Flow:
 *   1. Poll GET /quiz/messages 10 times (poll indices 0–9) with 5-second delays.
 *   2. Deduplicate events using (roundId + participant) as the unique key.
 *   3. Aggregate scores per participant.
 *   4. Sort leaderboard descending by totalScore.
 *   5. Submit leaderboard exactly once via POST /quiz/submit.
 *   6. Print the validator's response.
 *
 */
public class Main {

    public static void main(String[] args) {
        // ── 0. Read registration number from args or use default ──────────────
        String regNo = (args.length > 0) ? args[0] : "2024CS101";
        System.out.println("      Quiz Leaderboard System — SRM       ");
        System.out.printf("Registration Number : %s%n%n", regNo);

        QuizApiClient       apiClient = new QuizApiClient(regNo);
        LeaderboardProcessor processor = new LeaderboardProcessor();

        try {
            // ── 1. Poll the API 10 times ──────────────────────────────────────
            System.out.println("─── Step 1: Polling API (10 polls, 5-second delay each) ───");
            PollResponse[] polls = apiClient.fetchAllPolls();

            // ── 2–4. Deduplicate, aggregate, sort ─────────────────────────────
            System.out.println("\n─── Step 2: Processing events & building leaderboard ──────");
            List<LeaderboardEntry> leaderboard = processor.process(polls);

            int grandTotal = processor.computeGrandTotal(leaderboard);

            // ── 5. Print leaderboard before submitting ────────────────────────
            System.out.println("─── Final Leaderboard (before submission) ──────────────────");
            System.out.printf("%-5s %-20s %s%n", "Rank", "Participant", "Total Score");
            System.out.println("─".repeat(40));
            for (int i = 0; i < leaderboard.size(); i++) {
                LeaderboardEntry entry = leaderboard.get(i);
                System.out.printf("%-5d %-20s %d%n", i + 1, entry.getParticipant(), entry.getTotalScore());
            }
            System.out.println("─".repeat(40));
            System.out.printf("%-26s %d%n%n", "GRAND TOTAL:", grandTotal);

            // ── 6. Submit once ────────────────────────────────────────────────
            System.out.println("─── Step 3: Submitting leaderboard ─────────────────────────");
            SubmitRequest  request  = new SubmitRequest(regNo, leaderboard);
            SubmitResponse response = apiClient.submitLeaderboard(request);

            // ── 7. Print result ───────────────────────────────────────────────
            System.out.println("\n─── Validator Response ──────────────────────────────────────");
            System.out.println(response);
            System.out.println();

            if (response.isCorrect()) {
                System.out.println("✅  SUCCESS: Leaderboard accepted!");
            } else {
                System.out.printf("❌  INCORRECT: submitted=%d, expected=%d%n",
                        response.getSubmittedTotal(), response.getExpectedTotal());
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
