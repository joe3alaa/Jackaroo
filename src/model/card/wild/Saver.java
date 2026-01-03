package model.card.wild;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.strategy.CardStrategyFactory;

public class Saver extends Wild {

    public Saver(String name, String description, 
                 BoardManager boardManager, GameManager gameManager) {
        super(name, description, boardManager, gameManager,
              CardStrategyFactory.createSaverStrategy()); // <<< Use factory
    }
}