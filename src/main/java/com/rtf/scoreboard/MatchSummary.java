package com.rtf.scoreboard;

import java.time.Instant;
import java.util.Objects;

public record MatchSummary(
        String firstTeam,
        String secondTeam,
        int firstTeamScore,
        int secondTeamScore,
        Instant startTime) {

    public MatchSummary {
        Objects.requireNonNull(firstTeam);
        Objects.requireNonNull(secondTeam);
        Objects.requireNonNull(startTime);
    }

    public String getFirstTeam() { return firstTeam; }
    public String getSecondTeam() { return secondTeam; }
    public int getFirstTeamScore() { return firstTeamScore; }
    public int getSecondTeamScore() { return secondTeamScore; }
    public Instant getStartTime() { return startTime; }
}
