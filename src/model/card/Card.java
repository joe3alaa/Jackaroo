package model.card;

import java.util.ArrayList;
import engine.GameManager;
import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import model.Colour;
import model.card.strategy.CardActionStrategy; // <<< NEW IMPORT
import model.player.Marble;

public abstract class Card {
    private final String name;
    private final String description;
    protected BoardManager boardManager;
    protected GameManager gameManager;
    protected CardActionStrategy strategy; // <<< NEW FIELD

    // <<< UPDATED CONSTRUCTOR: Now accepts a strategy
    public Card(String name, String description, BoardManager boardManager, 
                GameManager gameManager, CardActionStrategy strategy) {
        this.name = name;
        this.description = description;
        this.boardManager = boardManager;
        this.gameManager = gameManager;
        this.strategy = strategy; // <<< NEW
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    
    /**
     * Executes the action - now delegates to strategy.
     */
    public void act(ArrayList<Marble> marbles, GameManager gameManager, ActionResult result) 
            throws GameException {
        strategy.execute(marbles, boardManager, gameManager, result);
    }
    
    /**
     * Validates the action - now delegates to strategy.
     */
    public void validate(ArrayList<Marble> marbles, GameManager gameManager) throws GameException {
        strategy.validate(marbles, boardManager, gameManager);
    }
    
    // These methods remain unchanged
    public boolean validateMarbleSize(ArrayList<Marble> marbles) {
        if (marbles == null) return false;
        return marbles.size() == 1;
    }
    
    public boolean validateMarbleColours(ArrayList<Marble> marbles) {
        Colour ownerColour = gameManager.getActivePlayerColour();
        for (Marble marble : marbles) {
            if (marble.getColour() != ownerColour) {
                return false;
            }
        }
        return true;
    }
}
