# AI Usage Documentation

## Short summary

AI tools were used for requirements analysis, implementation, testing, review, and refactoring.

ChatGPT was used to clarify open design questions, identify edge cases, and prepare a detailed implementation specification. That specification was then supplied to an OpenAI Codex coding agent in a single request. The agent inspected the repository, implemented the Java library and its unit tests, and ran the Maven test suite. Follow-up prompts were used to review the implementation and improve naming, exception design, and encapsulation.

The assignment defined four core operations, summary ordering, and the requirement to add one additional operation. The remaining domain rules described below were design assumptions chosen by the author, not requirements imposed by the assignment. The author reviewed the generated solution and requested changes where the initial implementation did not match the preferred design.

## AI tools used

| Tool | How it was used | Result |
| --- | --- | --- |
| ChatGPT | Requirements clarification, API discussion, edge-case analysis, and preparation of the implementation specification | Documented assumptions, design decisions, and the implementation artifact included below |
| OpenAI Codex coding agent | Repository inspection, implementation, unit-test creation, local build verification, review, and refactoring | Production code, domain exceptions, JUnit tests, Maven test configuration, and subsequent clean-code changes |

## Assignment requirements and author-defined assumptions

### Requirements supplied in the assignment

The assignment required the library to:

1. start a new match;
2. update a score;
3. finish a match;
4. return matches in progress ordered by descending total score and then by most recent start time;
5. provide exactly one additional operation selected by the author and introduced in a distinct git commit.

The chosen additional operation is `getSummaryOfTheTeam`, which calculates completed-match statistics for a selected team.

## Prompt history

The prompts below are quoted verbatim. Spelling, grammar, and wording have not been corrected. Explanatory text outside the block quotes is contextual information and is not part of the original prompts.

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

ChatGPT produced the implementation brief reproduced in the next section. The brief was initially prepared for Copilot and was later supplied to the Codex coding agent as the primary implementation artifact. It guided both production behavior and unit-test coverage.

### Prompt 5: AI documentation

> dobra, dzięki. to teraz Pomóż mi napisać dokumentację. Chcę zrobić AI.md - pomóż mi z treścią pliku. Chcę:  
> • Short summary of how AI tools were used [do tej pory]  
> • Include your prompt history and other contextual information  
> • Any artifact that guided the implementation

### Prompt 6: implementation request

The author submitted the full implementation specification reproduced in the artifact section below and requested:

> Zaimplementuj prostą jednowątkową bibliotekę w Javie do zarządzania meczami. Najpierw sprawdź istniejącą strukturę projektu, wersję Javy, system budowania i konwencje testowe. Dostosuj implementację do projektu i nie zmieniaj niepowiązanych plików.

The Codex agent inspected the Maven project, implemented the library in `com.rtf.scoreboard`, added JUnit 5 configuration and tests, and ran the build.

### Prompt 7: clean-code refactoring

> dobra, pora na refaktor nieużywanych zmiennych, weź pogrupuj wyjątki, zmień nazwy zmiennych itd. wedle clean code'u

This prompted a behavior-preserving refactor. Domain exceptions were grouped under `com.rtf.scoreboard.exception`; abbreviated variables were replaced with domain-specific names; collection names were clarified; and score/status mutations were encapsulated inside the private `Match` model.

### Prompt 8: exception-constructor design

> Co do wyjątków - mam wrażenie, że konstruktory można by uprościć - bo tekst jest stały, to czy jest sens, by przekazywać pełen string, a ine tylko zmienną część?

Exception messages were centralized in the exception classes. Exceptions with fixed messages received no-argument constructors. Initially, `TeamAlreadyPlayingException` accepted only the team name as its variable component.

### Prompt 9: update AI documentation

> dobra, popatrz na plik AI.md - zaktualizuj i uzupełnij o to, co robiliśmy. Plik AI.md powinien zawierać: Short summary of how AI tools were used; Include your prompt history and other contextual information; Any artifact that guided the implementation

This prompt resulted in an update preserving the original design record while adding the implementation, verification, and refactoring history.

### Prompt 10: implementation review

> Explain why this implementation satisfies the requirement and identify any case in which it may violate the documented assumptions.

The Codex agent compared the current implementation against the documented requirements. It confirmed the main behavior and identified assumption boundaries concerning the meaning of a team's “first valid use,” exceptional failures between in-memory collection mutations, custom `Clock` implementations, and theoretical `long` overflow.

### Prompt 11: documentation update

> dobra, zaktualizuj AI.md o ten wpis nowy

## Implementation artifact

The following specification was prepared with ChatGPT for use as the primary implementation prompt in GitHub Copilot. It was later supplied without scope changes to the Codex coding agent.

<details>
<summary>Full implementation specification</summary>

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

</details>

## Implementation and verification

### Result produced by the coding agent

The agent implemented:

- `MatchManager`, containing the public operations, validation, active-match lookup, history, sorting, and team statistics;
- immutable `MatchSummary` and `TeamSummary` records;
- domain exceptions under `com.rtf.scoreboard.exception`;
- `MatchManagerTest`, using JUnit 5;
- the JUnit Jupiter dependency and Maven Surefire configuration in `pom.xml`.

### Important implementation choices

- Team identity uses `String.strip()` and `toLowerCase(Locale.ROOT)`.
- Active matches are indexed by normalized team name, enforcing the one-active-match-per-team assumption and allowing exact-pair validation.
- Finished matches are stored separately and are the sole input to team statistics.
- Reversed score updates are mapped to the stored team order.
- Match summaries are immutable snapshots sorted by total score, start time, and creation sequence.
- `Clock` is constructor-injected for deterministic time tests.
- Exception messages are defined inside the corresponding exception classes.

### Author-requested changes after generation

- Domain exceptions were moved from the root scoreboard package to a dedicated exception package.
- Abbreviated variables and unclear collection names were replaced with domain-specific names.
- Score and status mutations were encapsulated inside the private match model.
- Exception constructors were simplified so callers provide only message-specific context, not complete fixed messages.

### Verification performed

The generated JUnit suite contains 14 test methods covering the 24 author-defined scenarios from the implementation artifact. The final Maven execution reported:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The project is configured to build with Java 26. The recorded Maven build and test execution used JDK 26, matching the intended project version.