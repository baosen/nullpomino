package nullpomino.util;

import java.util.List;
import java.util.function.Supplier;

import nullpomino.game.ai.BasicAI;
import nullpomino.game.ai.ComboRaceBot;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.ai.Nohoho;
import nullpomino.game.ai.PoochyBot;
import nullpomino.game.ai.PoochyBotDefensive;
import nullpomino.game.ai.TSpinAI;

/**
 * Compile-checked roster of the browser-compatible AIs, feeding the TeaVM
 * target's reflection-free {@link ClassFactory} registry via
 * {@link FactoryDefaults}. Entries are direct class references so the compiler
 * catches renames.
 *
 * <p>{@code RanksAI} is deliberately absent: it deserializes a rank table with
 * {@code java.io.ObjectInputStream}, which TeaVM's classlib does not provide,
 * and referencing it here would drag that unsupported class into the web
 * build's reachable graph. The desktop build still loads RanksAI reflectively
 * from {@code config/list/ai.lst}.
 */
public final class AIRegistry {
	private static final List<Class<? extends DummyAI>> AIS = List.of(
			BasicAI.class,
			TSpinAI.class,
			PoochyBot.class,
			PoochyBotDefensive.class,
			Nohoho.class,
			ComboRaceBot.class);

	// Constructor suppliers in the same order as AIS. Kept in lockstep by
	// AIRegistryTest.
	private static final List<Supplier<? extends DummyAI>> SUPPLIERS = List.of(
			BasicAI::new,
			TSpinAI::new,
			PoochyBot::new,
			PoochyBotDefensive::new,
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
