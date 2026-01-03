package model.card.standard;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.strategy.CardStrategyFactory;
import java.util.ArrayList;
import model.player.Marble;

public class King extends Standard {

    public King(String name, String description, Suit suit, 
                BoardManager boardManager, GameManager gameManager) {
        super(name, description, 13, suit, boardManager, gameManager,
              CardStrategyFactory.createKingStrategy()); // <<< Use factory
    }

    @Override
    public boolean validateMarbleSize(ArrayList<Marble> marbles) {
        return marbles.isEmpty() || super.validateMarbleSize(marbles);
    }
}