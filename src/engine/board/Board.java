// PASTE THIS ENTIRE CODE INTO your Board.java file.

package engine.board;

import java.util.ArrayList;

import engine.GameManager;
import engine.action.ActionResult;
import engine.action.AnimationStep;
import exception.CannotFieldException;
import exception.IllegalDestroyException;
import exception.IllegalMovementException;
import exception.IllegalSwapException;
import exception.InvalidMarbleException;
import model.Colour;
import model.player.Marble;
import static engine.board.BoardConstants.*;

public class Board implements BoardManager {
    private final ArrayList<Cell> track;
    private final ArrayList<SafeZone> safeZones;
	private final GameManager gameManager;
    private int splitDistance;
    private final ArrayList<Colour> boardColourOrder;

    public Board(ArrayList<Colour> colourOrder, GameManager gameManager) {
        this.track = new ArrayList<>();
        this.safeZones = new ArrayList<>();
        this.gameManager = gameManager;
        this.boardColourOrder = new ArrayList<>(colourOrder);
        
        // <<< CHANGE: Board initialization now sets cell colours >>>
        for (int i = 0; i < TRACK_SIZE; i++) {
            Colour ownerColour = null;
            CellType type = CellType.NORMAL;

            // Determine if the cell is a BASE or ENTRY cell and who owns it
            int playerIndex = i / CELLS_PER_SECTION;
            if (i % CELLS_PER_SECTION == 0 && playerIndex < colourOrder.size()) {
                type = CellType.BASE;
                ownerColour = colourOrder.get(playerIndex);
            } else if ((i + BASE_TO_ENTRY_OFFSET) % CELLS_PER_SECTION == 0 && playerIndex < colourOrder.size()) {
                type = CellType.ENTRY;
                ownerColour = colourOrder.get(playerIndex);
            }
            
            this.track.add(new Cell(type, ownerColour, i));
        }

        for(int i = 0; i < NUM_TRAP_CELLS; i++) {
            assignTrapCell();
        }

        for (Colour colour : this.boardColourOrder) {
            this.safeZones.add(new SafeZone(colour));
        }

        splitDistance = 3;
    }

    public ArrayList<Cell> getTrack() {
        return this.track;
    }

    public ArrayList<SafeZone> getSafeZones() {
        return this.safeZones;
    }

    public void setSplitDistance(int splitDistance) {
        this.splitDistance = splitDistance;
    }
   
    private void assignTrapCell() {
        int randIndex;
        do {
            randIndex = (int)(Math.random() * TRACK_SIZE); 
        } while(this.track.get(randIndex).getCellType() != CellType.NORMAL || this.track.get(randIndex).isTrap());
        
        this.track.get(randIndex).setTrap(true);
    }
    
    private ArrayList<Cell> getSafeZoneCells(Colour colour) {
        for (SafeZone sz : this.safeZones) {
            if (sz.getColour() == colour) {
                return sz.getCells();
            }
        }
        return null;
    }

    private int getPositionInPath(ArrayList<Cell> path, Marble marble) {
        if (path == null) return -1;
        for(int i = 0; i < path.size(); i++) {
        	if(path.get(i).getMarble() == marble) {
                // If the path is the main track, the cell's index is the position.
                if (path == this.track) {
                    return path.get(i).getIndex();
                }
                // If it's a safe zone, the position is its index within that smaller list.
                return i;
            }
        }
        return -1;
    }


    private int getBasePosition(Colour colour) {
        int index = this.boardColourOrder.indexOf(colour);
        return (index != -1) ? index * CELLS_PER_SECTION : -1;
    }

    private int getEntryPosition(Colour colour) {
        int basePos = getBasePosition(colour);
        return (basePos != -1) ? (basePos - BASE_TO_ENTRY_OFFSET) : -1;
    }
    
    /**
     * REFACTORED VERSION - From 60+ lines to ~15 lines in main method
     * 
     * KEY PRINCIPLES APPLIED:
     * 1. Single Responsibility - Each method does ONE thing
     * 2. Extract Method - Complex logic to descriptive method names
     * 3. Early Return - Handle edge cases first
     * 4. Fail Fast - Validate immediately
     */

    // ==================== MAIN METHOD (15 lines) ====================

