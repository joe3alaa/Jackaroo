# Failing Tests Analysis: BoardTest.java Tests 3 and 8

> **Status**: Root-caused, not yet fixed (per request).
> **Date**: 2026-03-13

Both failures are **test setup bugs**, not Board bugs.
The Board is enforcing the correct game rules in both cases.

---

## Test 3: `testWrapAroundMove` — ALWAYS FAILS

### Test Code (BoardTest.java lines 101–115)

```java
@Test
@DisplayName("Test 3: Move wraps around the board correctly")
void testWrapAroundMove() throws Exception {
    // Arrange: Place marble at position 95 (near end of track)
    board.sendToBase(testMarble, testResult);          // → position 0 (GREEN's base)
    board.moveBy(testMarble, 95, false, testResult);   // → position 95

    // Act: Move forward 10 spaces (should wrap to position 5)
    board.moveBy(testMarble, 10, false, testResult);

    // Assert: Marble should be at position 5
    Cell expectedCell = board.getTrack().get(5);
    assertEquals(testMarble, expectedCell.getMarble(),
        "Marble should wrap around and land at position 5");
}
```

### Error Message

```
exception.IllegalMovementException: Move is too far to land in the safe zone.
    at engine.board.Board.buildSafeZoneEntryPath(Board.java:268)
    at engine.board.Board.validateSteps(Board.java:149)
    at engine.board.Board.moveBy(Board.java:438)
    at engine.board.BoardTest$BasicMovementTests.testWrapAroundMove(BoardTest.java:109)
```

