package model.card.wild;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.Card;
import model.card.strategy.CardActionStrategy;

public abstract class Wild extends Card {

    // <<< UPDATED CONSTRUCTOR: Now accepts strategy
    public Wild(String name, String description, BoardManager boardManager, 
                GameManager gameManager, CardActionStrategy strategy) {
        super(name, description, boardManager, gameManager, strategy);
    }
}