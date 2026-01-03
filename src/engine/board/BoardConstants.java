// CREATE THIS FILE at: src/engine/board/BoardConstants.java

package engine.board;

/**
 * Central repository for all board-related constants in the Jackaroo game.
 * Using a constants class eliminates magic numbers and provides a single
 * source of truth for game dimensions and rules.
 * 
 * Design principle: If a number appears more than once, or if its meaning
 * isn't immediately obvious, it belongs here.
 * 
 * @author Your Name
 * @version 2.0
 */
public final class BoardConstants {
    
    // ==================== BOARD STRUCTURE ====================
    
    /**
     * Total number of cells in the main circular track.
     * The board is divided into 4 equal sections of 25 cells each.
     */
    public static final int TRACK_SIZE = 100;
    
    /**
     * Number of cells per player section on the main track.
     * Each player "owns" a 25-cell section.
     */
    public static final int CELLS_PER_SECTION = 25;
    
    /**
     * Number of safe zone cells each player has.
     * Marbles must fill all 4 safe cells to win.
     */
    public static final int SAFE_ZONE_SIZE = 4;
    
    /**
     * Total number of players in the game.
     */
    public static final int NUM_PLAYERS = 4;
    
    /**
     * Number of marbles each player starts with in their home area.
     */
    public static final int MARBLES_PER_PLAYER = 4;
    
    /**
     * Number of trap cells randomly placed on the board.
     */
    public static final int NUM_TRAP_CELLS = 8;
    
    // ==================== POSITIONAL OFFSETS ====================
    
    /**
     * Offset from a player's base position to their entry point.
     * Entry cells are 2 positions BEFORE the next player's base.
     * 
     * Example: If Green's base is at 0, their entry is at (0-2+100)%100 = 98
     */
    public static final int BASE_TO_ENTRY_OFFSET = 2;
    
    /**
     * When moving past the entry point, we add 1 to enter the safe zone.
     * This represents the step from the entry cell INTO the first safe cell.
     */
    public static final int ENTRY_TO_SAFE_STEP = 1;
    
    // ==================== CARD GAME RULES ====================
    
    /**
     * Number of cards each player draws per round.
     */
    public static final int CARDS_PER_HAND = 4;
    
    /**
     * Minimum split distance for a Seven card.
     * Must be at least 1.
     */
    public static final int SEVEN_MIN_SPLIT = 1;
    
    /**
     * Maximum split distance for a Seven card.
     * Must be at most 6 (leaving at least 1 for the second marble).
     */
    public static final int SEVEN_MAX_SPLIT = 6;
    
    /**
     * Total value of a Seven card that must be distributed.
     */
    public static final int SEVEN_TOTAL_VALUE = 7;
    
    // ==================== GUI CONSTANTS ====================
    // These could go in a separate GUIConstants class, but are included
    // here since they're tightly coupled with board logic
    
    /**
     * Size of each cell in pixels for GUI rendering.
     */
    public static final int GUI_CELL_SIZE = 35;
    
    /**
     * Radius of marble circles in the GUI.
     */
    public static final int GUI_MARBLE_RADIUS = GUI_CELL_SIZE / 3;
    
    /**
     * Dimension of the board grid (10x10 for 100 cells).
     */
    public static final int GUI_BOARD_DIMENSION = 10;
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Calculates the base position for a given player index.
     * 
     * @param playerIndex Zero-based index (0-3)
     * @return The cell index of this player's base
     */
    public static int getBasePositionForPlayer(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= NUM_PLAYERS) {
            throw new IllegalArgumentException(
                "Player index must be 0-" + (NUM_PLAYERS - 1) + ", got: " + playerIndex
            );
        }
        return playerIndex * CELLS_PER_SECTION;
    }
    
    /**
     * Calculates the entry position for a given player index.
     * 
     * @param playerIndex Zero-based index (0-3)
     * @return The cell index of this player's entry point
     */
    public static int getEntryPositionForPlayer(int playerIndex) {
        int basePos = getBasePositionForPlayer(playerIndex);
        return (basePos - BASE_TO_ENTRY_OFFSET + TRACK_SIZE) % TRACK_SIZE;
    }
    
    /**
     * Normalizes a position to valid track index (0-99).
     * Handles negative numbers and wrapping.
     * 
     * @param position Any integer position
     * @return Normalized position in range [0, TRACK_SIZE)
     */
    public static int normalizePosition(int position) {
        return ((position % TRACK_SIZE) + TRACK_SIZE) % TRACK_SIZE;
    }
    
    /**
     * Calculates the distance between two positions on the circular track.
     * Always returns the shortest path distance (0 to TRACK_SIZE/2).
     * 
     * @param from Starting position
     * @param to Ending position
     * @return Distance in cells
     */
    public static int calculateDistance(int from, int to) {
        from = normalizePosition(from);
        to = normalizePosition(to);
        
        int forward = (to - from + TRACK_SIZE) % TRACK_SIZE;
        int backward = (from - to + TRACK_SIZE) % TRACK_SIZE;
        
        return Math.min(forward, backward);
    }
    
    // ==================== VALIDATION ====================
    
    /**
     * Validates that a split distance for a Seven card is legal.
     * 
     * @param split The proposed split distance
     * @return true if split is valid
     */
    public static boolean isValidSevenSplit(int split) {
        return split >= SEVEN_MIN_SPLIT && split <= SEVEN_MAX_SPLIT;
    }
    
    /**
     * Validates that a position is within the main track.
     * 
     * @param position The position to check
     * @return true if position is valid
     */
    public static boolean isValidTrackPosition(int position) {
        return position >= 0 && position < TRACK_SIZE;
    }
    
    // Private constructor to prevent instantiation
    private BoardConstants() {
        throw new AssertionError("BoardConstants is a utility class and should not be instantiated");
    }
}