(Occasionally `"Cannot move marble that is not on board. Is it still in the home
area?"` — when a random trap cell at position 95 kills the marble during
the setup move, before the test's Act step even runs.)

### Root Cause

**The test places the active player's marble 3 cells before the player's
own safe-zone entry, then moves it 10 forward.  The Board correctly
diverts the marble into the safe zone instead of wrapping around the
track.  The test expectation contradicts the game rules.**

Step-by-step trace:

1. The `setUp()` method creates the board with colour order
   `[GREEN, RED, YELLOW, BLUE]` and stubs `getActivePlayerColour()` to
   return `GREEN`.

2. GREEN's base = `0 × 25 = 0`.
   GREEN's entry = `0 − 2 = −2`, which normalizes to **position 98**.

3. The test places `testMarble` (GREEN, active-player colour) at
   position 95 and asks to move +10.

4. Inside `validateSteps` → `shouldEnterSafeZone`:
   ```
   distanceToEntry = normalizeDistance(95, 98) = 3
   steps(10) > distanceToEntry(3) → true
   ```
   So the Board routes to `buildSafeZoneEntryPath`, not `buildTrackPath`.

5. Inside `buildSafeZoneEntryPath`:
   ```
   stepsInSafe = 10 − 3 − 1 = 6
   stepsInSafe(6) >= SAFE_ZONE_SIZE(4) → true → THROW
   ```

6. The test expects a simple wrap to position 5.  The Board says
   "you can't skip past your own safe zone."  **The Board is right.**

### Why the test is wrong

The test is trying to verify wrap-around movement, but it chose a
marble colour (GREEN = active player) and a starting position (95)
that guarantee the marble crosses its own entry point (98).  In
Jackaroo, a marble of the active player that crosses its entry point
**must** enter the safe zone — it cannot continue on the track.

To test pure wrapping without safe-zone interference, the test
should either:
- Use a marble whose colour ≠ the active player colour (only active
  player marbles are diverted), or
- Start from a position where +10 does NOT cross position 98
  (e.g., start at position 5, move 10 → position 15)

---

## Test 8: `testCannotLandOnOwnMarble` — ALWAYS FAILS

### Test Code (BoardTest.java lines 201–217)

```java
@Test
@DisplayName("Test 8: Cannot land on own marble")
void testCannotLandOnOwnMarble() throws Exception {
    // Arrange: Place two of our marbles on the board
    Marble marble1 = testPlayer.getOneMarble();
    Marble marble2 = testPlayer.getOneMarble();

    board.sendToBase(marble1, testResult);
    board.sendToBase(marble2, testResult);     // ← CRASHES HERE

    board.moveBy(marble1, 5, false, testResult);

    // Act & Assert: marble2 cannot land on marble1
    assertThrows(IllegalMovementException.class, () -> {
        board.moveBy(marble2, 5, false, testResult);
    }, "Should not allow landing on own marble");
}
```

### Error Message

```
exception.CannotFieldException: Your own marble is already on your Base Cell.
    at engine.board.Board.validateFielding(Board.java:422)
    at engine.board.Board.sendToBase(Board.java:479)
    at engine.board.BoardTest$CollisionTests.testCannotLandOnOwnMarble(BoardTest.java:209)
```

The exception is thrown on **line 209** (`board.sendToBase(marble2, ...)`),
during **test setup**, before the `assertThrows` block ever runs.
The test expects `IllegalMovementException` but gets
`CannotFieldException` — from the wrong line entirely.

### Root Cause

**Two compounding bugs in the test setup:**

#### Bug 1: `getOneMarble()` returns the same marble twice

```java
// Player.java
public Marble getOneMarble() {
    if (marbles.isEmpty()) return null;
    return this.marbles.get(0);     // always returns index 0, never removes
}
```

The test calls `getOneMarble()` twice:
```java
Marble marble1 = testPlayer.getOneMarble();   // → marbles[0]
Marble marble2 = testPlayer.getOneMarble();   // → marbles[0] AGAIN
```

`marble1` and `marble2` are **the same object**.  `getOneMarble()`
returns the first marble from the home zone without removing it.
Calling it twice gives back the same reference both times.

#### Bug 2: `sendToBase` refuses to field when own marble occupies base

Even if the test managed to get two different GREEN marbles, the
second `sendToBase` would still fail:

```java
board.sendToBase(marble1, testResult);   // Places marble1 on GREEN's base (pos 0)
board.sendToBase(marble2, testResult);   // Base already occupied by marble1...
```

Inside `sendToBase` (Board.java line 474–485):
```java
public void sendToBase(Marble marble, ActionResult result)
        throws CannotFieldException, IllegalDestroyException {
    int basePosition = getBasePosition(marble.getColour());
    Cell baseCell = this.track.get(basePosition);

    if (baseCell.getMarble() != null) {
        validateFielding(baseCell.getMarble());   // ← checks occupant
        destroyMarble(baseCell.getMarble(), result);
    }
    baseCell.setMarble(marble);
}
```

`validateFielding` (Board.java line 420–423):
```java
private void validateFielding(Marble marbleOnBase) throws CannotFieldException {
    if (marbleOnBase.getColour() == gameManager.getActivePlayerColour())
        throw new CannotFieldException("Your own marble is already on your Base Cell.");
}
```

The marble on base is GREEN (same colour as active player) →
`CannotFieldException`.  The test never reaches the movement
step it intended to test.

### Why the test is wrong

The test tries to use `sendToBase()` twice as a shortcut for "place
two of my marbles on the board."  But `sendToBase()` is a game action
(fielding from home zone), and the game correctly prevents fielding
when your own marble is already on the base cell.

To test landing-on-own-marble, the test should place marbles directly
on track cells instead of going through `sendToBase()`:
```java
Marble marble1 = new Marble(Colour.GREEN);
Marble marble2 = new Marble(Colour.GREEN);
board.getTrack().get(0).setMarble(marble2);   // Place marble2 at pos 0
board.getTrack().get(5).setMarble(marble1);    // Place marble1 at pos 5

// Now try to move marble2 to pos 5 where marble1 sits
assertThrows(IllegalMovementException.class, () -> {
    board.moveBy(marble2, 5, false, testResult);
});
```

---

## Additional observation: Flaky tests caused by random trap cells

Tests 1, 4, and 7 pass most of the time but fail intermittently.
The Board constructor places 8 trap cells at random positions
(`assignTrapCell()` in Board.java line 73).  When a trap lands on a
position used by a test, the marble is destroyed on arrival,
causing the test to fail with an unexpected null or a different
exception.

Over 5 runs: Test 7 failed 2/5, Test 1 failed 2/5, Test 4 passed all 5
(but has been observed failing in other runs).

This is a separate issue from Tests 3 and 8, which fail **every
single run** regardless of trap placement.