    /**
     * Validates a marble's movement and returns the complete path it will travel.
     * This is the orchestrator method - it delegates to specialists.
     * 
     * @param marble The marble to move
     * @param steps Number of steps (positive = forward, negative = backward)
     * @return Complete path from current position to final position
     * @throws IllegalMovementException if move is illegal
     */
    private ArrayList<Cell> validateSteps(Marble marble, int steps) throws IllegalMovementException {
        // Step 1: Determine where the marble currently is
        PathContext context = determineMarbleContext(marble);
        
        // Step 2: Delegate to appropriate specialist based on location
        if (context.isInSafeZone()) {
            return buildSafeZonePath(context, steps);
        }
        
        // Step 3: Check if this move enters the safe zone
        if (shouldEnterSafeZone(context, steps)) {
            return buildSafeZoneEntryPath(context, steps);
        }
        
        // Step 4: Standard track movement (forward or backward)
        return buildTrackPath(context, steps);
    }

    /**
     * Value object that captures everything we need to know about a marble's
     * current position. Using a dedicated class makes our intent crystal clear.
     */
    private static class PathContext {
        final Marble marble;
        final int trackPosition;      // -1 if not on track
        final int safeZonePosition;   // -1 if not in safe zone
        final ArrayList<Cell> safeZoneCells;
        final int entryPosition;
        
        PathContext(Marble marble, int trackPos, int safePos, 
                    ArrayList<Cell> safeCells, int entry) {
            this.marble = marble;
            this.trackPosition = trackPos;
            this.safeZonePosition = safePos;
            this.safeZoneCells = safeCells;
            this.entryPosition = entry;
        }
        
        boolean isInSafeZone() {
            return safeZonePosition != -1;
        }
        
        boolean isOnTrack() {
            return trackPosition != -1;
        }
    }

    // ==================== STEP 1: DETERMINE CONTEXT ====================

    /**
     * Finds where the marble is and packages up all relevant info.
     * This replaces the scattered position-finding logic.
     */
    private PathContext determineMarbleContext(Marble marble) throws IllegalMovementException {
        int trackPos = getPositionInPath(track, marble);
        ArrayList<Cell> safeZone = getSafeZoneCells(marble.getColour());
        int safePos = getPositionInPath(safeZone, marble);
        int entryPos = getEntryPosition(marble.getColour());
        
        if (trackPos == -1 && safePos == -1) {
            throw new IllegalMovementException(
                "Cannot move marble that is not on board. Is it still in the home area?"
            );
        }
        
        return new PathContext(marble, trackPos, safePos, safeZone, entryPos);
    }

    // ==================== STEP 2: SAFE ZONE MOVEMENT ====================

    /**
     * Handles movement WITHIN the safe zone (marble already inside).
     * Rules: Can only move forward, cannot exceed safe zone size.
     */
    private ArrayList<Cell> buildSafeZonePath(PathContext ctx, int steps) 
            throws IllegalMovementException {
        
        if (steps < 0) {
            throw new IllegalMovementException(
                "Cannot move backward inside safe zone. Forward moves only!"
            );
        }
        
        int targetPosition = ctx.safeZonePosition + steps;
        
        if (targetPosition >= SAFE_ZONE_SIZE) { // <<< WAS: hardcoded size
            throw new IllegalMovementException(
                "Move is too high to complete within the safe zone."
            );
        }
        
        // Build the path cell by cell
        ArrayList<Cell> path = new ArrayList<>();
        for (int i = ctx.safeZonePosition; i <= targetPosition; i++) {
            path.add(ctx.safeZoneCells.get(i));
        }
        return path;
    }

    // ==================== STEP 3: ENTERING SAFE ZONE ====================

    /**
     * Determines if this move should transition from track to safe zone.
     * Key insight: You must PASS the entry point, not land ON it.
     */
    private boolean shouldEnterSafeZone(PathContext ctx, int steps) {
        if (!ctx.isOnTrack() || steps <= 0) {
            return false; // Only forward moves from track can enter
        }
        
        boolean isActivePlayer = (ctx.marble.getColour() == gameManager.getActivePlayerColour());
        if (!isActivePlayer) {
            return false; // Only active player's marbles can enter their safe zone
        }
        
        int distanceToEntry = normalizeDistance(ctx.trackPosition, ctx.entryPosition);
        return steps > distanceToEntry; // Must go PAST entry
    }

