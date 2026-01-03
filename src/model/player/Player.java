package model.player;

import java.util.ArrayList;

import engine.Game;
import engine.action.ActionResult;
import engine.board.Board;
import engine.board.Cell;
import engine.board.SafeZone;
import exception.GameException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.MarbleSelectionNeededException;
import model.Colour;
import model.card.Card;
import model.card.standard.Standard;

@SuppressWarnings("unused")
public class Player {
    private final String name;
    private final Colour colour;
    private ArrayList<Card> hand;
    private final ArrayList<Marble> marbles; // Home zone marbles
    private Card selectedCard;
    private final ArrayList<Marble> selectedMarbles; // Marbles selected for action
    protected Game game; // Already exists, ensure it's set

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
    
    // NEW METHOD TO SET GAME REFERENCE
    public void setGameReference(Game gameInstance) {
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
            if (this.game != null && playerHasActionableMarblesOnBoard(this.game.getBoard())) {
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

    // NEW HELPER METHOD: doesCardRequireMarbles
    private boolean doesCardRequireMarbles(Card card) {
        // This is a simplified check.
        // Cards like Ace, King, Queen, Ten can have 0-marble actions (field/discard)
        // OR 1-marble actions (move). If 0 marbles are selected for these,
        // validateMarbleSize will check if the 0-marble action is valid.
        // We are interested if the *primary intended action for an empty selection* would typically need a marble.
        String cardNameUpper = card.getName().toUpperCase();

        if (cardNameUpper.contains("ACE") || cardNameUpper.contains("KING") ||
            cardNameUpper.contains("QUEEN") || cardNameUpper.contains("TEN")) {
            // These have dual nature. If selectedMarbles is empty, validateMarbleSize
            // will determine if their 0-marble action (fielding/discarding) is valid.
            // So, for our check, if selectedMarbles is empty, we don't *strictly* require one *yet*.
            // The MarbleSelectionNeededException is for when the player *intends* a marble action
            // but forgets to click.
            // If validateMarbleSize later fails for 0 marbles (e.g. cannot field/discard),
            // it will throw InvalidMarbleException.
            return false; // Let validateMarbleSize handle 0-marble cases for these dual-nature cards.
        }

        // Cards that almost always require marbles if they are to do anything beyond being a "Standard" card.
        if (card instanceof Standard || // Includes numeric cards and special cards acting as standard
            cardNameUpper.contains("FOUR") ||
            cardNameUpper.contains("FIVE") ||
            cardNameUpper.contains("JACK") || // Requires 1 or 2
            cardNameUpper.contains("SEVEN") || // Requires 1 or 2
            cardNameUpper.contains("SAVER") ||
            cardNameUpper.contains("BURNER")) {
            return true;
        }
        return false; // Default, or for cards with no marble interaction.
    }

    // NEW HELPER METHOD: playerHasActionableMarblesOnBoard
    private boolean playerHasActionableMarblesOnBoard(Board board) {
        if (board == null) return false; // Safety check

        // Check track
        for (Cell cell : board.getTrack()) {
            if (cell.getMarble() != null && cell.getMarble().getColour() == this.colour) {
                // Further check: Is this marble actually movable? (not blocked, etc.)
                // For simplicity here, we'll just check if they have *any* marble on track.
                // A more sophisticated check would involve board.getActionableMarbles()
                // or trying to simulate a move.
                return true;
            }
        }
        // Check safe zone (marbles in safe zone can also be moved by some cards or within safe zone)
        for (SafeZone sz : board.getSafeZones()) {
            if (sz.getColour() == this.colour) {
                for (Cell safeCell : sz.getCells()) {
                    if (safeCell.getMarble() != null) { // Already implies it's this player's colour
                        return true;
                    }
                }
            }
        }
        return false; // No marbles of this player's colour found on track or in their safe zone.
    }

    
    public ArrayList<Marble> getSelectedMarblesPublic() { // Or just make selectedMarbles getter public
        return selectedMarbles;
    }

}
