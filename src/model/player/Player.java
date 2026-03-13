package model.player;

import java.util.ArrayList;

import engine.GameManager;
import engine.action.ActionResult;
import exception.GameException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.MarbleSelectionNeededException;
import model.Colour;
import model.card.Card;

public class Player {
    private final String name;
    private final Colour colour;
    private ArrayList<Card> hand;
    private final ArrayList<Marble> marbles; // Home zone marbles
    private Card selectedCard;
    private final ArrayList<Marble> selectedMarbles; // Marbles selected for action
    // Player depends on the GameManager INTERFACE, not the concrete Game class.
    // This is the key architectural fix: the model layer must not reference the
    // engine implementation. Game implements GameManager, so Game.setGameReference(this)
    // still works through polymorphism — but Player no longer knows about Game.
    protected GameManager game;

    public Player(String name, Colour colour) {
        this.name = name;
        this.colour = colour;
        this.hand = new ArrayList<>();
        this.selectedMarbles = new ArrayList<>();
        this.marbles = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            this.marbles.add(new Marble(colour));
        }
        this.selectedCard = null;
        // this.game = null; // Initialized by Game class
    }
    
    public void setGameReference(GameManager gameInstance) {
        this.game = gameInstance;
    }

    public String getName() {
        return name;
    }

    public Colour getColour() {
        return colour;
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public void setHand(ArrayList<Card> hand) {
        this.hand = hand;
    }
    
    public ArrayList<Marble> getMarbles() {
		return marbles;
	}
    
    public Card getSelectedCard() {
        return selectedCard;
    }
    
    public void regainMarble(Marble marble) {
        this.marbles.add(marble);
    }

    public Marble getOneMarble() {
        if(marbles.isEmpty())
            return null;

        return this.marbles.get(0);
    }

    public void selectCard(Card card) throws InvalidCardException {
        if (!this.hand.contains(card)) 
            throw new InvalidCardException("Card not in hand.");
        
        this.selectedCard = card;
    }

    public void selectMarble(Marble marble) throws InvalidMarbleException {
        if (this.selectedMarbles.contains(marble)) {
        	this.selectedMarbles.remove(marble); // Deselect on re-click
            return; 
        }

        if (this.selectedMarbles.size() >= 2) { // Corrected condition
            throw new InvalidMarbleException("Cannot select more than 2 marbles. Please deselect first or play your turn.");
        }
        
        // Additional check: If a card is selected, does it even allow this many marbles?
        // This is usually handled by the GUI calling validateMarbleSize before allowing play,
        // or by Player.play() calling card.validateMarbleSize().
        // For selection itself, just limiting to 2 is generally okay.
        
        selectedMarbles.add(marble);
    }

    public void deselectAll() {
        this.selectedCard = null;
        this.selectedMarbles.clear();
    }

    // --- MODIFIED play() method ---
    public void play(ActionResult result) throws GameException {
        if (selectedCard == null) {
            throw new InvalidCardException("No card selected to play.");
        }

        // --- START NEW LOGIC for MarbleSelectionNeededException ---
        boolean cardRequiresMarblesForPrimaryAction = doesCardRequireMarbles(selectedCard);

        if (cardRequiresMarblesForPrimaryAction && selectedMarbles.isEmpty()) {
            if (this.game != null && playerHasActionableMarblesOnBoard()) {
                throw new MarbleSelectionNeededException(); // Throw specific exception
            } else {
                // If no actionable marbles, then it's a genuine InvalidMarbleException
                // or the card might have a 0-marble action (like Ace/King fielding)
                // The original validateMarbleSize will handle this.
            }
        }
        // --- END NEW LOGIC ---

        // Original validation calls proceed
        if (!selectedCard.validateMarbleSize(selectedMarbles)) {
            // This will now primarily catch cases where:
            // 1. Too many marbles are selected.
            // 2. 0 marbles selected for a card that strictly needs >0 and has no 0-marble alternative,
            //    AND the player had no actionable marbles (so MarbleSelectionNeededException wasn't thrown).
            // 3. Wrong number of marbles for a specific alternative action (e.g. 1 for Jack when not standard move).
            throw new InvalidMarbleException("Invalid number of marbles selected for the card: " + selectedCard.getName());
        }

        if (!selectedCard.validateMarbleColours(selectedMarbles)) {
            throw new InvalidMarbleException("Invalid colour of marbles selected for the card: " + selectedCard.getName());
        }

        selectedCard.act(selectedMarbles, game, result);
    }

    // HELPER METHOD: doesCardRequireMarbles
    //
    // Ask the card itself whether it can act on zero marbles — don't guess by name.
    //
    // The previous version used card.getName().toUpperCase().contains("ACE") and similar
    // string checks. That is a Tell-Don't-Ask violation: Player was classifying cards
    // by their names instead of querying their declared behaviour.
    //
    // The Card hierarchy already encodes this capability via validateMarbleSize():
    //   - Ace/King/Queen/Ten override it to accept an empty list   → they do NOT require marbles
    //   - Everything else (Standard, Four, Five, Jack, Seven, ...) → they DO require marbles
    //
    // One call to validateMarbleSize(emptyList) is the only authoritative answer.
    // Renaming "Ace" to "As" tomorrow would silently break 8 string checks; it will
    // never break this one.
    private boolean doesCardRequireMarbles(Card card) {
        return !card.validateMarbleSize(new ArrayList<>());
    }

    // Uses BoardManager.getActionableMarbles() through the selected card's
    // board manager instead of reaching into Game.getBoard(). This keeps the
    // model layer decoupled from the engine — Player never needs to know that
    // Game exists.
    private boolean playerHasActionableMarblesOnBoard() {
        if (selectedCard == null) return false;
        var bm = selectedCard.getBoardManager();
        if (bm == null) return false;
        return !bm.getActionableMarbles().isEmpty();
    }

    
    public ArrayList<Marble> getSelectedMarblesPublic() { // Or just make selectedMarbles getter public
        return selectedMarbles;
    }

}
