package model.card.standard;

import engine.GameManager;
import engine.board.BoardManager;
import model.card.strategy.CardStrategyFactory;
import model.Colour;
import model.player.Marble;
import java.util.ArrayList;

public class Jack extends Standard {

    public Jack(String name, String description, Suit suit, 
                BoardManager boardManager, GameManager gameManager) {
        super(name, description, 11, suit, boardManager, gameManager,
              CardStrategyFactory.createJackStrategy()); // <<< Use factory
    }
    
    @Override
    public boolean validateMarbleSize(ArrayList<Marble> marbles) {
        return marbles.size() == 2 || super.validateMarbleSize(marbles);
    }

    @Override
    public boolean validateMarbleColours(ArrayList<Marble> marbles) {
        if(marbles.size() == 2) {
            Colour myColour = gameManager.getActivePlayerColour();
            return (marbles.get(0).getColour() == myColour && marbles.get(1).getColour() != myColour) ||
                   (marbles.get(1).getColour() == myColour && marbles.get(0).getColour() != myColour);       
        }
        return super.validateMarbleColours(marbles);
    }
}