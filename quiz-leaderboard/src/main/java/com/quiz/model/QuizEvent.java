package com.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a single quiz event (one participant's score in one round).
 * Used as the unit of deduplication: (roundId + participant) is the unique key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizEvent {

    private String roundId;
    private String participant;
    private int score;

    public QuizEvent() {}

    public QuizEvent(String roundId, String participant, int score) {
        this.roundId = roundId;
        this.participant = participant;
        this.score = score;
    }

    public String getRoundId()      { return roundId; }
    public String getParticipant()  { return participant; }
    public int    getScore()        { return score; }

    public void setRoundId(String roundId)          { this.roundId = roundId; }
    public void setParticipant(String participant)   { this.participant = participant; }
    public void setScore(int score)                  { this.score = score; }

    /**
     * The deduplication key: roundId + participant uniquely identifies an event.
     * If the same key appears again across multiple polls, it is a duplicate and must be ignored.
     */
    public String getDeduplicationKey() {
        return roundId + "::" + participant;
    }

    @Override
    public String toString() {
        return String.format("QuizEvent{roundId='%s', participant='%s', score=%d}", roundId, participant, score);
    }
}
