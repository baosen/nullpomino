package nullpomino.util;

import java.util.List;

import nullpomino.game.randomizer.BagBonusBagRandomizer;
import nullpomino.game.randomizer.BagBonusRandomizer;
import nullpomino.game.randomizer.BagMinusRandomizer;
import nullpomino.game.randomizer.BagMinusTwoRandomizer;
import nullpomino.game.randomizer.BagNoSZORandomizer;
import nullpomino.game.randomizer.BagRandomizer;
import nullpomino.game.randomizer.DoubleBagRandomizer;
import nullpomino.game.randomizer.ExpDistWeightRandomizer;
import nullpomino.game.randomizer.FixedSequenceRandomizer;
import nullpomino.game.randomizer.GameBoyRandomizer;
import nullpomino.game.randomizer.History4RollsRandomizer;
import nullpomino.game.randomizer.History6RollsRandomizer;
import nullpomino.game.randomizer.LinearDistWeightRandomizer;
import nullpomino.game.randomizer.MemorylessRandomizer;
import nullpomino.game.randomizer.NineBagRandomizer;
import nullpomino.game.randomizer.NintendoRandomizer;
import nullpomino.game.randomizer.QuadraticDistWeightRandomizer;
import nullpomino.game.randomizer.Randomizer;
import nullpomino.game.randomizer.StrictHistoryRandomizer;

/**
 * Compile-checked randomizer roster for tools that present built-in choices.
 */
public final class RandomizerRegistry {
	private static final List<Class<? extends Randomizer>> RANDOMIZERS = List.of(
			MemorylessRandomizer.class,
			BagRandomizer.class,
			BagNoSZORandomizer.class,
			BagBonusRandomizer.class,
			BagBonusBagRandomizer.class,
			DoubleBagRandomizer.class,
			NineBagRandomizer.class,
			BagMinusRandomizer.class,
			BagMinusTwoRandomizer.class,
			History4RollsRandomizer.class,
			History6RollsRandomizer.class,
			StrictHistoryRandomizer.class,
			NintendoRandomizer.class,
			GameBoyRandomizer.class,
			LinearDistWeightRandomizer.class,
			QuadraticDistWeightRandomizer.class,
			ExpDistWeightRandomizer.class,
			FixedSequenceRandomizer.class);

	private RandomizerRegistry() {}

	public static List<Class<? extends Randomizer>> all() {
		return RANDOMIZERS;
	}

	public static List<String> classNames() {
		return RANDOMIZERS.stream()
				.map(Class::getName)
				.map(LegacyClassNames::toLegacy)
				.toList();
	}
}
