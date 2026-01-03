// REPLACE THIS FILE: src/engine/action/AnimationStep.java

package engine.action;

import engine.board.Cell;
import model.card.Card;
import model.player.Marble;
import model.player.Player;

import java.util.ArrayList;

/**
 * An interface representing a single, distinct visual action that should
 * be animated by the GUI. A single player turn (represented by an ActionResult)
 * can consist of multiple AnimationSteps (e.g., a move and a destroy).
 * <p>
 * This is the "receipt" telling the GUI exactly what happened, so it doesn't have to guess.
 * This version is compatible with Java 8/11.
 */
public interface AnimationStep {

    /**
     * Represents a marble moving along a path on the board.
     * This is used for standard moves, Four, Five, Seven, etc.
     */
    final class Move implements AnimationStep {
        public final Marble marble;
        public final ArrayList<Cell> path;

        public Move(Marble marble, ArrayList<Cell> path) {
            this.marble = marble;
            this.path = path;
        }
    }

    /**
     * Represents a marble being fielded from the player's home area to their base cell.
     */
    final class Field implements AnimationStep {
        public final Marble fieldedMarble;
        public final Cell toBaseCell;

        public Field(Marble fieldedMarble, Cell toBaseCell) {
            this.fieldedMarble = fieldedMarble;
            this.toBaseCell = toBaseCell;
        }
    }

    /**
     * Represents two marbles swapping positions.
     */
    final class Swap implements AnimationStep {
        public final Marble marble1;
        public final Cell start1;
        public final Cell end1;
        public final Marble marble2;
        public final Cell start2;
        public final Cell end2;

        public Swap(Marble marble1, Cell start1, Cell end1, Marble marble2, Cell start2, Cell end2) {
            this.marble1 = marble1;
            this.start1 = start1;
            this.end1 = end1;
            this.marble2 = marble2;
            this.start2 = start2;
            this.end2 = end2;
        }
    }

    /**
     * Represents a marble being moved from the main track into its safe zone.
     */
    final class Save implements AnimationStep {
        public final Marble savedMarble;
        public final Cell fromCell;
        public final Cell toSafeCell;

        public Save(Marble savedMarble, Cell fromCell, Cell toSafeCell) {
            this.savedMarble = savedMarble;
            this.fromCell = fromCell;
            this.toSafeCell = toSafeCell;
        }
    }

    /**
     * Represents a marble being destroyed (sent back to its owner's home).
     */
    final class Destroy implements AnimationStep {
        public final Marble destroyedMarble;
        public final Cell fromCell;

        public Destroy(Marble destroyedMarble, Cell fromCell) {
            this.destroyedMarble = destroyedMarble;
            this.fromCell = fromCell;
        }
    }

    /**
     * Represents a card being forcibly discarded from an opponent's hand.
     */
    final class Discard implements AnimationStep {
        public final Player targetPlayer;
        public final Card discardedCard;

        public Discard(Player targetPlayer, Card discardedCard) {
            this.targetPlayer = targetPlayer;
            this.discardedCard = discardedCard;
        }
    }
}