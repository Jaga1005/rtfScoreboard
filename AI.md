# AI Usage Documentation

## Short summary

AI tools were used before implementation to refine the requirements, identify edge cases, define the intended public API, and prepare an implementation brief for GitHub Copilot.

ChatGPT was used as a requirements and design assistant. It did not implement or verify production code at the time this document was created. GitHub Copilot is intended to be used during implementation, based on the implementation brief included below. This document should be updated after implementation if Copilot generates, completes, refactors, or tests any code.

All domain decisions were reviewed and accepted by the author. In particular, the author chose the public operations, case-insensitive team identification, the absence of public match IDs, full-score updates, completed-match-only team statistics, and single-threaded operation.

## AI tools used so far

| Tool | How it was used | Result |
| --- | --- | --- |
| ChatGPT | Requirements clarification, API discussion, edge-case analysis, and preparation of a Copilot implementation prompt | The decisions and implementation artifact documented below |
| GitHub Copilot | Not yet used at the time this document was created | A prompt has been prepared for the implementation phase |

The exact ChatGPT model/version was not recorded in this document. No claim is made that AI-generated suggestions are correct merely because they were produced by an AI tool; the implementation and tests remain the author's responsibility.

## Context and decisions established with ChatGPT

The intended deliverable is a simple Java library for managing matches. It is not a console UI and should not print directly to standard output.

The following decisions were made during the conversation:

- Teams are identified in the public API by names represented as `String` values.
- Team-name comparison is case-insensitive and ignores leading and trailing whitespace.
- Match IDs are not exposed to callers.
- A team cannot play against itself.
- A team can participate in at most one in-progress match at a time.
- `A-B` and `B-A` identify the same in-progress match.
- Multiple completed matches between the same teams are allowed.
- Every new match begins at `0:0`.
- The library supplies and stores the immutable start time.
- A score update supplies the complete new score rather than a score delta.
- Score values are non-negative integers and may be raised or lowered.
- Completed matches cannot be updated.
- Completed matches are retained as history and kept separately from in-progress matches.
- In-progress matches are summarized by descending total score and then by newest start time.
- Team statistics include completed matches only.
- The library is designed for single-threaded use.
- Invalid operations are reported using domain-specific exceptions.
- Read operations return immutable snapshots rather than mutable domain entities.

## Prompt history

The prompts below record the requirements discussion that preceded implementation. Minor typographical errors in the original Polish prompts have been retained where practical. Assistant responses are not reproduced verbatim; their resulting decisions are captured in the context section and implementation artifact.

### Prompt 1: initial task description

> Mam zadanie, by zrobić prostą bibliotekę javową do zarządzania meczami.  
> Mecz to obiekt, który jest pomiędzy dwoma zespołami, ma określony wynik liczbowy [liczby naturalne, nieujemne], jest trwający lub ukończony, ma czas rozpoczęcia. Jeśli jest trwający to można zmieniać jego wynik, jeśli jest zakończony to już nie. może być kilka meczy pomiędzy tymi samymi zespołami, ale w danym czasie dana drużyna może brać udział tylko w jednym meczu. Dopóki dany mecz się nie zakończy, to ta drużyna nie może brać udziału w innym meczu. Równolegle może być wiele trwających meczy lub żadnego. Można odejmować lub zwiększać wynik meczu, ale można poruszać się tylko w scopie liczb nieujemnych. Data rozpoczęcia meczu jest stała.  
> Jak mnie rozumiesz? Czego Ci brakuje?

### Prompt 2: clarified requirements

> 1. Zróbmy stringi, ale case insensitive, nie używamy id w interfejsie użytkownika
> 2. Nie może grać sama ze sobą
> 3. updateScore i użyłabym albo nazw dwóch drużyn i nowy wynik albo drużyna i nowa wartość, co o tym sądzisz?
> 4. Tak, 0:0
> 5. Usera nie obchodzą Id, dla niego gra drużyna A i drużyna B. Rozdzieliłabym zakończone mecze od trwających.
> 6. Biblioteka
> 7. Tak, jako historię
> 8. chcę operacje:  
     > createNewMatch przyjmujący dwa zespoły,  
     > updateScore o którym już pisaliśmy  
     > finishMatch przyjmujący dwa zespoły  
     > getSummaryOfInProgressMatches - gdzie wyświetla wszystkie trwające mecze posortowane malejąco po total score [suma punktów obu drużyn w danym meczu], a potem który mecz jest nowszy  
     > getSummaryOfTheTeam - gdzie wyświetla dla danej drużyny w ilu meczach brała udział, ile meczy wygrała, ile przegrała, ile zremisowała, ile bramek strzeliła
