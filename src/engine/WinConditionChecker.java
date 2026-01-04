// CREATE THIS FILE: src/engine/WinConditionChecker.java

package engine;

import engine.board.Board;
import engine.board.Cell;
import engine.board.SafeZone;
import model.Colour;

/**
 * Checks for win conditions in the game.
 * 
 * RESPONSIBILITY: Knows the rules for winning and how to detect it.
 * 
 * Design Decision: This is a STATELESS checker. It doesn't store anything, it
 * just analyzes the current board state. This makes it easy to test.
 * 
 * Win Condition: A player wins when all 4 cells in their safe zone are filled.
 * 
 * @author Your Name
 * @version 2.0
 */
public class WinConditionChecker {

	// ==================== STATE ====================

	/**
	 * Reference to the game board to check safe zones. This is IMMUTABLE (final) -
	 * we never change which board we're checking.
	 */
	private final Board board;

	// ==================== CONSTRUCTOR ====================

	/**
	 * Creates a new win checker for a specific board.
	 * 
	 * @param board The game board to check (must not be null)
	 * @throws IllegalArgumentException if board is null
	 */
	public WinConditionChecker(Board board) {
		if (board == null) {
			throw new IllegalArgumentException("Board cannot be null");
		}
		this.board = board;
	}

	// ==================== PUBLIC METHODS ====================

	/**
	 * Checks if any player has won the game.
	 * 
	 * Algorithm: 1. Loop through all safe zones 2. Check if each safe zone is full
	 * (4 marbles) 3. Return the colour of the first full safe zone found 4. Return
	 * null if no winner yet
	 * 
	 * Performance: O(4 * 4) = O(16) = O(1) constant time (Always checks exactly 4
	 * safe zones with 4 cells each)
	 * 
	 * @return The winning player's colour, or null if no winner
	 */
	public Colour checkForWinner() {
		// Iterate through all safe zones
		for (SafeZone safeZone : board.getSafeZones()) {
			// Check if this safe zone is completely filled
			if (safeZone.isFull()) {
				// Found a winner!
				return safeZone.getColour();
			}
		}

		// No winner yet
		return null;
	}

	/**
	 * Checks if a specific player has won.
	 * 
	 * This is more efficient than checkForWinner() when you only care about one
	 * player (e.g., checking if current player won after their turn).
	 * 
	 * @param colour The player colour to check
	 * @return true if this player has won
	 */
	public boolean hasPlayerWon(Colour colour) {
		if (colour == null) {
			return false;
		}

		// Find this player's safe zone
		for (SafeZone safeZone : board.getSafeZones()) {
			if (safeZone.getColour() == colour) {
				return safeZone.isFull();
			}
		}

		return false; // Safe zone not found (shouldn't happen)
	}

	/**
	 * Gets the number of marbles a player has in their safe zone. Useful for UI to
	 * show progress: "3/4 marbles in safe zone"
	 * 
	 * @param colour The player colour to check
	 * @return Number of marbles in safe zone (0-4)
	 */
	public int getSafeZoneProgress(Colour colour) {
		if (colour == null) {
			return 0;
		}

		// Find the player's safe zone
		for (SafeZone safeZone : board.getSafeZones()) {
			if (safeZone.getColour() == colour) {
				// Count filled cells
				int count = 0;
				for (Cell cell : safeZone.getCells()) {
					if (cell.getMarble() != null) {
						count++;
					}
				}
				return count;
			}
		}

		return 0; // Not found
	}

	/**
	 * Checks if the game is over (any player has won). More readable than checking
	 * if checkForWinner() != null.
	 * 
	 * @return true if game is over
	 */
	public boolean isGameOver() {
		return checkForWinner() != null;
	}
}
