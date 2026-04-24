package com.quiz.service;

import com.quiz.model.LeaderboardEntry;
import com.quiz.model.PollResponse;
import com.quiz.model.QuizEvent;

import java.util.*;

/**
 * Core business logic for the quiz leaderboard system.
 *
 * Responsibilities:
 *  1. Deduplicate events across all polls using (roundId + participant) as the unique key.
 *  2. Aggregate total scores per participant.
 *  3. Produce a leaderboard sorted descending by totalScore.
 *  4. Compute the grand total across all participants.
 *
 * Design rationale for deduplication:
 *   The assignment states that "the same API response data may appear again in later polls".
 *   Each event is uniquely identified by (roundId, participant) — i.e., a participant can
 *   only score once per round. We track seen keys in a HashSet; the first occurrence is
 *   counted and any later occurrence of the same key is silently discarded.
 */
public class LeaderboardProcessor {

    /**
     * Processes all poll responses and returns a sorted leaderboard.
     *
     * @param pollResponses array of PollResponse objects (one per poll index 0–9)
     * @return sorted list of LeaderboardEntry (descending by totalScore)
     */
    public List<LeaderboardEntry> process(PollResponse[] pollResponses) {
        // Tracks which (roundId::participant) keys we have already counted
        Set<String> seenKeys = new HashSet<>();

        // Accumulates the total score for each participant
        Map<String, Integer> scoreMap = new LinkedHashMap<>();

        int totalEventsReceived  = 0;
        int totalDuplicatesFound = 0;

        for (PollResponse poll : pollResponses) {
            if (poll == null || poll.getEvents() == null) continue;

            for (QuizEvent event : poll.getEvents()) {
                totalEventsReceived++;
                String key = event.getDeduplicationKey();

                if (seenKeys.contains(key)) {
                    // Duplicate — skip
                    totalDuplicatesFound++;
                    System.out.printf("  [DUPLICATE] Skipping %s (score=%d)%n", key, event.getScore());
                } else {
                    // First time seeing this (roundId, participant) pair — count it
                    seenKeys.add(key);
                    scoreMap.merge(event.getParticipant(), event.getScore(), Integer::sum);
                }
            }
        }

        // Build sorted leaderboard (descending by score)
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : scoreMap.entrySet()) {
            leaderboard.add(new LeaderboardEntry(entry.getKey(), entry.getValue()));
        }
        Collections.sort(leaderboard); // Uses LeaderboardEntry.compareTo (descending)

        // Print a summary for debugging
        System.out.println("\n========== Processing Summary ==========");
        System.out.printf("Total events received across all polls : %d%n", totalEventsReceived);
        System.out.printf("Duplicate events discarded             : %d%n", totalDuplicatesFound);
        System.out.printf("Unique events processed                : %d%n", totalEventsReceived - totalDuplicatesFound);
        System.out.printf("Unique participants                    : %d%n", leaderboard.size());
        System.out.println("========================================\n");

        return leaderboard;
    }

    /**
     * Computes the sum of totalScore across all leaderboard entries.
     *
     * @param leaderboard sorted leaderboard list
     * @return grand total score
     */
    public int computeGrandTotal(List<LeaderboardEntry> leaderboard) {
        return leaderboard.stream()
                .mapToInt(LeaderboardEntry::getTotalScore)
                .sum();
    }
}
