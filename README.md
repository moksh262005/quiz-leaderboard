# Quiz Leaderboard System

Overview
This project implements a Quiz Leaderboard System that:
- Polls an external API 10 times with delay
- Deduplicates events using (roundId + participant)
- Aggregates scores per participant
- Generates a sorted leaderboard
- Submits results to validator API

---
Tech Stack
- Java
- Maven
- HTTP Client (Java 11+)
- Jackson (JSON parsing)

---

Workflow

1. Poll API 10 times with 5-second delay
2. Collect events from all polls
3. Remove duplicate events
4. Aggregate scores per participant
5. Sort leaderboard (descending order)
6. Submit final leaderboard

---

Features

- ✅ Deduplication using composite key
- ✅ Robust API handling with retry logic (502/503)
- ✅ Clean modular design
- ✅ Error handling & logging

---

#How to Run

```bash
mvn clean package
cd target
java -jar quiz-leaderboard-1.0-SNAPSHOT.jar
