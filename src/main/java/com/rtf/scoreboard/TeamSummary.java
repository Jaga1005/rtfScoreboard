package com.rtf.scoreboard;

import java.util.Objects;

public record TeamSummary(
        String teamName,
        long matchesPlayed,
        long matchesWon,
        long matchesLost,
        long matchesDrawn,
        long goalsScored) {

    public TeamSummary {
        Objects.requireNonNull(teamName);
    }

    public String getTeamName() { return teamName; }
    public long getMatchesPlayed() { return matchesPlayed; }
    public long getMatchesWon() { return matchesWon; }
    public long getMatchesLost() { return matchesLost; }
    public long getMatchesDrawn() { return matchesDrawn; }
    public long getGoalsScored() { return goalsScored; }
}
