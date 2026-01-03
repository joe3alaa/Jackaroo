package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import exception.InvalidMarbleException;
import model.player.Marble;
import java.util.ArrayList;

public class FieldingStrategy implements CardActionStrategy {
    
    @Override
    public void validate(ArrayList<Marble> marbles, BoardManager boardManager, 
                        GameManager gameManager) throws GameException {
        if (marbles != null && !marbles.isEmpty()) {
            throw new InvalidMarbleException("Fielding action requires NO marbles selected");
        }
        // Actual validation happens in execute - fielding checks are safe there
    }
    
    @Override
    public void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                       GameManager gameManager, ActionResult result) 
                       throws GameException {
        gameManager.fieldMarble(result);
    }
}