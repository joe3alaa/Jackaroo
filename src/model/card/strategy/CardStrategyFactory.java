package model.card.strategy;

/**
 * Factory class for creating card strategies. Centralizes strategy
 * instantiation logic.
 * 
 * This is a utility class with only static methods.
 */
public final class CardStrategyFactory {

	// Private constructor prevents instantiation
	private CardStrategyFactory() {
		throw new AssertionError("CardStrategyFactory is a utility class and cannot be instantiated");
	}

	/**
	 * Creates strategy for Ace card. Ace can EITHER field a marble OR move forward
	 * 1.
	 */
	public static CardActionStrategy createAceStrategy() {
		return new CompositeCardStrategy(new FieldingStrategy(), new StandardMoveStrategy(1));
	}

	/**
	 * Creates strategy for King card. King can EITHER field a marble OR move
	 * forward 13 (destroying path).
	 */
	public static CardActionStrategy createKingStrategy() {
		return new CompositeCardStrategy(new FieldingStrategy(), new KingMoveStrategy());
	}

	/**
	 * Creates strategy for Jack card. Jack can EITHER swap two marbles OR move
	 * forward 11.
	 */
	public static CardActionStrategy createJackStrategy() {
		return new CompositeCardStrategy(new SwapStrategy(), new StandardMoveStrategy(11));
	}

	/**
	 * Creates strategy for Seven card. Seven can EITHER split 7 steps between two
	 * marbles OR move one marble 7.
	 */
	public static CardActionStrategy createSevenStrategy() {
		return new CompositeCardStrategy(new SevenSplitStrategy(), new StandardMoveStrategy(7));
	}

	/**
	 * Creates strategy for Ten card. Ten can EITHER discard from next player and
	 * skip them OR move forward 10.
	 */
	public static CardActionStrategy createTenStrategy() {
		return new CompositeCardStrategy(new DiscardAndSkipStrategy(true), // true = skip NEXT player
				new StandardMoveStrategy(10));
	}

	/**
	 * Creates strategy for Queen card. Queen can EITHER discard from random player
	 * and skip them OR move forward 12.
	 */
	public static CardActionStrategy createQueenStrategy() {
		return new CompositeCardStrategy(new DiscardAndSkipStrategy(false), // false = skip RANDOM player
				new StandardMoveStrategy(12));
	}

	/**
	 * Creates strategy for Four card (backward move).
	 */
	public static CardActionStrategy createFourStrategy() {
		return new StandardMoveStrategy(-4);
	}

	/**
	 * Creates strategy for Burner card.
	 */
	public static CardActionStrategy createBurnerStrategy() {
		return new DestroyStrategy();
	}

	/**
	 * Creates strategy for Saver card.
	 */
	public static CardActionStrategy createSaverStrategy() {
		return new SaveStrategy();
	}

	/**
	 * Creates a standard forward move strategy. Used for cards 2, 3, 5, 6, 8, 9.
	 * 
	 * @param steps number of steps to move (must be positive)
	 */
	public static CardActionStrategy createStandardStrategy(int steps) {
		return new StandardMoveStrategy(steps);
	}
}