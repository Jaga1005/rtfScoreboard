package com.rtf.scoreboard;

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
    private final Map<String, Match> activeByTeam = new HashMap<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private final List<Match> history = new ArrayList<>();
    private long nextCreationOrder;

    public MatchManager() {
        this(Clock.systemUTC());
    }

    public MatchManager(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    private static TeamName parseTeam(String name) {
        if (name == null) throw new InvalidTeamNameException("Team name must not be null");
        String trimmed = name.strip();
        if (trimmed.isEmpty()) throw new InvalidTeamNameException("Team name must not be blank");
        return new TeamName(trimmed.toLowerCase(Locale.ROOT), trimmed);
    }

    public void createNewMatch(String firstTeam, String secondTeam) {
        TeamName first = parseTeam(firstTeam);
        TeamName second = parseTeam(secondTeam);
        if (first.key.equals(second.key)) {
            throw new SameTeamMatchException("A team cannot play against itself");
        }
        if (activeByTeam.containsKey(first.key)) {
            throw new TeamAlreadyPlayingException(first.trimmed + " is already playing");
        }
        if (activeByTeam.containsKey(second.key)) {
            throw new TeamAlreadyPlayingException(second.trimmed + " is already playing");
        }

        String firstDisplay = displayNames.getOrDefault(first.key, first.trimmed);
        String secondDisplay = displayNames.getOrDefault(second.key, second.trimmed);
        Match match = new Match(first.key, firstDisplay, second.key, secondDisplay,
                clock.instant(), nextCreationOrder++);
        displayNames.putIfAbsent(first.key, first.trimmed);
        displayNames.putIfAbsent(second.key, second.trimmed);
        activeByTeam.put(first.key, match);
        activeByTeam.put(second.key, match);
    }

    public void updateScore(String firstTeam, String secondTeam,
                            int firstTeamScore, int secondTeamScore) {
        TeamName first = parseTeam(firstTeam);
        TeamName second = parseTeam(secondTeam);
        if (firstTeamScore < 0 || secondTeamScore < 0) {
            throw new InvalidScoreException("Scores must be non-negative");
        }
        Match match = findActivePair(first.key, second.key);
        if (match.firstKey.equals(first.key)) {
            match.firstScore = firstTeamScore;
            match.secondScore = secondTeamScore;
        } else {
            match.firstScore = secondTeamScore;
            match.secondScore = firstTeamScore;
        }
    }

    public void finishMatch(String firstTeam, String secondTeam) {
        TeamName first = parseTeam(firstTeam);
        TeamName second = parseTeam(secondTeam);
        Match match = findActivePair(first.key, second.key);
        match.status = Status.FINISHED;
        activeByTeam.remove(match.firstKey);
        activeByTeam.remove(match.secondKey);
        history.add(match);
    }

    public List<MatchSummary> getSummaryOfInProgressMatches() {
        return activeByTeam.values().stream()
                .distinct()
                .sorted(Comparator
                        .comparingLong(Match::totalScore).reversed()
                        .thenComparing((Match match) -> match.startTime, Comparator.reverseOrder())
                        .thenComparing(Comparator.comparingLong(
                                (Match match) -> match.creationOrder).reversed()))
                .map(Match::summary)
                .toList();
    }

    private Match findActivePair(String firstKey, String secondKey) {
        Match match = activeByTeam.get(firstKey);
        if (match == null || !match.containsPair(firstKey, secondKey)) {
            throw new MatchNotInProgressException("The specified match is not in progress");
        }
        return match;
    }

    private enum Status {IN_PROGRESS, FINISHED}

    private record TeamName(String key, String trimmed) {
    }

    private static final class Match {
        private final String firstKey;
        private final String firstDisplay;
        private final String secondKey;
        private final String secondDisplay;
        private final Instant startTime;
        private final long creationOrder;
        private int firstScore;
        private int secondScore;
        private Status status = Status.IN_PROGRESS;

        private Match(String firstKey, String firstDisplay, String secondKey,
                      String secondDisplay, Instant startTime, long creationOrder) {
            this.firstKey = firstKey;
            this.firstDisplay = firstDisplay;
            this.secondKey = secondKey;
            this.secondDisplay = secondDisplay;
            this.startTime = startTime;
            this.creationOrder = creationOrder;
        }

        private boolean containsPair(String a, String b) {
            return firstKey.equals(a) && secondKey.equals(b)
                    || firstKey.equals(b) && secondKey.equals(a);
        }

        private long totalScore() {
            return (long) firstScore + secondScore;
        }

        private MatchSummary summary() {
            return new MatchSummary(firstDisplay, secondDisplay, firstScore, secondScore, startTime);
        }
    }
}