    /**
     * Builds a path that goes from track to entry to safe zone.
     * This is a composite path spanning two different areas.
     */
    private ArrayList<Cell> buildSafeZoneEntryPath(PathContext ctx, int steps) 
            throws IllegalMovementException {
        
        int distanceToEntry = normalizeDistance(ctx.trackPosition, ctx.entryPosition);
        int stepsInSafe = steps - distanceToEntry - ENTRY_TO_SAFE_STEP;
        
        if (stepsInSafe >= SAFE_ZONE_SIZE) { // <<< WAS: hardcoded size
            throw new IllegalMovementException(
                "Move is too far to land in the safe zone."
            );
        }
        
        ArrayList<Cell> path = new ArrayList<>();
        
        // Part 1: Track cells up to and including entry
        for (int i = 0; i <= distanceToEntry; i++) {
            int pos = normalizePosition(ctx.trackPosition + i);
            path.add(track.get(pos));
        }
        
        // Part 2: Safe zone cells
        for (int i = 0; i <= stepsInSafe; i++) {
            path.add(ctx.safeZoneCells.get(i));
        }
        
        return path;
    }

    // ==================== STEP 4: STANDARD TRACK MOVEMENT ====================

    /**
     * Handles simple forward or backward movement on the main track.
     * This is the fallback case - no special zones involved.
     */
    private ArrayList<Cell> buildTrackPath(PathContext ctx, int steps) {
        ArrayList<Cell> path = new ArrayList<>();
        int current = ctx.trackPosition;
        int stepCount = Math.abs(steps);
        int direction = (steps > 0) ? 1 : -1;
        
        // Include starting position, then step-by-step to destination
        for (int i = 0; i <= stepCount; i++) {
            path.add(track.get(current));
            current = normalizePosition(current + direction);
        }
        
        return path;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Calculates forward distance on circular track.
     * Example: From position 98 to position 2 = distance 4 (not 96!)
     */
    private int normalizeDistance(int from, int to) {
        return (to - from + TRACK_SIZE) % TRACK_SIZE;
    }

    /**
     * Wraps position to valid track range [0, TRACK_SIZE).
     * Handles negative numbers correctly.
     */
    private int normalizePosition(int position) {
        return ((position % TRACK_SIZE) + TRACK_SIZE) % TRACK_SIZE;
    }

    // <<< REWRITTEN FOR CLARITY AND CORRECTNESS >>>
    private void validatePath(Marble movingMarble, ArrayList<Cell> fullPath, boolean isKingMove) throws IllegalMovementException {
        if (fullPath.isEmpty()) return;

        Colour ownerColour = movingMarble.getColour();
        Cell targetCell = fullPath.get(fullPath.size() - 1);

        // Rule: You can never land on your own marble.
        if (targetCell.getMarble() != null && targetCell.getMarble().getColour() == ownerColour) {
            throw new IllegalMovementException("Cannot land on a cell already occupied by your own marble.");
        }

        // Check all cells IN THE MIDDLE of the path (from index 1 to size-2) for blockers.
        for (int i = 1; i < fullPath.size() - 1; i++) {
            Cell cellInPath = fullPath.get(i);
            Marble marbleInPath = cellInPath.getMarble();

            if (marbleInPath != null) {
                // For a King's move, all path blockers are ignored (they will be destroyed).
                if (isKingMove) continue;

                // Rule: Cannot bypass a marble on its own protected Base Cell.
                if (cellInPath.getCellType() == CellType.BASE && getBasePosition(marbleInPath.getColour()) == track.indexOf(cellInPath)) {
                    throw new IllegalMovementException("Cannot bypass a marble protecting its own Base Cell.");
                }

                // Rule: Cannot bypass your own marbles on the main track.
                if (marbleInPath.getColour() == ownerColour && cellInPath.getCellType() != CellType.SAFE) {
                    throw new IllegalMovementException("Cannot bypass your own marble on the main track.");
                }

                // Rule: Cannot bypass an opponent's marble.
                if (marbleInPath.getColour() != ownerColour) {
                    throw new IllegalMovementException("Path is blocked by an opponent's marble.");
                }
            }
        }
    }

    private void move(Marble marble, ArrayList<Cell> fullPath, boolean isKing, ActionResult result) throws IllegalDestroyException {
        Cell currentCell = fullPath.get(0);
        Cell targetCell = fullPath.get(fullPath.size()-1);

        currentCell.setMarble(null);

        if (isKing) {
            // King's move destroys everything in its path, including the target cell
            for(int i = 1; i < fullPath.size(); i++) {
                if (fullPath.get(i).getMarble() != null) {
                    destroyMarble(fullPath.get(i).getMarble(), result);
                }
            }
        } else if (targetCell.getMarble() != null) {
            // Standard move destroys only the marble on the target cell
            destroyMarble(targetCell.getMarble(), result);
        }

        targetCell.setMarble(marble);
        result.addAnimation(new AnimationStep.Move(marble, fullPath));

        if(targetCell.isTrap() && targetCell.getCellType() == CellType.NORMAL) {
            destroyMarble(marble, result);
            targetCell.setTrap(false);
            assignTrapCell();
        }
    }
    
    private void validateSwap(Marble marble_1, Marble marble_2) throws IllegalSwapException {
        if (marble_1 == null || marble_2 == null) throw new IllegalSwapException("Must select two marbles to swap.");
        if (getPositionInPath(track, marble_1) == -1 || getPositionInPath(track, marble_2) == -1) {
            throw new IllegalSwapException("Both marbles must be on the main track to be swapped.");
        }
        Cell cell1 = findCellForMarble(marble_1);
        if (cell1.getCellType() == CellType.BASE || cell1.getCellType() == CellType.SAFE) {
             throw new IllegalSwapException("Cannot swap a marble that is on a Base or in a Safe Zone.");
        }
        Cell cell2 = findCellForMarble(marble_2);
        if (cell2.getCellType() == CellType.BASE || cell2.getCellType() == CellType.SAFE) {
             throw new IllegalSwapException("Cannot swap with a marble that is on a Base or in a Safe Zone.");
        }
    }
    
    private void validateDestroy(Marble marbleToDestroy) throws IllegalDestroyException {
        Cell cell = findCellForMarble(marbleToDestroy);
        if (cell == null || cell.getCellType() == CellType.SAFE) {
            throw new IllegalDestroyException("Cannot destroy a marble that is safe.");
        }
        if (cell.getCellType() == CellType.BASE && track.indexOf(cell) == getBasePosition(marbleToDestroy.getColour())) {
            throw new IllegalDestroyException("Cannot destroy a marble that is on its own protected Base Cell.");
        }
    }
    
    private void validateFielding(Marble marbleOnBase) throws CannotFieldException {
        if (marbleOnBase.getColour() == gameManager.getActivePlayerColour())
            throw new CannotFieldException("Your own marble is already on your Base Cell.");
    }
    
    private void validateSaving(Marble marbleToSave) throws InvalidMarbleException {
        if (getPositionInPath(track, marbleToSave) == -1) {
            throw new InvalidMarbleException("Cannot save a marble that is not on the main track.");
        }
    }

    @Override
    public int getSplitDistance() {
        return this.splitDistance;
    }

    @Override
    public void moveBy(Marble marble, int steps, boolean isKing, ActionResult result) throws IllegalMovementException, IllegalDestroyException {
        ArrayList<Cell> fullPath = validateSteps(marble, steps);
        validatePath(marble, fullPath, isKing);
        move(marble, fullPath, isKing, result);
    }

    @Override
    public void swap(Marble marble1, Marble marble2, ActionResult result) throws IllegalSwapException {
        validateSwap(marble1, marble2);

        Cell startCell1 = findCellForMarble(marble1);
        Cell startCell2 = findCellForMarble(marble2);

        // Add animation BEFORE changing the model
        result.addAnimation(new AnimationStep.Swap(marble1, startCell1, startCell2, marble2, startCell2, startCell1));
        
        startCell1.setMarble(marble2);
        startCell2.setMarble(marble1);
    }

    @Override
    public void destroyMarble(Marble marble, ActionResult result) throws IllegalDestroyException {
        Cell marbleCell = findCellForMarble(marble);
        if (marbleCell == null) return; // Already gone, nothing to do.

        // Only validate if an OPPONENT marble is being destroyed by a card effect.
        if (marble.getColour() != gameManager.getActivePlayerColour()) {
            validateDestroy(marble);
        }

        result.addAnimation(new AnimationStep.Destroy(marble, marbleCell));

        marbleCell.setMarble(null);
        this.gameManager.sendHome(marble, result);
    }

    @Override
    public void sendToBase(Marble marble, ActionResult result) throws CannotFieldException, IllegalDestroyException {
        int basePosition = getBasePosition(marble.getColour());
        Cell baseCell = this.track.get(basePosition);

        if(baseCell.getMarble() != null) {
            validateFielding(baseCell.getMarble());
            destroyMarble(baseCell.getMarble(), result);
        }
        
        result.addAnimation(new AnimationStep.Field(marble, baseCell));
        baseCell.setMarble(marble);
    }

    @Override
    public void sendToSafe(Marble marble, ActionResult result) throws InvalidMarbleException {
        validateSaving(marble);
        Cell fromCell = findCellForMarble(marble);
        ArrayList<Cell> safeZone = getSafeZoneCells(marble.getColour());
        Cell targetCell = null;
        if (safeZone != null) {
            for(Cell cell : safeZone){
                if(cell.getMarble() == null) {
                    targetCell = cell;
                    break;
                }
            }
        }
        if (targetCell == null) throw new InvalidMarbleException("Cannot save marble, safe zone is full.");

        result.addAnimation(new AnimationStep.Save(marble, fromCell, targetCell));
        targetCell.setMarble(marble);
        if (fromCell != null) fromCell.setMarble(null);
    }
    
    @Override
    public void checkMove(Marble marble, int steps, boolean isKing) throws IllegalMovementException {
        // Run the exact same checks as moveBy, but stop before executing move()
        ArrayList<Cell> fullPath = validateSteps(marble, steps);
        validatePath(marble, fullPath, isKing);
    }

    @Override
    public void checkSwap(Marble marble1, Marble marble2) throws IllegalSwapException {
        validateSwap(marble1, marble2);
    }

    @Override
    public void checkDestroy(Marble marble) throws IllegalDestroyException {
        // Note: Logic copied from destroyMarble validation check
        if (marble.getColour() != gameManager.getActivePlayerColour()) {
            validateDestroy(marble);
        }
    }

    @Override
    public void checkField(Marble marble) throws CannotFieldException {
        int basePosition = getBasePosition(marble.getColour());
        Cell baseCell = this.track.get(basePosition);
        if(baseCell.getMarble() != null) {
            validateFielding(baseCell.getMarble());
        }
    }

    @Override
    public void checkSafe(Marble marble) throws InvalidMarbleException {
        validateSaving(marble);
        ArrayList<Cell> safeZone = getSafeZoneCells(marble.getColour());
        boolean hasSpace = false;
        if (safeZone != null) {
            for(Cell cell : safeZone){
                if(cell.getMarble() == null) {
                    hasSpace = true;
                    break;
                }
            }
        }
        if (!hasSpace) throw new InvalidMarbleException("Cannot save marble, safe zone is full.");
    }
    
    @Override
    public ArrayList<Marble> getActionableMarbles() {
        ArrayList<Marble> marbles = new ArrayList<>();
        Colour activePlayerColour = gameManager.getActivePlayerColour();
        
        for (Cell cell : this.track) {
            if (cell.getMarble() != null && cell.getMarble().getColour() == activePlayerColour) {
                marbles.add(cell.getMarble());
            }
        }
        ArrayList<Cell> safeZoneCells = getSafeZoneCells(activePlayerColour);
        if (safeZoneCells != null) {
            for (Cell cell : safeZoneCells) {
                if (cell.getMarble() != null) {
                    marbles.add(cell.getMarble());
                }
            }
        }
        return marbles;
    }
    
    public Cell findCellForMarble(Marble marbleToFind) {
        if (marbleToFind == null) return null;
        for (Cell cell : this.track) {
            if (cell.getMarble() == marbleToFind) return cell;
        }
        for (SafeZone sz : this.safeZones) {
            for (Cell safeCell : sz.getCells()) {
                if (safeCell.getMarble() == marbleToFind) return safeCell;
            }
        }
        return null;
    }
}