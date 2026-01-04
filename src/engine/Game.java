// PASTE THIS ENTIRE CODE INTO your Game.java file - REFACTORED VERSION

package engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import engine.action.ActionResult;
import engine.board.Board;
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
 * The main game engine - NOW WITH FOCUSED RESPONSIBILITIES!
 * 
 * BEFORE: 520 lines, 7+ responsibilities
 * AFTER: ~280 lines, 3 focused responsibilities
 * 
 * Current Responsibilities (ONLY):
 * 1. Coordinating game initialization
 * 2. Handling player actions (select card/marble, play turn)
 * 3. Implementing the GameManager interface
 * 
 * Delegated Responsibilities:
 * - Turn management to TurnManager
 * - Deck management to DeckManager  
 * - Win checking to WinConditionChecker
 * 
 * Design Pattern: This is now a FACADE - it provides a simple interface
 * to a complex subsystem by delegating to specialists.
 * 
 * @author Your Name
 * @version 3.0 - Refactored for Single Responsibility Principle
 */
public class Game implements GameManager {
    
    // ==================== CORE GAME COMPONENTS ====================
    
    /** The game board containing track, safe zones, and cells. */
    private final Board board;
    
    /** All players in the game (human + CPU opponents). */
    private final ArrayList<Player> players;
    
    /** Turn order for players, determines board layout. */
    private ArrayList<Colour> playerColourOrder;
    
    // ==================== DELEGATED MANAGERS ====================
    
    /** Manages whose turn it is and skip logic. */
    private final TurnManager turnManager;
    
    /** Manages deck, fire pit, and dealing cards. */
    private final DeckManager deckManager;
    
    /** Checks for win conditions. */
    private final WinConditionChecker winChecker;

    // ==================== CONSTRUCTOR ====================
    
    /**
     * Constructs a new Jackaroo game.
     * 
     * REFACTORING NOTE: Constructor logic unchanged, but now creates
     * the three manager objects.
     * 
     * @param playerName the name of the human player
     * @throws IOException if Cards.csv cannot be loaded
     */
    public Game(String playerName) throws IOException {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty");
        }
        
        // Initialize managers FIRST
        this.turnManager = new TurnManager();
        this.deckManager = new DeckManager();
        
        // Randomly assign colors
        ArrayList<Colour> allColours = new ArrayList<>(Arrays.asList(Colour.values()));
        Collections.shuffle(allColours);
        this.playerColourOrder = new ArrayList<>(allColours.subList(0, 4));
        
        // Create board and load deck
        this.board = new Board(this.playerColourOrder, this);
        Deck.loadCardPool(this.board, this);
        
        // Initialize win checker AFTER board exists
        this.winChecker = new WinConditionChecker(this.board);
        
        // Create players
        this.players = new ArrayList<>();
        
        Player humanPlayer = new Player(playerName, this.playerColourOrder.get(0));
        humanPlayer.setGameReference(this);
        this.players.add(humanPlayer);

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
    
    public Board getBoard() { 
        return board; 
    }
    
    public ArrayList<Player> getPlayers() { 
        return players; 
    }
    
    /**
     * Gets the fire pit for UI display.
     * 
     * REFACTORING NOTE: Now delegates to DeckManager.
     */
    public ArrayList<Card> getFirePit() { 
        return deckManager.getFirePit();
    }
    
    /**
     * Gets current player index for UI.
     * 
     * REFACTORING NOTE: Now delegates to TurnManager.
     */
    public int getCurrentPlayerIndexPublic() { 
        return turnManager.getCurrentPlayerIndex();
    }
    
    /**
     * Helper to get the current player object.
     * Makes code more readable than players.get(turnManager.getCurrentPlayerIndex()).
     */
    private Player getCurrentPlayer() {
        return players.get(turnManager.getCurrentPlayerIndex());
    }

    // ==================== PLAYER ACTIONS ====================
    
    /**
     * Selects a card from the current player's hand.
     */
    public void selectCard(Card card) throws InvalidCardException {
        getCurrentPlayer().selectCard(card);
    }

    /**
     * Selects a marble for the current action.
     */
    public void selectMarble(Marble marble) throws InvalidMarbleException {
        getCurrentPlayer().selectMarble(marble);
    }

    /**
     * Deselects all currently selected cards and marbles.
     */
    public void deselectAll() {
        getCurrentPlayer().deselectAll();
    }

    /**
     * Sets the split distance for a Seven card.
     */
    public void editSplitDistance(int splitDistance) throws SplitOutOfRangeException {
        if (splitDistance < 1 || splitDistance > 6) {
            throw new SplitOutOfRangeException("Split must be between 1 and 6, got: " + splitDistance);
        }
        board.setSplitDistance(splitDistance);
    }

    /**
     * Executes the current player's turn.
     * 
     * REFACTORING NOTE: This method unchanged - it's a core responsibility of Game.
     */
    public ActionResult playPlayerTurn() throws GameException {
        Player currentPlayer = getCurrentPlayer();
        
        if (!(currentPlayer instanceof CPU) && currentPlayer.getSelectedCard() == null) {
            throw new InvalidCardException("Human player must select a card to play.");
        }
        
        ActionResult result = new ActionResult(currentPlayer, currentPlayer.getSelectedCard());
        currentPlayer.play(result);
        
        if (result.getCardPlayed() == null && currentPlayer.getSelectedCard() != null) {
            result.setCardPlayed(currentPlayer.getSelectedCard());
        }
        
        return result;
    }
    
