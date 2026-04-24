package com.quiz.model;

/**
 * Represents one participant's entry in the final leaderboard.
 * Sorted descending by totalScore before submission.
 */
public class LeaderboardEntry implements Comparable<LeaderboardEntry> {

    private String participant;
    private int totalScore;

    public LeaderboardEntry() {}

    public LeaderboardEntry(String participant, int totalScore) {
        this.participant = participant;
        this.totalScore  = totalScore;
    }

    public String getParticipant()  { return participant; }
    public int    getTotalScore()   { return totalScore; }

    public void setParticipant(String participant)   { this.participant = participant; }
    public void setTotalScore(int totalScore)        { this.totalScore = totalScore; }

    /** Descending order by totalScore for leaderboard ranking. */
    @Override
    public int compareTo(LeaderboardEntry other) {
        return Integer.compare(other.totalScore, this.totalScore);
    }

    @Override
    public String toString() {
        return String.format("LeaderboardEntry{participant='%s', totalScore=%d}", participant, totalScore);
    }
}
