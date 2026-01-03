package engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import engine.action.ActionResult;
import engine.action.AnimationStep;
import engine.board.Board;
import engine.board.SafeZone;
import exception.CannotDiscardException;
import exception.CannotFieldException;
import exception.GameException;
import exception.IllegalDestroyException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.SplitOutOfRangeException;
import model.Colour;
import model.card.Card;
import model.card.Deck;
import model.player.*;

/**
 * The main game engine that orchestrates all game logic for Jackaroo.
 * This class implements the GameManager interface and serves as the central
 * coordinator between the board, players, cards, and UI.
 * 
 * Responsibilities:
 *   Managing player turns and turn order
 *   Handling card selection and validation
 *   Coordinating marble movements and special actions
 *   Detecting win conditions
 *   Managing the deck and fire pit
 * 
 * Thread Safety: This class is NOT thread-safe. All methods
 * should be called from the JavaFX Application Thread.
 * 
 * Design Pattern: Implements the Facade pattern, providing a
 * simplified interface to the complex game subsystems.
 * 
 * @author Your Name
 * @version 2.0
 * @since 1.0
 */
public class Game implements GameManager {
    
    /** The game board containing the track, safe zones, and cells. */
    private final Board board;
    
    /** All players in the game (human + CPU opponents). */
    private final ArrayList<Player> players;
    
    /** Index of the player whose turn it currently is (0-3). */
    private int currentPlayerIndex;
    
    /** Cards that have been played and discarded this round. */
    private final ArrayList<Card> firePit;
    
    /** Turn order for players, determines board layout. */
    private ArrayList<Colour> playerColourOrder;
    
    /** If set, this player's turn will be skipped next. Used by Ten/Queen cards. */
    private Colour playerToSkip = null;

    // ==================== CONSTRUCTOR ====================
    
    /**
     * Constructs a new Jackaroo game with one human player and three CPU opponents.
     * 
     * Initialization sequence:
     *   Randomly assigns colors to all players
     *   Creates the game board with the color order
     *   Loads the card pool from Cards.csv
     *   Creates player instances (1 human, 3 CPU)
     *   Deals initial hands to all players
     *
     * 
     * @param playerName the name of the human player
     * @throws IOException if the Cards.csv file cannot be loaded
     * @throws NullPointerException if playerName is null
     */
    public Game(String playerName) throws IOException {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty");
        }
        
        currentPlayerIndex = 0;
        firePit = new ArrayList<>();
        
        // Randomly assign colors
        ArrayList<Colour> allColours = new ArrayList<>(Arrays.asList(Colour.values()));
        Collections.shuffle(allColours);
        this.playerColourOrder = new ArrayList<>(allColours.subList(0, 4));
        
        this.board = new Board(this.playerColourOrder, this);
        Deck.loadCardPool(this.board, this);
        
        this.players = new ArrayList<>();
        
        // Create human player
        Player humanPlayer = new Player(playerName, this.playerColourOrder.get(0));
        humanPlayer.setGameReference(this);
        this.players.add(humanPlayer);

        // Create CPU opponents
        for (int i = 1; i < 4; i++) {
            CPU cpuPlayer = new CPU("CPU " + i, this.playerColourOrder.get(i), this.board);
            cpuPlayer.setGameReference(this);
            this.players.add(cpuPlayer);
        }
        
