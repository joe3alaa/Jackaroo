// CREATE THIS FILE: src/engine/DeckManager.java

package engine;

import model.card.Card;
import model.card.Deck;
import model.player.Player;
import java.util.ArrayList;

/**
 * Manages the card deck, fire pit (discard pile), and dealing.
 * 
 * RESPONSIBILITY: Knows about card distribution and recycling.
 * 
 * Design Decision: This class OWNS a Deck instance and coordinates
 * between it and the fire pit.  Each Game gets its own DeckManager
 * with its own Deck — no static state is shared between games.
 * 
 * @author Your Name
 * @version 3.0
 */
public class DeckManager {
    
    // ==================== STATE ====================
    
    /** 
     * The fire pit (discard pile) where played cards go.
     * When the main deck runs out, these cards are shuffled back in.
     */
    private final ArrayList<Card> firePit;
    
    /**
     * The deck instance that owns the card pool for this game.
     * Previously Deck was a static singleton — every DeckManager (and every
     * Game) shared the same card pool.  Now each DeckManager owns its own
     * Deck, so two games can coexist without corrupting each other's state.
     */
    private final Deck deck;
    
    /**
     * Number of cards each player receives per round.
     * This is a game constant but stored here for clarity.
     */
    private static final int CARDS_PER_HAND = 4;
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Creates a new DeckManager that owns the given Deck.
     *
     * @param deck the Deck instance for this game (must not be null)
     */
    public DeckManager(Deck deck) {
        if (deck == null) {
            throw new IllegalArgumentException("Deck cannot be null");
        }
        this.firePit = new ArrayList<>();
        this.deck = deck;
    }
    
    // ==================== PUBLIC METHODS ====================

    /**
     * Draws a hand of cards from the deck.
     * Delegates to the owned Deck instance.
     *
     * @return a new hand of cards
     */
    public ArrayList<Card> drawCards() {
        return deck.drawCards();
    }
    
    /**
     * Discards a card to the fire pit.
     * 
     * This is called when:
     * - A player plays a card (end of turn)
     * - A card is forcibly discarded (Ten/Queen effects)
     * 
     * @param card The card to discard (can be null, will be ignored)
     */
    public void discardCard(Card card) {
        if (card != null) {
            firePit.add(card);
        }
    }
    
    /**
     * Checks if all players have empty hands.
     * This signals that a new round needs to be dealt.
     * 
     * Why separate this? Because it makes the calling code more readable:
     *   if (deckManager.needsNewRound(players)) {
     *       deckManager.dealNewRound(players);
     *   }
     * 
     * @param players List of all players
     * @return true if all hands are empty
     */
    public boolean needsNewRound(ArrayList<Player> players) {
        if (players == null || players.isEmpty()) {
            return false;
        }
        
        // Use Java 8 streams for elegant checking
        return players.stream().allMatch(p -> p.getHand().isEmpty());
    }
    
    /**
     * Deals a new round of cards to all players.
     * 
     * Process:
     * 1. Check if deck has enough cards
     * 2. If not, refill from fire pit
     * 3. Deal CARDS_PER_HAND to each player
     * 
     * Critical Error Handling: If BOTH deck and fire pit are empty,
     * we cannot deal. This should never happen in normal play, but
     * we handle it gracefully.
     * 
     * @param players List of all players to deal to
     * @throws IllegalStateException if cannot deal (deck and fire pit empty)
     */
    public void dealNewRound(ArrayList<Player> players) {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Cannot deal to no players");
        }
        
        int cardsNeeded = players.size() * CARDS_PER_HAND;
        
        // Step 1: Check if we need to refill the deck
        if (deck.getPoolSize() < cardsNeeded) {
            refillDeckFromFirePit();
        }
        
        // Step 2: Verify we now have enough cards
        if (deck.getPoolSize() < cardsNeeded) {
            throw new IllegalStateException(
                "CRITICAL ERROR: Cannot deal new round. " +
                "Deck size: " + deck.getPoolSize() + 
                ", Fire pit size: " + firePit.size() + 
                ", Cards needed: " + cardsNeeded
            );
        }
        
        // Step 3: Deal to each player
        for (Player player : players) {
            player.setHand(deck.drawCards());
        }
    }
    
    /**
     * Gets the current fire pit for UI display.
     * Returns a reference to the actual list (not a copy) for performance.
     * 
     * Warning: The caller should NOT modify this list directly.
     * Use discardCard() instead.
     * 
     * @return The fire pit list
     */
    public ArrayList<Card> getFirePit() {
        return firePit;
    }
    
    /**
     * Gets the number of cards currently in the fire pit.
     * Useful for debugging and UI display.
     * 
     * @return Fire pit size
     */
    public int getFirePitSize() {
        return firePit.size();
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    /**
     * Refills the main deck by recycling cards from the fire pit.
     * 
     * This is a CRITICAL operation that prevents the game from getting stuck.
     * 
     * Process:
     * 1. Take all cards from fire pit
     * 2. Add them back to the deck pool
     * 3. Clear the fire pit
     * 
     * Note: Deck.refillPool() handles shuffling automatically.
     */
    private void refillDeckFromFirePit() {
        if (firePit.isEmpty()) {
            System.err.println(
                "WARNING: Attempted to refill deck from empty fire pit. " +
                "This should not happen in normal gameplay."
            );
            return;
        }
        
        // Transfer cards from fire pit back to deck
        deck.refillPool(new ArrayList<>(firePit)); // Pass a copy to be safe
        firePit.clear();
        
        System.out.println(
            "INFO: Deck refilled from fire pit. " +
            "New deck size: " + deck.getPoolSize()
        );
    }
}