package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import exception.InvalidMarbleException;
import model.player.Marble;
import java.util.ArrayList;

public class SevenSplitStrategy implements CardActionStrategy {
    
    @Override
    public void validate(ArrayList<Marble> marbles, BoardManager boardManager, 
                        GameManager gameManager) throws GameException {
        if (marbles == null || marbles.size() != 2) {
            throw new InvalidMarbleException("Seven split requires exactly 2 marbles");
        }
        
        int split = boardManager.getSplitDistance();
        int remainder = 7 - split;
        
        boardManager.checkMove(marbles.get(0), split, false);
        boardManager.checkMove(marbles.get(1), remainder, false);
    }
    
    @Override
    public void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                       GameManager gameManager, ActionResult result) 
                       throws GameException {
        if (marbles == null || marbles.size() != 2) {
            throw new InvalidMarbleException("Seven split requires exactly 2 marbles");
        }
        
        int split = boardManager.getSplitDistance();
        boardManager.moveBy(marbles.get(0), split, false, result);
        boardManager.moveBy(marbles.get(1), 7 - split, false, result);
    }
}