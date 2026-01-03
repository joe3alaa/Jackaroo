package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import exception.InvalidMarbleException;
import model.player.Marble;
import java.util.ArrayList;

/**
 * Standard forward/backward movement strategy.
 * Used by cards 2, 3, 5, 6, 8, 9, 10, 11, 12, 13.
 */
public class StandardMoveStrategy implements CardActionStrategy {
    private final int steps;
    
    public StandardMoveStrategy(int steps) {
        this.steps = steps;
    }
    
    @Override
    public void validate(ArrayList<Marble> marbles, BoardManager boardManager, 
                        GameManager gameManager) throws GameException {
        if (marbles == null || marbles.isEmpty()) {
            throw new InvalidMarbleException("Standard move requires a marble");
        }
        boardManager.checkMove(marbles.get(0), steps, false);
    }
    
    @Override
    public void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                       GameManager gameManager, ActionResult result) 
                       throws GameException {
        if (marbles == null || marbles.isEmpty()) {
            throw new InvalidMarbleException("Standard move requires a marble");
        }
        boardManager.moveBy(marbles.get(0), steps, false, result);
    }
}