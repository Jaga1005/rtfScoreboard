# Scoreboard Library

## Overview

This project implements an in-memory Java scoreboard library that supports multiple matches in progress at the same time.

The assignment deliberately leaves several domain and API questions open. This README distinguishes the required behavior from the assumptions and design choices made for this implementation.

## Assignment requirements

The required operations are:

1. Start a new match.
2. Update the score of a match.
3. Finish a match.
4. Get a summary of matches in progress, ordered by:
    - total score in descending order;
    - most recently started match first when total scores are equal.
5. Add exactly one additional operation and explain why it was chosen.

## Public operations

The library exposes operations equivalent to:

```java
void createNewMatch(String firstTeam, String secondTeam);

void updateScore(
        String firstTeam,
        String secondTeam,
        int firstTeamScore,
        int secondTeamScore
);

void finishMatch(String firstTeam, String secondTeam);

List<MatchSummary> getSummaryOfInProgressMatches();

TeamSummary getSummaryOfTheTeam(String team);
```

The first four methods implement the required functionality. `getSummaryOfTheTeam` is the one additional operation chosen for this solution.

The library returns data and does not print directly to the console. This keeps it independent of any particular user interface.

## Additional operation: team summary

The additional operation is:

```java
TeamSummary getSummaryOfTheTeam(String team);
```

It returns statistics calculated from completed matches:

- number of matches played;
- number of wins;
- number of losses;
- number of draws;
- total number of goals scored by the team.

### Why this feature was chosen

A team summary is a natural extension of a sports scoreboard. The core operations describe the current state of matches, while this operation turns completed match data into useful historical information.

It was also chosen because it exercises both sides of the model:

- active matches must remain separate from completed matches;
- scores must be interpreted correctly regardless of the order in which teams were supplied;
- repeated matches between the same teams must be counted independently.

Only completed matches are included. A match in progress cannot yet be classified reliably as a win, loss, or draw, and including only some of its data would make the returned statistics internally inconsistent.

The additional feature should be introduced in a distinct git commit, as required by the assignment.

## Assumptions

The following behavior is not fully specified by the assignment and is therefore treated as part of this solution's domain contract.

### Team identity

- A team is identified by its name represented as a `String`.
- Public match IDs are not used.
- Team-name comparison is case-insensitive.
- Leading and trailing whitespace is ignored.
- Empty, blank, and `null` team names are rejected.
- Internal whitespace is significant. For example, `"Real Madrid"` and `"Real  Madrid"` are different names.
- The spelling from a team's first valid use is preserved for display purposes.
- A team cannot play against itself, including under different casing.

For example, `"Poland"`, `"POLAND"`, and `" Poland "` identify the same team.

### Match identity and team availability

- A pair of teams identifies an in-progress match.
- Team order does not affect match identity: `A-B` and `B-A` identify the same match.
- A team can participate in at most one in-progress match at a time.
- Multiple matches may be in progress simultaneously as long as they do not share a team.
- Multiple completed matches between the same teams are allowed.
- After a match finishes, both teams become available immediately.

The one-active-match-per-team rule removes ambiguity from a name-based API. Without this rule, names alone would not be sufficient to identify a match in all cases and a match ID or competition identifier would be required.

### Match lifecycle

- Every new match starts at `0:0`.
- The library records the start time when the match is created.
- The start time cannot be changed.
- A match is either in progress or finished.
- A finished match cannot be updated or finished again.
- Finished matches are retained as history because the additional team-summary operation depends on them.
- The library does not record an end time because no current operation requires it.

### Score updates

- Each score is a non-negative `int`.
- `updateScore` accepts the complete new score, not a score increment or decrement.
- A score may increase, decrease, remain unchanged, or return to zero.
- Negative scores are rejected.
- Both score values are validated before either value is changed.
- Repeating the same update is valid and leaves the match unchanged.
- Score values correspond to the team order supplied to `updateScore`.

For example:

```java
createNewMatch("A", "B");
updateScore("B", "A", 3, 2);
```

results in the stored score:

```text
A 2:3 B
```

If the supplied teams do not form an exact in-progress pair, no match is updated. This remains true if both teams are currently playing, but against different opponents.

### Summary ordering

Matches in progress are ordered by:

1. total score in descending order;
2. start time in descending order;
3. creation sequence in descending order if two matches have the same start time.

The third rule is an implementation-level tie-breaker that makes results deterministic when a fixed or low-resolution clock gives multiple matches the same timestamp.

Total score is calculated using `long` arithmetic even though individual scores are `int` values:

```java
(long) firstTeamScore + secondTeamScore
```

This avoids integer overflow during comparison.

### Team statistics

- Statistics include completed matches only.
- Every completed match is counted independently.
- A tied score, including `0:0`, is a draw.
- Goals are mapped correctly whether the requested team was stored first or second.
- A valid team with no completed matches receives zero statistics.
- The same zero result is returned for a valid but previously unknown team.
- Statistical counters use `long` values.

