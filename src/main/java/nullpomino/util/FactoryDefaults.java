package nullpomino.util;

import java.util.List;
import java.util.function.Supplier;

/**
 * Populates {@link ClassFactory}'s registry with reflection-free constructors
 * for every built-in wallkick, randomizer, and AI. Called once at startup by
 * the browser (TeaVM) entry point, where {@code Class.forName} does not work.
 * The desktop build never calls this and continues to resolve these classes
 * reflectively.
 */
public final class FactoryDefaults {
	private FactoryDefaults() {}

	public static void installAll() {
		register(WallkickRegistry.all(), WallkickRegistry.suppliers());
		register(RandomizerRegistry.all(), RandomizerRegistry.suppliers());
		register(AIRegistry.all(), AIRegistry.suppliers());
	}

	private static void register(List<? extends Class<?>> classes, List<? extends Supplier<?>> suppliers) {
		for (int i = 0; i < classes.size(); i++) {
			ClassFactory.register(classes.get(i).getName(), suppliers.get(i));
		}
	}
}