> 9. Lecimy jednowątkowo
> 10. Tak  
      > Jak mnie teraz rozumiesz?

During the discussion following this prompt, the author accepted the recommendation that `updateScore` should identify both teams and supply both new score values. The author also accepted that team statistics should use completed matches only.

### Prompt 3: reversed pair and edge cases

> Zgadzam się, także createNewMatch("A","B") nie powinno pozwolić potem na createNewMatch("B", "A") dopóki ten pierwszy mecz się nie zakończy.  
> Czy widzisz jeszcze edge case'y do podanych metod lub sposobu działania?

This led to explicit handling of reversed team order, invalid names, score validation, attempts to operate on a non-matching pair, deterministic sorting, integer-overflow-safe score totals, immutable result objects, and zero statistics for a team with no completed matches.

### Prompt 4: preparation for implementation

> Świetnie. Podsumuj mi to tak, bym mogła teraz przejść z tym do implementacji kodu z GitHub Copilotem

ChatGPT produced the implementation brief reproduced in the next section. At the time this document was created, that brief was an input artifact prepared for Copilot rather than evidence that the implementation had already been completed.

### Prompt 5: AI documentation

> dobra, dzięki. to teraz Pomóż mi napisać dokumentację. Chcę zrobić AI.md - pomóż mi z treścią pliku. Chcę:  
> • Short summary of how AI tools were used [do tej pory]  
> • Include your prompt history and other contextual information  
> • Any artifact that guided the implementation

## Artifact that guides the implementation

The following specification was prepared with ChatGPT for use as the primary implementation prompt in GitHub Copilot.

### Goal

Implement a simple, single-threaded Java library for managing in-progress and completed matches. Follow the Java version, build system, package structure, and testing conventions already present in the repository. Avoid unrelated changes and unnecessary architectural layers.

### Proposed public API

The main service, for example `MatchManager`, should expose operations equivalent to:

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

Names may be adapted to existing project conventions, but the behavior described here should remain unchanged. The library must not print directly to the console.

### Team identity

- Validate that every name is non-null and non-blank.
- Trim leading and trailing whitespace.
- Normalize identity case-insensitively using `Locale.ROOT`.
- Preserve the spelling from the team's first valid use for presentation.
- Do not collapse internal whitespace.
- Reject a match in which both names normalize to the same team.

### Creating a match

`createNewMatch("A", "B")` creates an in-progress match with score `0:0` and a start time supplied by the library.

While `A-B` is in progress, all of the following must be rejected:

```java
createNewMatch("A", "B");
createNewMatch("B", "A");
createNewMatch("a", "b");
createNewMatch("A", "C");
createNewMatch("C", "B");
```

Any number of matches without shared teams may be in progress simultaneously. A failed creation must not leave partially updated internal state.

### Updating a score

`updateScore` replaces the complete current score. Both values must be non-negative `int` values. A score can increase, decrease, remain unchanged, or return to `0:0`.

Score values correspond to the order of team arguments in the update call. Therefore:

```java
createNewMatch("A", "B");
updateScore("B", "A", 3, 2);
```

produces the stored result `A 2:3 B`.

The method must locate the exact in-progress pair. If `A` plays `C` and `B` plays `D`, an update for `A-B` must fail without modifying either match. A negative value, missing in-progress pair, or completed match must also fail without changing state.

### Finishing a match

`finishMatch` identifies the pair case-insensitively and independently of argument order. It must:

- locate the exact in-progress pair,
- mark it as finished,
- preserve its score and start time,
- move it from in-progress storage to completed-match history,
- prevent subsequent score changes,
- release both teams for future matches.

