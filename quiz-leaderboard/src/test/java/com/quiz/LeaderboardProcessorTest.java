package com.quiz;

import com.quiz.model.LeaderboardEntry;
import com.quiz.model.PollResponse;
import com.quiz.model.QuizEvent;
import com.quiz.service.LeaderboardProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeaderboardProcessor.
 * These tests validate the core deduplication and aggregation logic
 * WITHOUT making any real HTTP calls.
 */
class LeaderboardProcessorTest {

    private LeaderboardProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new LeaderboardProcessor();
    }

    // ── Helper to build a PollResponse inline ────────────────────────────────
    private PollResponse makePoll(int pollIndex, QuizEvent... events) {
        PollResponse pr = new PollResponse();
        pr.setPollIndex(pollIndex);
        pr.setEvents(Arrays.asList(events));
        return pr;
    }

    private QuizEvent event(String roundId, String participant, int score) {
        return new QuizEvent(roundId, participant, score);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Basic aggregation: unique events across two polls are summed correctly")
    void testBasicAggregation() {
        PollResponse[] polls = {
            makePoll(0, event("R1", "Alice", 10), event("R1", "Bob", 20)),
            makePoll(1, event("R2", "Alice", 15), event("R2", "Bob",  5))
        };

        List<LeaderboardEntry> lb = processor.process(polls);

        assertEquals(2, lb.size());
        // Sorted descending: Bob=25, Alice=25 — tie; order may vary, check totals
        int aliceScore = lb.stream().filter(e -> e.getParticipant().equals("Alice"))
                           .mapToInt(LeaderboardEntry::getTotalScore).sum();
        int bobScore   = lb.stream().filter(e -> e.getParticipant().equals("Bob"))
                           .mapToInt(LeaderboardEntry::getTotalScore).sum();

        assertEquals(25, aliceScore);
        assertEquals(25, bobScore);
    }

    @Test
    @DisplayName("Duplicate events from different polls are deduplicated (same roundId + participant)")
    void testDuplicateEventsAreIgnored() {
        // Poll 0 and Poll 2 contain the exact same event for Alice in R1
        PollResponse[] polls = {
            makePoll(0, event("R1", "Alice", 10)),
            makePoll(1, event("R2", "Alice", 20)),
            makePoll(2, event("R1", "Alice", 10))   // duplicate — must be ignored
        };

        List<LeaderboardEntry> lb = processor.process(polls);

        assertEquals(1, lb.size());
        assertEquals("Alice", lb.get(0).getParticipant());
        assertEquals(30, lb.get(0).getTotalScore(), "R1(10) + R2(20) = 30, NOT 40");
    }

    @Test
    @DisplayName("Grand total matches sum of individual participant scores")
    void testGrandTotal() {
        PollResponse[] polls = {
            makePoll(0,
                event("R1", "Alice", 10),
                event("R1", "Bob",   20),
                event("R1", "Carol", 30))
        };

        List<LeaderboardEntry> lb = processor.process(polls);
        assertEquals(60, processor.computeGrandTotal(lb));
    }

    @Test
    @DisplayName("Leaderboard is sorted descending by totalScore")
    void testLeaderboardSortOrder() {
        PollResponse[] polls = {
            makePoll(0,
                event("R1", "Alice", 5),
                event("R1", "Bob",   50),
                event("R1", "Carol", 25))
        };

        List<LeaderboardEntry> lb = processor.process(polls);

        assertEquals("Bob",   lb.get(0).getParticipant());
        assertEquals("Carol", lb.get(1).getParticipant());
        assertEquals("Alice", lb.get(2).getParticipant());
    }

    @Test
    @DisplayName("All 10 polls returning the same event deduplicate to a single count")
    void testAllPollsDuplicate() {
        PollResponse[] polls = new PollResponse[10];
        for (int i = 0; i < 10; i++) {
            polls[i] = makePoll(i, event("R1", "Alice", 10));
        }

        List<LeaderboardEntry> lb = processor.process(polls);

        assertEquals(1, lb.size());
        assertEquals(10, lb.get(0).getTotalScore(),
                "Event should be counted exactly once regardless of how many polls repeat it");
    }

    @Test
    @DisplayName("Same participant in different rounds are NOT duplicates and are summed")
    void testSameParticipantDifferentRoundsNotDuplicate() {
        PollResponse[] polls = {
            makePoll(0, event("R1", "Alice", 10)),
            makePoll(1, event("R2", "Alice", 20)),
            makePoll(2, event("R3", "Alice", 30))
        };

        List<LeaderboardEntry> lb = processor.process(polls);
        assertEquals(60, lb.get(0).getTotalScore());
    }

    @Test
    @DisplayName("Null events list in a poll does not throw NullPointerException")
    void testNullEventsAreHandledGracefully() {
        PollResponse emptyPoll = new PollResponse();
        emptyPoll.setPollIndex(0);
        emptyPoll.setEvents(null);

        PollResponse[] polls = { emptyPoll, makePoll(1, event("R1", "Bob", 15)) };

        assertDoesNotThrow(() -> {
            List<LeaderboardEntry> lb = processor.process(polls);
            assertEquals(1, lb.size());
            assertEquals(15, lb.get(0).getTotalScore());
        });
    }

    @Test
    @DisplayName("Empty poll array returns empty leaderboard with zero grand total")
    void testEmptyPollArray() {
        List<LeaderboardEntry> lb = processor.process(new PollResponse[0]);
        assertTrue(lb.isEmpty());
        assertEquals(0, processor.computeGrandTotal(lb));
    }
}
