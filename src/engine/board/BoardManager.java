// PASTE THIS ENTIRE CODE INTO your BoardManager.java file.

package engine.board;

import java.util.ArrayList;
import engine.action.ActionResult;
import exception.*;
import model.player.Marble;

public interface BoardManager { 
    int getSplitDistance();
    
    // --- Execution Methods (Change State) ---
    void moveBy(Marble marble, int steps, boolean isKing, ActionResult result) throws IllegalMovementException, IllegalDestroyException;
    void swap(Marble marble_1, Marble marble_2, ActionResult result) throws IllegalSwapException;
    void destroyMarble(Marble marble, ActionResult result) throws IllegalDestroyException;
    void sendToBase(Marble marble, ActionResult result) throws CannotFieldException, IllegalDestroyException;
    void sendToSafe(Marble marble, ActionResult result) throws InvalidMarbleException;
    
    // --- Validation Methods (Read-Only, No State Change) ---
    void checkMove(Marble marble, int steps, boolean isKing) throws IllegalMovementException;
    void checkSwap(Marble marble_1, Marble marble_2) throws IllegalSwapException;
    void checkDestroy(Marble marble) throws IllegalDestroyException;
    void checkField(Marble marble) throws CannotFieldException;
    void checkSafe(Marble marble) throws InvalidMarbleException;

    ArrayList<Marble> getActionableMarbles();
}