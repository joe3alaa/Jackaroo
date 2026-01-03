
package model.player;

import engine.action.ActionResult;
import engine.board.BoardManager;
import exception.GameException;
import model.Colour;
import model.card.Card;
import model.card.standard.Queen;
import model.card.standard.Ten;

import java.util.ArrayList;
import java.util.Collections;

public class CPU extends Player {
    private final BoardManager boardManager;

    public CPU(String name, Colour colour, BoardManager boardManager) {
        super(name, colour);
        this.boardManager = boardManager;
    }

    @Override
    public void play(ActionResult result) throws GameException {
        ArrayList<Marble> actionableMarbles = boardManager.getActionableMarbles();
        ArrayList<Card> cards = new ArrayList<>(this.getHand());
        Collections.shuffle(cards);

        for (Card card : cards) {
            this.selectCard(card);

            // 1. Try Zero-Marble Actions (Discard/Skip/Fielding)
            if (card instanceof Ten || card instanceof Queen || card.validateMarbleSize(new ArrayList<>())) {
                try {
                    // <<< FIX: Validate FIRST >>>
                    card.validate(new ArrayList<>(), this.game);
                    
                    // If valid, then ACT
                    System.out.println("CPU " + getName() + ": Playing " + card.getName() + " (No marbles)");
                    getSelectedCard().act(new ArrayList<>(), this.game, result);
                    return;
                } catch (GameException e) { /* Invalid, keep looking */ }
            }

            // Determine marble counts to try
            ArrayList<Integer> counts = new ArrayList<>();
            if (!actionableMarbles.isEmpty() && card.validateMarbleSize(new ArrayList<>(Collections.singletonList(actionableMarbles.get(0))))) counts.add(1);
            if (actionableMarbles.size() >= 2 && card.validateMarbleSize(new ArrayList<>(actionableMarbles.subList(0, 2)))) counts.add(2);
            Collections.shuffle(counts);

            for (int countToTry : counts) {
                if (countToTry == 1) {
                    ArrayList<Marble> shuffledMarbles = new ArrayList<>(actionableMarbles);
                    Collections.shuffle(shuffledMarbles);
                    for (Marble marble : shuffledMarbles) {
                        ArrayList<Marble> toSend = new ArrayList<>(Collections.singletonList(marble));
                        if (card.validateMarbleColours(toSend)) {
                            try {
                                // <<< FIX: Validate FIRST >>>
                                card.validate(toSend, this.game);
                                
                                // If valid, then ACT
                                getSelectedCard().act(toSend, this.game, result);
                                return;
                            } catch (Exception e) { /* Invalid */ }
                        }
                    }
                } else if (countToTry == 2) {
                    ArrayList<Marble> shuffledMarbles = new ArrayList<>(actionableMarbles);
                    Collections.shuffle(shuffledMarbles);
                    for (int j = 0; j < shuffledMarbles.size(); j++) {
                        for (int k = j + 1; k < shuffledMarbles.size(); k++) {
                            ArrayList<Marble> toSend = new ArrayList<>();
                            toSend.add(shuffledMarbles.get(j));
                            toSend.add(shuffledMarbles.get(k));
                            if (card.validateMarbleColours(toSend)) {
                                try {
                                    // <<< FIX: Validate FIRST >>>
                                    card.validate(toSend, this.game);
                                    
                                    // If valid, then ACT
                                    getSelectedCard().act(toSend, this.game, result);
                                    return;
                                } catch (Exception e) { /* Invalid */ }
                            }
                        }
                    }
                }
            }
        }

        // If no action was taken, discard logic
        if (!this.getHand().isEmpty()) {
            System.out.println("CPU " + getName() + ": Stuck. Discarding " + this.getHand().get(0).getName());
            this.selectCard(this.getHand().get(0));
        }
    }
}