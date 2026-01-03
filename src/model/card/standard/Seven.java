package model.card.standard;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.strategy.CardStrategyFactory;
import model.player.Marble;
import java.util.ArrayList;

public class Seven extends Standard {

    public Seven(String name, String description, Suit suit, 
                 BoardManager boardManager, GameManager gameManager) {
        super(name, description, 7, suit, boardManager, gameManager,
              CardStrategyFactory.createSevenStrategy()); // <<< Use factory
    }

    @Override
    public boolean validateMarbleSize(ArrayList<Marble> marbles) {
        if (marbles == null) return false;
        return marbles.size() == 2 || super.validateMarbleSize(marbles);
    }
}