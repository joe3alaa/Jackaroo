package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import exception.InvalidMarbleException;
import model.player.Marble;
import java.util.ArrayList;

public class DestroyStrategy implements CardActionStrategy {
    
    @Override
    public void validate(ArrayList<Marble> marbles, BoardManager boardManager, 
                        GameManager gameManager) throws GameException {
        if (marbles == null || marbles.isEmpty()) {
            throw new InvalidMarbleException("Burner requires a marble to destroy");
        }
        boardManager.checkDestroy(marbles.get(0));
    }
    
    @Override
    public void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                       GameManager gameManager, ActionResult result) 
                       throws GameException {
        if (marbles == null || marbles.isEmpty()) {
            throw new InvalidMarbleException("Burner requires a marble to destroy");
        }
        boardManager.destroyMarble(marbles.get(0), result);
    }
}