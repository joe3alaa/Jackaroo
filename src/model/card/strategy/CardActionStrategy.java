package model.card.strategy;

import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException; // <<< KEY FIX: Use GameException, not ActionException
import model.player.Marble;
import java.util.ArrayList;

/**
 * Strategy interface for card actions.
 * Each concrete strategy encapsulates ONE type of card behavior.
 */
public interface CardActionStrategy {
    
    /**
     * Validates that this action can be performed.
     * MUST NOT modify game state.
     * 
     * @throws GameException if action is invalid (covers all game exceptions)
     */
    void validate(ArrayList<Marble> marbles, BoardManager boardManager, GameManager gameManager) 
        throws GameException; // <<< FIXED: GameException is the parent of all
    
    /**
     * Executes the action and records animations.
     */
    void execute(ArrayList<Marble> marbles, BoardManager boardManager, 
                 GameManager gameManager, ActionResult result) 
        throws GameException; // <<< FIXED: Simplified to just GameException
}