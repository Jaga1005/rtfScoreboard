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
}
