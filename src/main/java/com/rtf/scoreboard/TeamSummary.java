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
}
