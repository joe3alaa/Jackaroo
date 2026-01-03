// CREATE THIS FILE at: src/engine/action/ActionResult.java

package engine.action;

import model.card.Card;
import model.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the complete result of a single player's turn.
 * It contains who played, what card they played, and a comprehensive list of
 * all visual animations that need to be performed by the GUI.
 * <p>
 * This object is created by the Game engine and passed to the GUI.
 */
public class ActionResult {

    private final Player player;
    private Card cardPlayed;
    private final ArrayList<AnimationStep> animationSteps = new ArrayList<>();

    /**
     * Constructs a new result for a player's turn.
     *
     * @param player     The player who took the turn.
     * @param cardPlayed The card that was used for the action.
     */
    public ActionResult(Player player, Card cardPlayed) {
        this.player = player;
        this.cardPlayed = cardPlayed;
    }

    /**
     * Adds a new animation step to the result.
     * The order of addition can be important for the GUI to decide how to play animations
     * (e.g., sequentially or in parallel).
     *
     * @param step The AnimationStep to add.
     */
    public void addAnimation(AnimationStep step) {
        animationSteps.add(step);
    }

    public Player getPlayer() {
        return player;
    }

    public Card getCardPlayed() {
        return cardPlayed;
    }
    
    public void setCardPlayed(Card cardPlayed) {
        this.cardPlayed = cardPlayed;
    }

    /**
     * Returns an unmodifiable view of the list of animation steps.
     * This prevents the GUI from accidentally modifying the results.
     *
     * @return A read-only list of animation steps.
     */
    public List<AnimationStep> getAnimationSteps() {
        return Collections.unmodifiableList(animationSteps);
    }
}