    /**
     * Marks a player to be skipped.
     * 
     * REFACTORING NOTE: Now delegates to TurnManager.
     */
    public void setPlayerToBeSkipped(Colour colour) {
        turnManager.skipPlayer(colour);
    }

    /**
     * Ends the current player's turn and advances to the next.
     * 
     * REFACTORING NOTE: This is where the magic happens! Look how clean this is now.
     * 
     * BEFORE: 40+ lines of complex logic
     * AFTER: 15 lines of simple delegation
     */
    public void endPlayerTurn() {
        Player currentPlayer = getCurrentPlayer();
        Card selectedCard = currentPlayer.getSelectedCard();

        // Step 1: Discard the played card (delegate to DeckManager)
        if (selectedCard != null) {
            currentPlayer.getHand().remove(selectedCard);
            deckManager.discardCard(selectedCard);
        }
        
        currentPlayer.deselectAll();

        // Step 2: Advance to next player (delegate to TurnManager)
        turnManager.advanceToNextPlayer(players);

        // Step 3: Deal new round if needed (delegate to DeckManager)
        if (deckManager.needsNewRound(players)) {
            deckManager.dealNewRound(players);
        }
    }
	 
    /**
     * Checks if any player has won.
     * 
     * REFACTORING NOTE: Now delegates to WinConditionChecker.
     * 
     * BEFORE: 8 lines of logic in Game
     * AFTER: 1 line delegation
     */
    public Colour checkWin() {
        return winChecker.checkForWinner();
    }

    // ==================== GAMEMANAGER INTERFACE ====================
    
    @Override
    public void sendHome(Marble marble, ActionResult result) {
        for (Player player : players) {
            if (player.getColour() == marble.getColour()) {
                player.regainMarble(marble);
                break;
            }
        }
    }

    @Override
    public void fieldMarble(ActionResult result) throws CannotFieldException, IllegalDestroyException {
        Player currentPlayer = getCurrentPlayer();
        Marble marble = currentPlayer.getOneMarble();
        
        if (marble == null) {
            throw new CannotFieldException("No marbles left in the Home Zone to field.");
        }
        
        board.sendToBase(marble, result);
        currentPlayer.getMarbles().remove(marble);
    }
    
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
        deckManager.discardCard(discardedCard); // <<< Delegate to DeckManager
        result.addAnimation(new engine.action.AnimationStep.Discard(targetPlayer, discardedCard));
    }

    @Override
    public void discardFromAndSkipNextPlayer(ActionResult result) throws CannotDiscardException {
        Player nextPlayer = players.get(turnManager.peekNextPlayerIndex(players.size())); // <<< Use TurnManager
        
        if (nextPlayer.getColour() == getActivePlayerColour()) {
            throw new CannotDiscardException("Cannot use Ten card to skip yourself.");
        }
        
        discardCard(nextPlayer.getColour(), result);
        turnManager.skipPlayer(nextPlayer.getColour()); // <<< Delegate to TurnManager
    }
    
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
        turnManager.skipPlayer(targetPlayer.getColour()); // <<< Delegate to TurnManager
    }

    @Override
    public Colour getActivePlayerColour() { 
        return getCurrentPlayer().getColour();
    }
    
    @Override
    public Colour getNextPlayerColour() { 
        int nextIndex = turnManager.peekNextPlayerIndex(players.size()); // <<< Use TurnManager
        return players.get(nextIndex).getColour();
    }
    
    /**
     * Finds a player by their color.
     */
    public Player getPlayerByColour(Colour colourToFind) {
        return players.stream()
            .filter(p -> p.getColour() == colourToFind)
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if the current player has any valid moves.
     * 
     * REFACTORING NOTE: This method unchanged - it's still complex but
     * it's a core responsibility of Game (validating player actions).
     */
    public boolean hasAnyValidMoveForCurrentPlayer() {
        Player currentPlayer = getCurrentPlayer();
        Card selectedCard = currentPlayer.getSelectedCard();
        
        if (selectedCard == null) {
            return true;
        }

        // Test zero-marble actions
        if (selectedCard.validateMarbleSize(new ArrayList<>())) {
            try {
                selectedCard.validate(new ArrayList<>(), this);
                return true;
            } catch (GameException e) {
                // Invalid, continue testing
            }
        }

        // Test marble combinations
        ArrayList<Marble> actionableMarbles = board.getActionableMarbles();
        
        for (int i = 0; i < actionableMarbles.size(); i++) {
            // Single marble
            ArrayList<Marble> singleList = new ArrayList<>(Collections.singletonList(actionableMarbles.get(i)));
            if (selectedCard.validateMarbleSize(singleList) && selectedCard.validateMarbleColours(singleList)) {
                try {
                    selectedCard.validate(singleList, this);
                    return true;
                } catch (GameException e) {
                    // Invalid, continue
                }
            }
            
            // Double marble
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
        
        return false;
    }
}