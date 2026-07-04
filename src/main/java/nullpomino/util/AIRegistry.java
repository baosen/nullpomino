package nullpomino.util;

import java.util.List;
import java.util.function.Supplier;

import nullpomino.game.ai.BasicAI;
import nullpomino.game.ai.ComboRaceBot;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.ai.Nohoho;
import nullpomino.game.ai.PoochyBot;
import nullpomino.game.ai.PoochyBotDefensive;
import nullpomino.game.ai.RanksAI;
import nullpomino.game.ai.TSpinAI;

/**
 * Compile-checked AI roster. Replaces the {@code config/list/ai.lst} text file
 * (which listed the same classes by FQN string, loaded reflectively): entries
 * here are direct class references so the compiler catches renames, and the
 * constructor suppliers let the browser (TeaVM) target build AIs without
 * {@code Class.forName}.
 */
public final class AIRegistry {
	private static final List<Class<? extends DummyAI>> AIS = List.of(
			BasicAI.class,
			TSpinAI.class,
			PoochyBot.class,
			PoochyBotDefensive.class,
			RanksAI.class,
			Nohoho.class,
			ComboRaceBot.class);

	// Constructor suppliers in the same order as AIS. Kept in lockstep by
	// AIRegistryTest.
	private static final List<Supplier<? extends DummyAI>> SUPPLIERS = List.of(
			BasicAI::new,
			TSpinAI::new,
			PoochyBot::new,
			PoochyBotDefensive::new,
			RanksAI::new,
			Nohoho::new,
			ComboRaceBot::new);

	private AIRegistry() {}

	public static List<Class<? extends DummyAI>> all() {
		return AIS;
	}

	/** Constructor suppliers, aligned by index with {@link #all()}. */
	public static List<Supplier<? extends DummyAI>> suppliers() {
		return SUPPLIERS;
	}
}
