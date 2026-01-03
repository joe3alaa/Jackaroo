package model.card.standard;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.strategy.CardStrategyFactory;
import java.util.ArrayList;
import model.player.Marble;

public class Ace extends Standard {
    
    public Ace(String name, String description, Suit suit, 
               BoardManager boardManager, GameManager gameManager) {
        // Pass the Ace strategy from the factory
        super(name, description, 1, suit, boardManager, gameManager,
              CardStrategyFactory.createAceStrategy()); // <<< THIS IS THE KEY LINE
    }

    // Keep this validation override for marble size
    @Override
    public boolean validateMarbleSize(ArrayList<Marble> marbles) {
        return marbles.isEmpty() || super.validateMarbleSize(marbles);
    }
    
    // <<< REMOVE validate() and act() - the strategy handles them!
}