The following invariant is expected to hold:

```text
matchesPlayed = matchesWon + matchesLost + matchesDrawn
```

### Runtime model

- The library is in-memory and does not provide persistence.
- The intended usage is single-threaded.
- Thread safety, synchronization, and concurrent collections are outside the current scope.
- The library uses a supplied `Clock` where appropriate so time-dependent behavior can be tested deterministically.
- Empty collections are returned instead of `null`.
- Read operations return immutable snapshots rather than mutable internal match objects.

## Design reasoning

### Why use team names instead of public match IDs?

The public API is intended to reflect how a caller describes a match: two teams are playing each other. Avoiding IDs keeps simple operations concise and readable.

This is safe within the chosen domain constraints because a team cannot participate in more than one in-progress match. The exact pair therefore identifies at most one active match.

### Why treat the pair as unordered?

The domain does not define home and away teams. Treating `A-B` and `B-A` as different match identities would allow the same teams to appear in duplicate active matches and would make update and finish operations unnecessarily sensitive to argument order.

The original creation order is still retained for display. During a score update, each score corresponds to the team next to it in that particular method call.

### Why replace the complete score?

Sports data can arrive as a current snapshot rather than as a perfect stream of scoring events. Replacing the complete score:

- makes repeated updates idempotent;
- allows incorrect data to be corrected;
- avoids dependence on receiving every increment exactly once;
- makes it straightforward to reconcile the scoreboard with an external source.

### Why keep active matches and history separate?

Core operations primarily work with active matches, while the additional feature works with completed matches. Separating them makes lifecycle rules explicit and prevents a completed match from being updated accidentally.

History is retained only because it is necessary for team statistics. No separate public history-query operation is added because the assignment permits exactly one additional operation.

### Why use immutable summaries?

Returning internal mutable match objects would let callers bypass validation and change scores, teams, status, or start time directly. Immutable snapshots preserve the library's invariants and keep mutation behind the intended operations.

### Why inject a clock?

The library remains responsible for assigning match start times, but using `Clock` instead of calling the system clock directly makes ordering tests repeatable. A default instance can still use the real UTC clock.

### Why use domain-specific exceptions?

Invalid operations have different causes, such as an invalid team name, a team already playing, a negative score, or a match that is not in progress. Explicit exceptions make failures visible and allow callers and tests to distinguish these cases without inspecting error-message text.

## Trade-offs

| Decision | Benefit | Cost or limitation |
| --- | --- | --- |
| Names instead of public match IDs | Small, readable API | Renaming and name collisions are harder to model; the API would not support two simultaneous matches involving the same team |
| Case-insensitive team identity | More forgiving for callers | Distinct teams whose names differ only by case cannot be represented |
| One in-progress match per team | Removes match-selection ambiguity and reflects the chosen domain model | Prevents modelling parallel squads, competitions, or data errors without adding another identifier |
| Unordered team pair | `A-B` and `B-A` cannot become duplicate active matches | The model has no home/away semantics |
| Complete-score replacement | Idempotent updates and easy corrections | No event-level history of how the score changed |
| Non-negative `int` scores | Simple API and ample range for normal sports results | Does not support fractional scores and has a fixed upper bound |
| Separate active matches and completed history | Clear lifecycle and supports statistics | Requires maintaining multiple collections consistently |
| Retaining all completed matches | Enables accurate team statistics | Memory usage grows for the lifetime of the service |
| Completed-only team statistics | Produces stable and internally consistent outcomes | Current in-progress performance is not reflected |
| Zero summary for an unknown team | Simple, null-free query behavior | A caller typo is not distinguished from a real team with no completed matches |
| Immutable snapshots | Protects invariants and prevents accidental mutation | Creates additional objects when summaries are requested |
| Injected `Clock` | Deterministic tests | Adds a small amount of constructor/API complexity |
| Domain-specific exceptions | Precise and testable failure contract | Introduces more exception classes |
| In-memory, single-threaded implementation | Keeps the exercise focused and easy to reason about | No persistence, process sharing, or thread-safety guarantees |

## Error handling

Invalid operations fail explicitly and leave the scoreboard unchanged. Expected error categories include:

- invalid team name;
- a team attempting to play itself;
- a team already participating in an in-progress match;
- a negative score;
- an update or finish request for a pair that is not currently playing.

Validation is performed before state mutation so a failed operation cannot partially update the scoreboard.

## Out of scope

The following features are intentionally not included:

- persistence or database integration;
- thread-safe access;
- public match IDs;
- competitions, seasons, venues, or home/away semantics;
- score-event history;
- match end timestamps;
- editing completed matches;
- deleting matches or history;
- a separate operation for querying full match history;
- console, web, or graphical user interfaces.

These omissions keep the implementation focused on the required operations and the single chosen extension.
