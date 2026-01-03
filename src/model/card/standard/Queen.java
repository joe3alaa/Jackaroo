package model.card.standard;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.strategy.CardStrategyFactory;
import model.player.Marble;
import java.util.ArrayList;

public class Queen extends Standard {

    public Queen(String name, String description, Suit suit, 
                 BoardManager boardManager, GameManager gameManager) {
        super(name, description, 12, suit, boardManager, gameManager,
              CardStrategyFactory.createQueenStrategy()); // <<< Use factory
    }

    @Override
    public boolean validateMarbleSize(ArrayList<Marble> marbles) {
        return marbles.isEmpty() || super.validateMarbleSize(marbles);
    }
}