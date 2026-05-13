# Game Logic API

This document summarizes the game-logic API owned by role C.
The goal is to keep poker rules independent from UI and server transport.

## Main Classes

- `TexasHoldemGame`
  - Round state, betting, phases, showdown, side pots, and event log.
- `PokerHandEvaluator`
  - Evaluates the best poker hand from 7 cards.
- `PlayerActionType`
  - Type-safe action enum: `CHECK`, `CALL`, `RAISE`, `FOLD`.
- `GameSnapshot` / `PlayerSnapshot`
  - Read-only state for client display.
- `RoundResult` / `WinnerInfo`
  - Round result, winners, hand names, and payouts.
- `GameEvent`
  - Event log item for server push or UI history.

## Basic Flow

```java
List<Player> players = new ArrayList<>();
players.add(new Player("Alice"));
players.add(new Player("Bob"));

TexasHoldemGame game = new TexasHoldemGame(players);

game.startNewRound();
game.preFlop();

int start = game.getBettingStartIndex();
game.playerAction("Alice", PlayerActionType.CALL, 0);

game.resetBetsForNewStreet();
game.flop();

GameSnapshot aliceView = game.createSnapshotFor("Alice");
```

## Server-Side API

### `startNewRound()`

Starts a new round.

- Removes players with 0 chips.
- Creates and shuffles a new deck.
- Clears hands and community cards.
- Posts small blind and big blind.
- Resets the event log.

### `preFlop()`, `flop()`, `turn()`, `river()`

Moves to the next phase and deals cards.

### `resetBetsForNewStreet()`

Call this before `flop()`, `turn()`, and `river()`.

```java
game.resetBetsForNewStreet();
game.flop();
```

### `getBettingStartIndex()`

Returns the player index that should act first.

- Pre-flop: player after the big blind.
- Flop/Turn/River: player left of the dealer.

### `playerAction(String playerName, PlayerActionType action, int raiseAmount)`

Applies a player action.

Use `raiseAmount = 0` for `CHECK`, `CALL`, and `FOLD`.
For `RAISE`, `raiseAmount` means the extra raise amount above the call amount.

Invalid actions throw `IllegalArgumentException`.

### `playerAction(String playerName, String action, int raiseAmount)`

Compatibility overload for server messages such as `"call"` or `"raise"`.

### `getAvailableActions(Player player)`

Returns action commands as strings.

Example:

```java
[call, raise, fold]
```

### `getAvailableActionTypes(Player player)`

Returns action values as `PlayerActionType`.

### `getMinimumRaise()`

Returns the current minimum raise amount.

Rules:

- Normal raises must be at least `getMinimumRaise()`.
- A smaller raise is allowed only when it is all-in.
- A short all-in raise does not reopen betting.

### `getCallAmount(Player player)`

Returns the amount needed for the player to call.

### `getMaxRaiseAmount(Player player)`

Returns the maximum legal raise amount based on remaining chips.

### `wasLastActionReopensBetting()`

Returns `true` when the last raise was a full raise and should reopen action.
Short all-in raises return `false`.

### `isBettingRoundComplete()`

Returns true when all active non-all-in players have matched the current bet.

### `hasSingleActivePlayer()`

Returns true when only one player has not folded.

### `finishRound()`

Finishes the round, evaluates winners, and pays pots.
Side pots are supported.

```java
RoundResult result = game.finishRound();
System.out.println(result.getWinnerSummary());
```

## Client Snapshot API

### `createSnapshotFor(String playerName)`

Creates a player-specific view.

The viewer sees:

- phase
- pot
- current bet
- minimum raise
- community cards
- all players' public state
- own hand
- available actions

Other players' hands are hidden.

### `GameSnapshot`

Important getters:

- `getPhase()`
- `getPot()`
- `getCurrentBet()`
- `getMinimumRaise()`
- `getCommunityCards()`
- `getPlayers()`
- `getAvailableActions()`

### `PlayerSnapshot`

Important getters:

- `getName()`
- `getChips()`
- `getCurrentBet()`
- `getTotalContribution()`
- `isFolded()`
- `isAllIn()`
- `getRole()`
- `getHand()`

`getRole()` returns strings such as `[DEALER]`, `[SB]`, or `[BB]`.

## Event Log API

### `getEvents()`

Returns read-only `GameEvent` entries.

Current event types:

- `ROUND_STARTED`
- `BLIND`
- `PHASE_CHANGED`
- `PLAYER_ACTION`
- `POT_AWARDED`
- `ROUND_FINISHED`

This is useful for WebSocket broadcasting or frontend action history.

## Tests

Current self-contained test command:

```bash
javac Card.java Deck.java Player.java HandResult.java WinnerInfo.java RoundResult.java PlayerSnapshot.java GameSnapshot.java GameEvent.java PlayerActionType.java PokerHandEvaluator.java TexasHoldemGame.java PokerTest.java PokerGameTest.java
java PokerGameTest
```

Current test coverage:

- Deck has 52 unique cards.
- Royal flush evaluation.
- A2345 straight evaluation.
- Kicker comparison.
- Two pair beats one pair.
- Betting starts after the big blind pre-flop.
- Betting starts left of the dealer post-flop.
- Minimum raise is enforced.
- Short all-in raise is allowed.
- Short all-in raise does not reopen betting.
- Enum action API works.
- Game events are recorded.
- Side pot distribution.
- Fold win without showdown.
- Opponent hands are hidden in snapshots.
- Snapshot includes available actions.
- Invalid actions are rejected.

## Suggested Server Shape

Server room state:

```java
Map<String, TexasHoldemGame> rooms = new HashMap<>();
```

When a client sends an action:

```java
game.playerAction(playerName, actionType, raiseAmount);
GameSnapshot snapshot = game.createSnapshotFor(playerName);
List<GameEvent> events = game.getEvents();
```

Return:

- During a round: `GameSnapshot`
- After a round: `RoundResult` plus next `GameSnapshot`
