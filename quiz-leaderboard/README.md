# Quiz Leaderboard System — SRM Internship Assignment

A Java application that polls a quiz validator API, **deduplicates** events, aggregates scores, and submits a final leaderboard.

---

## Problem Summary

The validator API delivers quiz events (participant scores per round) across 10 polls. The **same event may appear in multiple polls**. The application must:

1. Poll 10 times with a mandatory 5-second delay between calls
2. Deduplicate events using `(roundId + participant)` as the unique key
3. Aggregate scores per participant
4. Sort the leaderboard descending by total score
5. Submit exactly once

---

## Project Structure

```
quiz-leaderboard/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/quiz/
    │   ├── Main.java                        ← Entry point
    │   ├── client/
    │   │   └── QuizApiClient.java           ← HTTP client (poll + submit)
    │   ├── model/
    │   │   ├── QuizEvent.java               ← Single event (roundId, participant, score)
    │   │   ├── PollResponse.java            ← Response from GET /quiz/messages
    │   │   ├── LeaderboardEntry.java        ← One row in the final leaderboard
    │   │   ├── SubmitRequest.java           ← POST body for /quiz/submit
    │   │   └── SubmitResponse.java          ← Response from /quiz/submit
    │   └── service/
    │       └── LeaderboardProcessor.java    ← Core deduplication & aggregation logic
    └── test/java/com/quiz/
        └── LeaderboardProcessorTest.java    ← Unit tests (no HTTP calls)
```

---

## How It Works

### Deduplication Key

Each event is uniquely identified by `roundId + "::" + participant`.

```
Poll 1 → { roundId="R1", participant="Alice", score=10 }  ← COUNTED
Poll 3 → { roundId="R1", participant="Alice", score=10 }  ← DUPLICATE → IGNORED
```

A `HashSet<String>` tracks all seen keys. The first occurrence is added to the score map; any subsequent occurrence with the same key is silently discarded.

### Aggregation

```java
scoreMap.merge(event.getParticipant(), event.getScore(), Integer::sum);
```

### Leaderboard Sorting

`LeaderboardEntry` implements `Comparable` with descending order by `totalScore`.

---

## Prerequisites

- Java 11+
- Maven 3.6+

---

## Build & Run

### 1. Build

```bash
mvn clean package -q
```

This produces `target/quiz-leaderboard-1.0-SNAPSHOT.jar` (fat JAR with all dependencies).

### 2. Run

```bash
java -jar target/quiz-leaderboard-1.0-SNAPSHOT.jar <your-regNo>
# e.g.:
java -jar target/quiz-leaderboard-1.0-SNAPSHOT.jar 2024CS101
```

The application will:
- Poll 10 times with 5-second delays (~50 seconds total)
- Print duplicate events as they are found
- Print the final leaderboard
- Submit once and print the validator's response

### 3. Run Tests

```bash
mvn test
```

Tests cover:
- Basic aggregation across multiple polls
- Duplicate events being ignored
- Grand total calculation
- Leaderboard sort order
- Edge cases (null events, empty polls, all-duplicate polls)

---

## Example Output

```
╔══════════════════════════════════════════╗
║      Quiz Leaderboard System — SRM       ║
╚══════════════════════════════════════════╝
Registration Number : 2024CS101

─── Step 1: Polling API (10 polls, 5-second delay each) ───
[Poll 1/10] Fetching poll index 0 ...
  → Received 2 event(s) in poll 0
  → Waiting 5 seconds before next poll...
...

─── Step 2: Processing events & building leaderboard ──────
  [DUPLICATE] Skipping R1::Alice (score=10)

========== Processing Summary ==========
Total events received across all polls : 25
Duplicate events discarded             : 5
Unique events processed                : 20
Unique participants                    : 4
========================================

─── Final Leaderboard (before submission) ──────────────────
Rank  Participant          Total Score
────────────────────────────────────────
1     Bob                  120
2     Alice                100
3     Carol                 80
4     Dave                  60
────────────────────────────────────────
GRAND TOTAL:               360

─── Step 3: Submitting leaderboard ─────────────────────────
[Submit] Posting leaderboard to /quiz/submit ...

─── Validator Response ──────────────────────────────────────
SubmitResponse{isCorrect=true, isIdempotent=true, submittedTotal=360, expectedTotal=360, message='Correct!'}

✅  SUCCESS: Leaderboard accepted!
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| `HashSet` for deduplication | O(1) lookup; handles any number of duplicate polls |
| `LinkedHashMap` for score accumulation | Preserves insertion order during aggregation |
| `Comparable` on `LeaderboardEntry` | Clean, reusable sort logic |
| Java 11 `HttpClient` | No extra HTTP library needed; clean async-capable API |
| Fat JAR via Maven Shade | Single portable artifact, no classpath issues |
| Unit tests with no HTTP | Fast, deterministic, CI-friendly |
