package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import exception.InvalidMarbleException;
import model.player.Marble;
import java.util.ArrayList;

public class KingMoveStrategy implements CardActionStrategy {
    private static final int KING_STEPS = 13;
    
    @Override
    public void validate(ArrayList<Marble> marbles, BoardManager boardManager, 
                        GameManager gameManager) throws GameException {
        if (marbles == null || marbles.isEmpty()) {
            throw new InvalidMarbleException("King move requires a marble");
        }
        boardManager.checkMove(marbles.get(0), KING_STEPS, true);
    }
    
    @Override
    public void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                       GameManager gameManager, ActionResult result) 
                       throws GameException {
        if (marbles == null || marbles.isEmpty()) {
            throw new InvalidMarbleException("King move requires a marble");
        }
        boardManager.moveBy(marbles.get(0), KING_STEPS, true, result);
    }
}