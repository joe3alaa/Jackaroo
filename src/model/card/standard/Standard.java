package model.card.standard;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.Card;
import model.card.strategy.CardActionStrategy;
import model.card.strategy.CardStrategyFactory;

public class Standard extends Card {
    private final int rank;
    private final Suit suit;

    /**
     * Constructor for subclasses (Ace, King, etc.) that provide their own strategy.
     */
    public Standard(String name, String description, int rank, Suit suit, 
                   BoardManager boardManager, GameManager gameManager,
                   CardActionStrategy strategy) {
        super(name, description, boardManager, gameManager, strategy);
        this.rank = rank;
        this.suit = suit;
    }
    
    /**
     * Constructor for generic standard cards (2, 3, 6, 8, 9).
     * These cards use a simple StandardMoveStrategy.
     */
    public Standard(String name, String description, int rank, Suit suit, 
                   BoardManager boardManager, GameManager gameManager) {
        // For generic cards, create a standard move strategy with the rank
        this(name, description, rank, suit, boardManager, gameManager,
             CardStrategyFactory.createStandardStrategy(rank));
    }

    public int getRank() {
        return rank;
    }

    public Suit getSuit() {
        return suit;
    }
}