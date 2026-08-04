package com.rtf.scoreboard;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchManagerTest {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Test void createsMatchAtZeroZeroAndReturnsImmutableSnapshotList() {
        MatchManager manager = managerAt(NOW);
        manager.createNewMatch(" Poland ", "Germany");
        List<MatchSummary> summary = manager.getSummaryOfInProgressMatches();
        assertEquals(new MatchSummary("Poland", "Germany", 0, 0, NOW), summary.getFirst());
        assertThrows(UnsupportedOperationException.class,
                () -> summary.add(new MatchSummary("X", "Y", 0, 0, NOW)));
        manager.updateScore("POLAND", "germany", 2, 1);
        assertEquals(0, summary.getFirst().firstTeamScore());
    }

    @Test void rejectsInvalidTeamNamesWithoutCreatingAnything() {
        MatchManager manager = new MatchManager();
        assertThrows(InvalidTeamNameException.class, () -> manager.createNewMatch(null, "B"));
        assertThrows(InvalidTeamNameException.class, () -> manager.createNewMatch("", "B"));
        assertThrows(InvalidTeamNameException.class, () -> manager.createNewMatch("  \t", "B"));
        assertThrows(InvalidTeamNameException.class, () -> manager.createNewMatch("\u2003", "B"));
        assertTrue(manager.getSummaryOfInProgressMatches().isEmpty());
    }

    @Test void rejectsSameTeamIgnoringCaseAndOuterSpaces() {
        assertThrows(SameTeamMatchException.class,
                () -> new MatchManager().createNewMatch(" Poland ", "POLAND"));
    }

    @Test void rejectsReverseDuplicateAndAnyAlreadyPlayingTeam() {
        MatchManager manager = new MatchManager();
        manager.createNewMatch("A", "B");
        assertThrows(TeamAlreadyPlayingException.class, () -> manager.createNewMatch("b", "a"));
        assertThrows(TeamAlreadyPlayingException.class, () -> manager.createNewMatch("A", "C"));
        assertThrows(TeamAlreadyPlayingException.class, () -> manager.createNewMatch("C", "B"));
        assertEquals(1, manager.getSummaryOfInProgressMatches().size());
    }

    @Test void permitsSeveralDisjointMatches() {
        MatchManager manager = new MatchManager();
        manager.createNewMatch("A", "B");
        manager.createNewMatch("C", "D");
        assertEquals(2, manager.getSummaryOfInProgressMatches().size());
    }

    @Test void updatesInEitherOrderAndAllowsDecreaseAndSameScore() {
        MatchManager manager = new MatchManager();
        manager.createNewMatch("A", "B");
        manager.updateScore("A", "B", 4, 2);
        manager.updateScore("b", "a", 1, 3);
        assertScore(manager, 3, 1);
        manager.updateScore("A", "B", 1, 0);
        manager.updateScore("A", "B", 1, 0);
        assertScore(manager, 1, 0);
    }

    @Test void invalidScoreDoesNotChangeExistingScore() {
        MatchManager manager = new MatchManager();
        manager.createNewMatch("A", "B");
        manager.updateScore("A", "B", 2, 3);
        assertThrows(InvalidScoreException.class, () -> manager.updateScore("A", "B", -1, 8));
        assertScore(manager, 2, 3);
    }

    @Test void refusesUpdateForTeamsWhoseActualOpponentsDiffer() {
        MatchManager manager = new MatchManager();
        manager.createNewMatch("A", "C");
        manager.createNewMatch("B", "D");
        assertThrows(MatchNotInProgressException.class, () -> manager.updateScore("A", "B", 9, 9));
        assertTrue(manager.getSummaryOfInProgressMatches().stream()
                .allMatch(match -> match.firstTeamScore() == 0 && match.secondTeamScore() == 0));
    }

    @Test void finishWorksInReverseAndPreventsRepeatedFinishOrUpdate() {
        MatchManager manager = new MatchManager();
        manager.createNewMatch("A", "B");
        manager.updateScore("A", "B", 2, 1);
        manager.finishMatch("b", "a");
        assertTrue(manager.getSummaryOfInProgressMatches().isEmpty());
        assertThrows(MatchNotInProgressException.class, () -> manager.finishMatch("A", "B"));
        assertThrows(MatchNotInProgressException.class, () -> manager.updateScore("A", "B", 0, 0));
    }

    @Test void finishingReleasesTeamsAndKeepsEachRematchInHistory() {
        MatchManager manager = new MatchManager();
        manager.createNewMatch("A", "B");
        manager.updateScore("A", "B", 1, 0);
        manager.finishMatch("A", "B");
        manager.createNewMatch("b", "a");
        assertScore(manager, 0, 0);
        manager.finishMatch("B", "A");
        TeamSummary a = manager.getSummaryOfTheTeam("a");
        assertEquals(2, a.matchesPlayed());
        assertEquals(1, a.matchesWon());
        assertEquals(1, a.matchesDrawn());
        assertEquals("A", a.teamName());
    }

    @Test void sortsByTotalScoreThenNewestStartTime() {
        MutableClock clock = new MutableClock(NOW);
        MatchManager manager = new MatchManager(clock);
        manager.createNewMatch("A", "B");
        manager.updateScore("A", "B", 2, 2);
        clock.instant = NOW.plusSeconds(1);
        manager.createNewMatch("C", "D");
        manager.updateScore("C", "D", 3, 1);
        manager.createNewMatch("E", "F");
        manager.updateScore("E", "F", 10, 0);
        assertEquals(List.of("E", "C", "A"), manager.getSummaryOfInProgressMatches().stream()
                .map(MatchSummary::firstTeam).toList());
    }

    @Test void identicalTimeUsesLaterCreationFirst() {
        MatchManager manager = managerAt(NOW);
        manager.createNewMatch("A", "B");
        manager.createNewMatch("C", "D");
        assertEquals(List.of("C", "A"), manager.getSummaryOfInProgressMatches().stream()
                .map(MatchSummary::firstTeam).toList());
    }

    @Test void teamStatisticsIncludeOnlyFinishedMatchesAndBothTeamPositions() {
        MatchManager manager = new MatchManager();
        play(manager, "A", "B", 3, 1);
        play(manager, "C", "A", 2, 0);
        play(manager, "A", "D", 2, 2);
        manager.createNewMatch("A", "E");
        manager.updateScore("A", "E", 100, 0);
        TeamSummary result = manager.getSummaryOfTheTeam(" a ");
        assertEquals(new TeamSummary("A", 3, 1, 1, 1, 5), result);
        assertEquals(result.matchesPlayed(), result.matchesWon() + result.matchesLost() + result.matchesDrawn());
    }

    @Test void unknownAndOnlyActiveTeamsHaveZeroStatistics() {
        MatchManager manager = new MatchManager();
        assertEquals(new TeamSummary("Unknown", 0, 0, 0, 0, 0),
                manager.getSummaryOfTheTeam(" Unknown "));
        manager.createNewMatch("First Seen", "B");
        assertEquals(new TeamSummary("First Seen", 0, 0, 0, 0, 0),
                manager.getSummaryOfTheTeam("FIRST SEEN"));
    }

    private static MatchManager managerAt(Instant instant) {
        return new MatchManager(Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static void assertScore(MatchManager manager, int first, int second) {
        MatchSummary match = manager.getSummaryOfInProgressMatches().getFirst();
        assertEquals(first, match.firstTeamScore());
        assertEquals(second, match.secondTeamScore());
    }

    private static void play(MatchManager manager, String a, String b, int x, int y) {
        manager.createNewMatch(a, b);
        manager.updateScore(a, b, x, y);
        manager.finishMatch(a, b);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
