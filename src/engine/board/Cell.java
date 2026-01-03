// PASTE THIS ENTIRE CODE INTO your Cell.java file.

package engine.board;

import model.Colour;
import model.player.Marble;

public class Cell {
    private Marble marble;
    private CellType cellType;
    private boolean trap;
    private final Colour colour; // <<< NEW: The owner's colour for this cell, if any.
    private final int index; // <<< NEW: The cell's position on the main track (0-99) or -1 if not on track.

    // Constructor for cells WITH a specific owner (BASE, ENTRY, SAFE)
    public Cell(CellType cellType, Colour ownerColour, int index) {
        this.cellType = cellType;
        this.marble = null;
        this.trap = false;
        this.colour = ownerColour; // <<< NEW
        this.index = index; // <<< NEW
    }
    
    // Constructor for cells NOT on the main track (SAFE)
    public Cell(CellType cellType, Colour ownerColour) {
        this(cellType, ownerColour, -1); // Default index to -1 for safe cells
    }
    
    // Original constructor for NORMAL cells, now calls the main one.
    public Cell(CellType cellType, int index) {
        this(cellType, null, index);
    }

	public Marble getMarble() {
		return marble;
	}

	public void setMarble(Marble marble) {
		this.marble = marble;
	}

	public CellType getCellType() {
		return cellType;
	}

	public void setCellType(CellType cellType) {
		this.cellType = cellType;
	}

	public boolean isTrap() {
		return trap;
	}

	public void setTrap(boolean trap) {
		this.trap = trap;
	}
	
	// <<< NEW: The required getter method.
	public Colour getColour() {
	    return this.colour;
	}
	
	// <<< NEW: The required getter method.
		public int getIndex() {
		    return this.index;
		}
}