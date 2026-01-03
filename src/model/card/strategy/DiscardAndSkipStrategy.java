package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import exception.InvalidMarbleException;
import model.player.Marble;
import java.util.ArrayList;

public class DiscardAndSkipStrategy implements CardActionStrategy {
    private final boolean skipNext; // true for Ten, false for Queen
    
    public DiscardAndSkipStrategy(boolean skipNext) {
        this.skipNext = skipNext;
    }
    
    @Override
    public void validate(ArrayList<Marble> marbles, BoardManager boardManager, 
                        GameManager gameManager) throws GameException {
        if (marbles != null && !marbles.isEmpty()) {
            throw new InvalidMarbleException("Discard action requires NO marbles selected");
        }
    }
    
    @Override
    public void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                       GameManager gameManager, ActionResult result) 
                       throws GameException {
        if (skipNext) {
            gameManager.discardFromAndSkipNextPlayer(result);
        } else {
            gameManager.discardFromAndSkipRandomPlayer(result);
        }
    }
}