Finishing a missing or already completed match must fail. After completing `A-B`, a later `B-A` is a distinct new match beginning at `0:0`; both completed matches must be retainable in history.

### In-progress match summary

`getSummaryOfInProgressMatches()` returns an immutable list of immutable snapshots containing at least:

- both display names,
- both score values,
- start time.

It contains in-progress matches only and is sorted by:

1. descending total score,
2. descending start time,
3. descending creation sequence if start times are equal.

Compute total score without `int` overflow:

```java
(long) firstTeamScore + secondTeamScore
```

Return an empty list rather than `null` when no match is in progress. Callers must not be able to mutate internal state through returned lists or objects.

### Team summary

`getSummaryOfTheTeam(String team)` returns an immutable value with:

```text
teamName
matchesPlayed
matchesWon
matchesLost
matchesDrawn
goalsScored
```

All counters should be `long`. Statistics include completed matches only, count every completed match separately, and correctly map scores whether the team was stored first or second. A tied score, including `0:0`, is a draw. The following invariant must hold:

```text
matchesPlayed = matchesWon + matchesLost + matchesDrawn
```

A valid unknown team, a team with no matches, or a team with only an in-progress match receives zero statistics rather than `null` or an exception.

### Time

Use `java.time.Clock` and `Instant` so behavior is testable. A default instance may use `Clock.systemUTC()`, while another constructor accepts an injected `Clock`. Start time is immutable. Maintain a creation sequence for deterministic ordering when multiple matches receive the same `Instant`.

### Encapsulation

Do not expose public setters for teams, scores, status, or start time. All state changes must pass through the main service. Returned summaries must be immutable snapshots and must not expose mutable internal collections or entities.

### Domain errors

Use clear domain-specific `RuntimeException` types or equivalent project conventions, for example:

- `InvalidTeamNameException`,
- `SameTeamMatchException`,
- `TeamAlreadyPlayingException`,
- `InvalidScoreException`,
- `MatchNotInProgressException`.

Perform all validation before mutating state.

### Concurrency

The library is intentionally single-threaded. Synchronization and concurrent collections are not required.

### Acceptance-test checklist

Tests should cover at least:

1. A new match starts at `0:0`.
2. Null, blank, and whitespace-only team names are rejected.
3. A team cannot play itself, including under different casing.
4. Team lookup is case-insensitive and trims surrounding whitespace.
5. `B-A` cannot be created while `A-B` is in progress.
6. A new match is rejected when either team is already playing.
7. Matches with no shared teams can coexist.
8. Score updates work with original and reversed team order.
9. A score may be lowered or left unchanged.
10. A negative score is rejected without changing the previous score.
11. Updating a pair that is not currently playing is rejected.
12. A match can be finished using reversed team order.
13. Finishing the same match twice is rejected.
14. A completed match cannot be updated.
15. Both teams are available after their match finishes.
16. Multiple completed matches between the same pair remain in history.
17. In-progress matches are sorted by total score.
18. Equal-total matches are sorted by start time.
19. Equal-time matches are sorted deterministically by creation sequence.
20. No in-progress matches produces an empty list.
21. Wins, losses, draws, matches played, and goals scored are calculated correctly.
22. In-progress matches do not affect team statistics.
23. A valid unknown team receives zero statistics.
24. Returned summaries cannot be used to mutate library state.

## Human review and responsibility

AI output was used as design assistance, not as an authority. The author remains responsible for:

- deciding whether the proposed design fits the assignment,
- reviewing every generated code change,
- checking API compatibility with the repository,
- running and interpreting the tests,
- correcting defects or unsupported assumptions,
- ensuring that the final submission is understood and can be explained without relying on AI output.

## Implementation log to complete later

Update this section after using GitHub Copilot:

- Copilot features used: _not yet recorded_
- Files or classes generated/completed by Copilot: _not yet recorded_
- Important implementation prompts: _not yet recorded_
- Suggestions rejected or substantially changed: _not yet recorded_
- Tests generated or proposed by Copilot: _not yet recorded_
- Manual verification performed: _not yet recorded_
- Final build and test result: _not yet recorded_
