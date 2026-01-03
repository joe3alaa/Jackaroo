// PASTE THIS ENTIRE CODE INTO your GameManager.java file.

package engine;

import engine.action.ActionResult;
import exception.CannotDiscardException;
import exception.CannotFieldException;
import exception.IllegalDestroyException;
import model.Colour;
import model.player.Marble;

public interface GameManager {
	void sendHome(Marble marble, ActionResult result);
    void fieldMarble(ActionResult result) throws CannotFieldException, IllegalDestroyException;
    void discardCard(Colour colour, ActionResult result) throws CannotDiscardException;
    void discardFromAndSkipNextPlayer(ActionResult result) throws CannotDiscardException;
    void discardFromAndSkipRandomPlayer(ActionResult result) throws CannotDiscardException;
    Colour getActivePlayerColour();
    Colour getNextPlayerColour();
}