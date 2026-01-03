package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import exception.InvalidMarbleException;
import model.player.Marble;
import java.util.ArrayList;

/**
 * Composite strategy for cards with multiple possible actions.
 * Example: Ace can field OR move 1.
 */
public class CompositeCardStrategy implements CardActionStrategy {
    private final ArrayList<CardActionStrategy> strategies;
    
    public CompositeCardStrategy(CardActionStrategy... strategiesArray) {
        this.strategies = new ArrayList<>();
        for (CardActionStrategy s : strategiesArray) {
            this.strategies.add(s);
        }
    }
    
    @Override
    public void validate(ArrayList<Marble> marbles, BoardManager boardManager, 
                        GameManager gameManager) throws GameException {
        GameException lastException = null;
        
        for (CardActionStrategy strategy : strategies) {
            try {
                strategy.validate(marbles, boardManager, gameManager);
                return; // At least one strategy is valid
            } catch (GameException e) {
                lastException = e;
            }
        }
        
        throw lastException != null ? lastException : 
            new InvalidMarbleException("No valid action for this card");
    }
    
    @Override
    public void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                       GameManager gameManager, ActionResult result) 
                       throws GameException {
        for (CardActionStrategy strategy : strategies) {
            try {
                strategy.validate(marbles, boardManager, gameManager);
                strategy.execute(marbles, boardManager, gameManager, result);
                return;
            } catch (GameException e) {
                // Try next strategy
            }
        }
        
        throw new InvalidMarbleException("No valid action could be executed");
    }
}