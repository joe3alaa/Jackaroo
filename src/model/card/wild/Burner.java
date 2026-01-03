package model.card.wild;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.strategy.CardStrategyFactory;
import model.player.Marble;
import java.util.ArrayList;

public class Burner extends Wild {

    public Burner(String name, String description, 
                  BoardManager boardManager, GameManager gameManager) {
        super(name, description, boardManager, gameManager,
              CardStrategyFactory.createBurnerStrategy()); // <<< Use factory
    }
    
    @Override
    public boolean validateMarbleColours(ArrayList<Marble> marbles) {
        return !super.validateMarbleColours(marbles); // Can only target opponents
    }
}