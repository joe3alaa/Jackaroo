package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import exception.InvalidMarbleException;
import model.player.Marble;
import java.util.ArrayList;

public class SwapStrategy implements CardActionStrategy {
    
    @Override
    public void validate(ArrayList<Marble> marbles, BoardManager boardManager, 
                        GameManager gameManager) throws GameException {
        if (marbles == null || marbles.size() != 2) {
            throw new InvalidMarbleException("Swap requires exactly 2 marbles");
        }
        boardManager.checkSwap(marbles.get(0), marbles.get(1));
    }
    
    @Override
    public void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                       GameManager gameManager, ActionResult result) 
                       throws GameException {
        if (marbles == null || marbles.size() != 2) {
            throw new InvalidMarbleException("Swap requires exactly 2 marbles");
        }
        boardManager.swap(marbles.get(0), marbles.get(1), result);
    }
}