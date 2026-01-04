// CREATE THIS FILE: src/engine/TurnManager.java

package engine;

import model.Colour;
import model.player.Player;
import java.util.ArrayList;

/**
 * Manages turn progression and player skip logic.
 * 
 * RESPONSIBILITY: Knows whose turn it is and who should be skipped.
 * 
 * Design Decision: This class is STATEFUL (tracks currentPlayerIndex).
 * Why? Because turn state needs to persist between method calls.
 * 
 * @author Your Name
 * @version 2.0
 */
public class TurnManager {
    
    // ==================== STATE ====================
    
    /** 
     * Index of the player whose turn it currently is.
     * Range: [0, players.size() - 1]
     */
    private int currentPlayerIndex;
    
    /** 
     * If set, this player will be skipped on the next turn advance.
     * Used by Ten and Queen cards.
     */
    private Colour playerToSkip;
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Creates a new TurnManager starting at player 0.
     */
    public TurnManager() {
        this.currentPlayerIndex = 0;
        this.playerToSkip = null;
    }
    
    // ==================== PUBLIC METHODS ====================
    
    /**
     * Advances to the next player's turn.
     * 
     * Logic:
     * 1. Calculate next player index (with wraparound)
     * 2. If that player should be skipped, advance again
     * 3. Clear the skip flag
     * 
     * Example:
     *   Current: Player 2
     *   playerToSkip: Player 3 (Red)
     *   Result: Advances to Player 0 (skips 3)
     * 
     * @param players List of all players in the game
     * @throws IllegalArgumentException if players list is null or empty
     */
    public void advanceToNextPlayer(ArrayList<Player> players) {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Cannot advance turn with no players");
        }
        
        // Step 1: Calculate next index with wraparound
        int nextPlayerIndex = (currentPlayerIndex + 1) % players.size();
        
        // Step 2: Check if this player should be skipped
        if (playerToSkip != null && 
            players.get(nextPlayerIndex).getColour() == playerToSkip) {
            
            // Skip this player, advance one more time
            currentPlayerIndex = (nextPlayerIndex + 1) % players.size();
            playerToSkip = null; // Clear the flag
        } else {
            // Normal advancement
            currentPlayerIndex = nextPlayerIndex;
        }
    }
    
    /**
     * Marks a player to be skipped on the next turn advance.
     * Used by Ten and Queen cards.
     * 
     * Important: The skip happens on the NEXT call to advanceToNextPlayer().
     * It does NOT skip the player immediately.
     * 
     * @param colour The colour of the player to skip
     */
    public void skipPlayer(Colour colour) {
        this.playerToSkip = colour;
    }
    
    /**
     * Gets the current player index.
     * 
     * @return Index of current player (0-3)
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    /**
     * Gets the next player index (without advancing).
     * Useful for UI to show "Next: Player X"
     * 
     * @param numPlayers Total number of players
     * @return Index of next player
     */
    public int peekNextPlayerIndex(int numPlayers) {
        return (currentPlayerIndex + 1) % numPlayers;
    }
    
    /**
     * Checks if a specific player is marked to be skipped.
     * 
     * @param colour The player colour to check
     * @return true if this player will be skipped
     */
    public boolean isPlayerMarkedForSkip(Colour colour) {
        return playerToSkip != null && playerToSkip == colour;
    }
    
    /**
     * Clears the skip flag without advancing turns.
     * Use this if you need to cancel a skip (rare).
     */
    public void clearSkipFlag() {
        this.playerToSkip = null;
    }
}