        // Deal initial hands
        for (Player p : this.players) {
            p.setHand(Deck.drawCards());
        }
    }
    
    // ==================== GETTERS ====================
    
    /**
     * Returns the game board instance.
     * 
     * @return the game board, never null
     */
    public Board getBoard() { 
        return board; 
    }
    
    /**
     * Returns all players in the game.
     * The list is mutable but modifying it may break game state.
     * 
     * @return list of all players in turn order
     */
    public ArrayList<Player> getPlayers() { 
        return players; 
    }
    
    /**
     * Returns all cards currently in the fire pit (discard pile).
     * 
     * @return list of discarded cards, may be empty
     */
    public ArrayList<Card> getFirePit() { 
        return firePit; 
    }
    
    /**
     * Returns the index of the current player whose turn it is.
     * This is primarily for GUI updates.
     * 
     * @return player index in range [0, 3]
     */
    public int getCurrentPlayerIndexPublic() { 
        return currentPlayerIndex; 
    }

    // ==================== PLAYER ACTIONS ====================
    
    /**
     * Selects a card from the current player's hand.
     * This is a prerequisite for calling {playPlayerTurn()}.
     * 
     * Only the current player can select a card. Calling this method
     * for any other player will have no effect.
     * 
     * @param card the card to select from the current player's hand
     * @throws InvalidCardException if the card is not in the player's hand
     */
    public void selectCard(Card card) throws InvalidCardException {
        players.get(currentPlayerIndex).selectCard(card);
    }

    /**
     * Selects a marble for the current action.
     * Multiple marbles can be selected for cards like Jack (swap) or Seven (split).
     * 
     * @param marble the marble to select
     * @throws InvalidMarbleException if the marble is invalid or more than 2 marbles selected
     */
    public void selectMarble(Marble marble) throws InvalidMarbleException {
        players.get(currentPlayerIndex).selectMarble(marble);
    }

    /**
     * Deselects all currently selected cards and marbles for the current player.
     * This is typically called when the player wants to cancel their selection.
     */
    public void deselectAll() {
        players.get(currentPlayerIndex).deselectAll();
    }

    /**
     * Sets the split distance for a Seven card.
     * The split distance determines how many of the 7 steps go to the first marble,
     * with the remainder going to the second marble.
     * 
     * Example: If split is 3, first marble moves 3 steps, second moves 4.
     * 
     * @param splitDistance the distance for the first marble (1-6 inclusive)
     * @throws SplitOutOfRangeException if distance is not in valid range
     */
    public void editSplitDistance(int splitDistance) throws SplitOutOfRangeException {
        if (splitDistance < 1 || splitDistance > 6) {
            throw new SplitOutOfRangeException("Split must be between 1 and 6, got: " + splitDistance);
        }
        board.setSplitDistance(splitDistance);
    }

    /**
     * Executes the current player's turn based on their selected card and marbles.
     * 
     * Execution flow:
     *   Validates that a card is selected (for human players)
     *   Validates marble selection is appropriate for the card
     *   Executes the card's action
     *   Records all animations in the ActionResult
     * 
     * 
     * Important: This method does NOT end the turn. Call {#endPlayerTurn()}
     * after animations complete to advance to the next player.
     * 
     * @return an ActionResult containing all animations that should be played
     * @throws GameException if the action is invalid or cannot be executed
     * @see #endPlayerTurn()
     */
    public ActionResult playPlayerTurn() throws GameException {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        // Human players must explicitly select a card
        if (!(currentPlayer instanceof CPU) && currentPlayer.getSelectedCard() == null) {
            throw new InvalidCardException("Human player must select a card to play.");
        }
        
        ActionResult result = new ActionResult(currentPlayer, currentPlayer.getSelectedCard());
        currentPlayer.play(result);
        
        // Ensure the result has the card that was played
        if (result.getCardPlayed() == null && currentPlayer.getSelectedCard() != null) {
            result.setCardPlayed(currentPlayer.getSelectedCard());
        }
        
        return result;
    }
    
    /**
     * Marks a player to have their turn skipped on the next cycle.
     * Used by Ten and Queen cards.
     * 
     * @param colour the color of the player to skip
     */
    public void setPlayerToBeSkipped(Colour colour) {
        this.playerToSkip = colour;
    }

    /**
     * Ends the current player's turn and advances to the next player.
     * 
     * Turn end sequence:
     *   Discards the played card to the fire pit
     *   Clears all player selections
     *   Advances to next player (or skips if marked)
     *   Checks if all hands are empty
     *   Deals new round if needed
     * 
     * Deck Management: If all players' hands are empty, this method
     * automatically deals a new round. If the deck is empty, it refills from
     * the fire pit.
     */
    public void endPlayerTurn() {
        Player currentPlayerObject = players.get(currentPlayerIndex);
        Card selectedCard = currentPlayerObject.getSelectedCard();

        // Discard the played card
        if (selectedCard != null) {
            currentPlayerObject.getHand().remove(selectedCard);
            firePit.add(selectedCard);
        }
        
        currentPlayerObject.deselectAll();

        // Advance to next player, respecting skip flag
        int nextPlayerIndex = (currentPlayerIndex + 1) % players.size();
        
        if (this.playerToSkip != null && players.get(nextPlayerIndex).getColour() == this.playerToSkip) {
            this.playerToSkip = null; // Reset the flag
            currentPlayerIndex = (nextPlayerIndex + 1) % players.size(); // Skip this player
        } else {
            currentPlayerIndex = nextPlayerIndex;
        }

        // Check if all hands are empty to deal new round
        boolean allHandsAreEmpty = players.stream().allMatch(p -> p.getHand().isEmpty());

        if (allHandsAreEmpty) {
            // Ensure we have enough cards in the deck
            if (Deck.getPoolSize() < (players.size() * 4)) {
                if (!firePit.isEmpty()) {
                    Deck.refillPool(firePit);
                    firePit.clear();
                } else {
                    System.err.println("CRITICAL ERROR: Cannot start new round. Deck and Firepit are empty.");
                    return;
                }
            }
            
            // Deal new hands
            for (Player p : players) {
                p.setHand(Deck.drawCards());
            }
        }
    }
	 
    /**
     * Checks if any player has won the game.
     * A player wins when all 4 of their safe zone cells are filled.
     * 
     * @return the winning player's color, or null if no winner yet
     */
    public Colour checkWin() {
        for (SafeZone safeZone : board.getSafeZones()) {
            if (safeZone.isFull()) {
                return safeZone.getColour();
            }
        }
        return null;
    }

    // ==================== GAMEMANAGER INTERFACE ====================
    
    /**
     * 
     * This method is called when a marble is destroyed (captured or burned).
     * It removes the marble from the board and returns it to the owning player's
     * home area.
     * 
     * @param marble the marble to send home
     * @param result the action result for recording animations
     */
    @Override
    public void sendHome(Marble marble, ActionResult result) {
        for (Player player : players) {
            if (player.getColour() == marble.getColour()) {
                player.regainMarble(marble);
                break;
            }
        }
    }

    /**
     * 
     * Fields (places) a marble from the current player's home area onto
     * their base cell. If an opponent's marble is on the base, it is destroyed.
     * 
     * @param result the action result for recording animations
     * @throws CannotFieldException if no marbles available or own marble on base
     * @throws IllegalDestroyException if unable to destroy blocking marble
     */
    @Override
    public void fieldMarble(ActionResult result) throws CannotFieldException, IllegalDestroyException {
        Player currentPlayer = players.get(currentPlayerIndex);
        Marble marble = currentPlayer.getOneMarble();
        
        if (marble == null) {
            throw new CannotFieldException("No marbles left in the Home Zone to field.");
        }
        
        board.sendToBase(marble, result);
        currentPlayer.getMarbles().remove(marble);
    }
    
    /**
     * 
     * <p>Forces a player to discard the first card from their hand to the fire pit.
     * 
     * @param colour the color of the player to target
     * @param result the action result for recording the discard animation
     * @throws CannotDiscardException if player has no cards
     */
    @Override
    public void discardCard(Colour colour, ActionResult result) throws CannotDiscardException {
        Player targetPlayer = getPlayerByColour(colour);
        
        if (targetPlayer == null) {
            throw new CannotDiscardException("Target player does not exist.");
        }
        
        if (targetPlayer.getHand().isEmpty()) {
            throw new CannotDiscardException("Player " + targetPlayer.getName() + " has no cards to discard.");
        }
        
        Card discardedCard = targetPlayer.getHand().remove(0);
        this.firePit.add(discardedCard);
        result.addAnimation(new AnimationStep.Discard(targetPlayer, discardedCard));
    }

    /**
     * 
     * Discards a card from the next player (in turn order) and marks them to be skipped.
     * Used by the Ten card.
     * 
     * @param result the action result for recording animations
     * @throws CannotDiscardException if cannot target self or next player has no cards
     */
    @Override
    public void discardFromAndSkipNextPlayer(ActionResult result) throws CannotDiscardException {
        Player nextPlayer = players.get((currentPlayerIndex + 1) % players.size());
        
        if (nextPlayer.getColour() == getActivePlayerColour()) {
            throw new CannotDiscardException("Cannot use Ten card to skip yourself.");
        }
        
        discardCard(nextPlayer.getColour(), result);
        setPlayerToBeSkipped(nextPlayer.getColour());
    }
    
    /**
     * 
     * <p>Discards a card from a random opponent and marks them to be skipped.
     * Used by the Queen card.
     * 
     * @param result the action result for recording animations
     * @throws CannotDiscardException if no other players available
     */
    @Override
    public void discardFromAndSkipRandomPlayer(ActionResult result) throws CannotDiscardException {
        ArrayList<Player> otherPlayers = new ArrayList<>();
        
        for (Player p : players) {
            if (p.getColour() != getActivePlayerColour()) {
                otherPlayers.add(p);
            }
        }
        
        if (otherPlayers.isEmpty()) {
            throw new CannotDiscardException("No other players to target.");
        }
        
        Collections.shuffle(otherPlayers);
        Player targetPlayer = otherPlayers.get(0);

        discardCard(targetPlayer.getColour(), result);
        setPlayerToBeSkipped(targetPlayer.getColour());
    }

    /**
     * 
     * @return the color of the player whose turn it currently is
     */
    @Override
    public Colour getActivePlayerColour() { 
        return players.get(currentPlayerIndex).getColour(); 
    }
    
    /**
     * 
     * @return the color of the player who will go after the current player
     */
    @Override
    public Colour getNextPlayerColour() { 
        return players.get((currentPlayerIndex + 1) % players.size()).getColour(); 
    }
    
    /**
     * Finds a player by their color.
     * 
     * @param colourToFind the color to search for
     * @return the matching player, or null if not found
     */
    public Player getPlayerByColour(Colour colourToFind) {
        return players.stream()
            .filter(p -> p.getColour() == colourToFind)
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if the current player has any valid moves with their selected card.
     * 
     * This method systematically tests all possible marble combinations
     * (0 marbles, 1 marble, 2 marbles) to see if ANY action is valid.
     * 
     * Use case: Prevents players from getting stuck with unplayable cards.
     * The GUI can use this to show a friendly "No Valid Moves" message.
     * 
     * @return true if at least one valid move exists
     */
    public boolean hasAnyValidMoveForCurrentPlayer() {
        Player currentPlayer = players.get(currentPlayerIndex);
        Card selectedCard = currentPlayer.getSelectedCard();
        
        if (selectedCard == null) {
            return true; // No card selected yet, so technically valid
        }

        // Test 1: Try zero-marble actions (fielding, discard)
        if (selectedCard.validateMarbleSize(new ArrayList<>())) {
            try {
                selectedCard.validate(new ArrayList<>(), this);
                return true;
            } catch (GameException e) {
                // Invalid, continue testing
            }
        }

        // Test 2: Try all possible marble combinations
        ArrayList<Marble> actionableMarbles = board.getActionableMarbles();
        
        for (int i = 0; i < actionableMarbles.size(); i++) {
            // Single marble actions
            ArrayList<Marble> singleList = new ArrayList<>(Collections.singletonList(actionableMarbles.get(i)));
            if (selectedCard.validateMarbleSize(singleList) && selectedCard.validateMarbleColours(singleList)) {
                try {
                    selectedCard.validate(singleList, this);
                    return true;
                } catch (GameException e) {
                    // Invalid, continue
                }
            }
            
            // Double marble actions (Jack swap, Seven split)
            for (int j = i + 1; j < actionableMarbles.size(); j++) {
                ArrayList<Marble> doubleList = new ArrayList<>(Arrays.asList(actionableMarbles.get(i), actionableMarbles.get(j)));
                if (selectedCard.validateMarbleSize(doubleList) && selectedCard.validateMarbleColours(doubleList)) {
                    try {
                        selectedCard.validate(doubleList, this);
                        return true;
                    } catch (GameException e) {
                        // Invalid, continue
                    }
                }
            }
        }
        
        return false; // No valid moves found
    }
}