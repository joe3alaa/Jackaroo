// PASTE THIS ENTIRE CODE INTO your SafeZone.java file.

package engine.board;

import java.util.ArrayList;
import model.Colour;

public class SafeZone {
    private final Colour colour;
    private final ArrayList<Cell> cells;

    public SafeZone(Colour colour) {
        this.colour = colour;
        this.cells = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            // <<< CHANGE: Pass the owner's colour to the Safe cells
            this.cells.add(new Cell(CellType.SAFE, colour));
        }
    }

    public Colour getColour() {
        return this.colour;
    }

    public ArrayList<Cell> getCells() {
        return this.cells;
    }
    
    public boolean isFull() {
        for (Cell cell : this.cells) {
            if (cell.getMarble() == null) {
                return false;
            }
        }
        return true;
    }
}