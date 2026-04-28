package nullpomino.util;

import java.util.List;

import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagBonusBagRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagBonusRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagMinusRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagMinusTwoRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagNoSZORandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.DoubleBagRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.ExpDistWeightRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.FixedSequenceRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.GameBoyRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.History4RollsRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.History6RollsRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.LinearDistWeightRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.NineBagRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.NintendoRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.QuadraticDistWeightRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.StrictHistoryRandomizer;

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
		return RANDOMIZERS.stream().map(Class::getName).toList();
	}
}
