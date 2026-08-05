package com.rtf.scoreboard;

import com.rtf.scoreboard.exception.InvalidScoreException;
import com.rtf.scoreboard.exception.InvalidTeamNameException;
import com.rtf.scoreboard.exception.MatchNotInProgressException;
import com.rtf.scoreboard.exception.SameTeamMatchException;
import com.rtf.scoreboard.exception.TeamAlreadyPlayingException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MatchManager {
    private final Clock clock;
    private final Map<String, Match> inProgressMatchesByTeamKey = new HashMap<>();
    private final Map<String, String> canonicalTeamNames = new HashMap<>();
    private final List<Match> finishedMatches = new ArrayList<>();
    private long nextCreationOrder;

    public MatchManager() {
        this(Clock.systemUTC());
    }

    public MatchManager(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public void createNewMatch(String firstTeam, String secondTeam) {
        TeamName firstTeamName = parseTeamName(firstTeam);
        TeamName secondTeamName = parseTeamName(secondTeam);
        if (firstTeamName.key.equals(secondTeamName.key)) {
            throw new SameTeamMatchException();
        }
        ensureTeamIsAvailable(firstTeamName);
        ensureTeamIsAvailable(secondTeamName);

        Match newMatch = new Match(
                firstTeamName.key,
                canonicalNameOf(firstTeamName),
                secondTeamName.key,
                canonicalNameOf(secondTeamName),
                clock.instant(),
                nextCreationOrder++);

        rememberCanonicalName(firstTeamName);
        rememberCanonicalName(secondTeamName);
        inProgressMatchesByTeamKey.put(firstTeamName.key, newMatch);
        inProgressMatchesByTeamKey.put(secondTeamName.key, newMatch);
    }

    public void updateScore(String firstTeam, String secondTeam,
                            int firstTeamScore, int secondTeamScore) {
        TeamName requestedFirstTeam = parseTeamName(firstTeam);
        TeamName requestedSecondTeam = parseTeamName(secondTeam);
        if (firstTeamScore < 0 || secondTeamScore < 0) {
            throw new InvalidScoreException();
        }
        Match match = findInProgressMatch(requestedFirstTeam.key, requestedSecondTeam.key);
        match.updateScore(requestedFirstTeam.key, firstTeamScore, secondTeamScore);
    }

    public void finishMatch(String firstTeam, String secondTeam) {
        TeamName requestedFirstTeam = parseTeamName(firstTeam);
        TeamName requestedSecondTeam = parseTeamName(secondTeam);
        Match match = findInProgressMatch(requestedFirstTeam.key, requestedSecondTeam.key);

        match.finish();
        inProgressMatchesByTeamKey.remove(match.firstTeamKey);
        inProgressMatchesByTeamKey.remove(match.secondTeamKey);
        finishedMatches.add(match);
    }

    public List<MatchSummary> getSummaryOfInProgressMatches() {
        return inProgressMatchesByTeamKey.values().stream()
                .distinct()
                .filter(Match::isInProgress)
                .sorted(createMatchComparator())
                .map(Match::summary)
                .toList();
    }

    public TeamSummary getSummaryOfTheTeam(String team) {
        TeamName requestedTeam = parseTeamName(team);
        String teamName = canonicalNameOf(requestedTeam);

        TeamStats stats = finishedMatches.stream()
                .filter(match -> match.includes(requestedTeam.key))
                .reduce(new TeamStats(),
                        (acc, match) -> acc.addMatch(match, requestedTeam.key),
                        TeamStats::combine);

        return new TeamSummary(teamName, stats.matchesPlayed, stats.matchesWon,
                stats.matchesLost, stats.matchesDrawn, stats.goalsScored);
    }

    private void ensureTeamIsAvailable(TeamName teamName) {
        if (inProgressMatchesByTeamKey.containsKey(teamName.key)) {
            throw new TeamAlreadyPlayingException(teamName.displayName);
        }
    }

    private String canonicalNameOf(TeamName teamName) {
        return canonicalTeamNames.getOrDefault(teamName.key, teamName.displayName);
    }

    private void rememberCanonicalName(TeamName teamName) {
        canonicalTeamNames.putIfAbsent(teamName.key, teamName.displayName);
    }

    private Match findInProgressMatch(String firstTeamKey, String secondTeamKey) {
        Match match = inProgressMatchesByTeamKey.get(firstTeamKey);
        if (match == null || !match.isPair(firstTeamKey, secondTeamKey)) {
            throw new MatchNotInProgressException();
        }
        return match;
    }

    private static TeamName parseTeamName(String teamName) {
        if (teamName == null) {
            throw new InvalidTeamNameException();
        }
        String displayName = teamName.strip();
        if (displayName.isEmpty()) {
            throw new InvalidTeamNameException();
        }
        return new TeamName(displayName.toLowerCase(Locale.ROOT), displayName);
    }

    private enum Status {IN_PROGRESS, FINISHED}

    private static Comparator<Match> createMatchComparator() {
        return Comparator
                .comparingLong(Match::totalScore).reversed()
                .thenComparing((Match match) -> match.startedAt, Comparator.reverseOrder())
                .thenComparingLong((Match match) -> -match.creationOrder);
    }

    private static final class TeamStats {
        long matchesPlayed;
        long matchesWon;
        long matchesLost;
        long matchesDrawn;
        long goalsScored;

        TeamStats() {
            this.matchesPlayed = 0;
            this.matchesWon = 0;
            this.matchesLost = 0;
            this.matchesDrawn = 0;
            this.goalsScored = 0;
        }

        private TeamStats(long matchesPlayed, long matchesWon, long matchesLost,
                          long matchesDrawn, long goalsScored) {
            this.matchesPlayed = matchesPlayed;
            this.matchesWon = matchesWon;
            this.matchesLost = matchesLost;
            this.matchesDrawn = matchesDrawn;
            this.goalsScored = goalsScored;
        }

        TeamStats addMatch(Match match, String teamKey) {
            matchesPlayed++;
            int teamScore = match.scoreOf(teamKey);
            int opponentScore = match.opponentScoreOf(teamKey);
            goalsScored += teamScore;

            if (teamScore > opponentScore) {
                matchesWon++;
            } else if (teamScore < opponentScore) {
                matchesLost++;
            } else {
                matchesDrawn++;
            }
            return this;
        }

        static TeamStats combine(TeamStats a, TeamStats b) {
            return new TeamStats(
                    a.matchesPlayed + b.matchesPlayed,
                    a.matchesWon + b.matchesWon,
                    a.matchesLost + b.matchesLost,
                    a.matchesDrawn + b.matchesDrawn,
                    a.goalsScored + b.goalsScored);
        }
    }

    private record TeamName(String key, String displayName) {
    }

    private static final class Match {
        private final String firstTeamKey;
        private final String firstTeamName;
        private final String secondTeamKey;
        private final String secondTeamName;
        private final Instant startedAt;
        private final long creationOrder;
        private int firstScore;
        private int secondScore;
        private Status status = Status.IN_PROGRESS;

        private Match(String firstTeamKey, String firstTeamName, String secondTeamKey,
                      String secondTeamName, Instant startedAt, long creationOrder) {
            this.firstTeamKey = firstTeamKey;
            this.firstTeamName = firstTeamName;
            this.secondTeamKey = secondTeamKey;
            this.secondTeamName = secondTeamName;
            this.startedAt = startedAt;
            this.creationOrder = creationOrder;
        }

        private boolean isPair(String oneTeamKey, String otherTeamKey) {
            return firstTeamKey.equals(oneTeamKey) && secondTeamKey.equals(otherTeamKey)
                    || firstTeamKey.equals(otherTeamKey) && secondTeamKey.equals(oneTeamKey);
        }

        private boolean includes(String teamKey) {
            return firstTeamKey.equals(teamKey) || secondTeamKey.equals(teamKey);
        }

        private void updateScore(String firstRequestedTeamKey,
                                 int firstRequestedTeamScore,
                                 int secondRequestedTeamScore) {
            if (firstTeamKey.equals(firstRequestedTeamKey)) {
                firstScore = firstRequestedTeamScore;
                secondScore = secondRequestedTeamScore;
            } else {
                firstScore = secondRequestedTeamScore;
                secondScore = firstRequestedTeamScore;
            }
        }

        private int scoreOf(String teamKey) {
            return firstTeamKey.equals(teamKey) ? firstScore : secondScore;
        }

        private int opponentScoreOf(String teamKey) {
            return firstTeamKey.equals(teamKey) ? secondScore : firstScore;
        }

        private void finish() {
            status = Status.FINISHED;
        }

        private boolean isInProgress() {
            return status == Status.IN_PROGRESS;
        }

        private long totalScore() {
            return (long) firstScore + secondScore;
        }

        private MatchSummary summary() {
            return new MatchSummary(firstTeamName, secondTeamName, firstScore, secondScore, startedAt);
        }
    }
}
