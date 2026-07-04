package nullpomino.util;

import java.util.List;
import java.util.function.Supplier;

import nullpomino.game.wallkick.AvalancheClassicWallkick;
import nullpomino.game.wallkick.AvalancheWallkick;
import nullpomino.game.wallkick.ClassicPlusWallkick;
import nullpomino.game.wallkick.ClassicWallkick;
import nullpomino.game.wallkick.DTETWallkick;
import nullpomino.game.wallkick.GBCWallkick;
import nullpomino.game.wallkick.PhysicianWallkick;
import nullpomino.game.wallkick.StandardMild180Wallkick;
import nullpomino.game.wallkick.StandardSymmetricMild180Wallkick;
import nullpomino.game.wallkick.StandardSymmetricWallkick;
import nullpomino.game.wallkick.StandardWallkick;
import nullpomino.game.wallkick.WallOnlyWallkick;
import nullpomino.game.wallkick.Wallkick;

/**
 * Compile-checked wallkick roster for tools that present built-in choices.
 */
public final class WallkickRegistry {
	private static final List<Class<? extends Wallkick>> WALLKICKS = List.of(
			StandardWallkick.class,
			StandardMild180Wallkick.class,
			StandardSymmetricWallkick.class,
			StandardSymmetricMild180Wallkick.class,
			ClassicWallkick.class,
			ClassicPlusWallkick.class,
			DTETWallkick.class,
			GBCWallkick.class,
			WallOnlyWallkick.class,
			AvalancheWallkick.class,
			AvalancheClassicWallkick.class,
			PhysicianWallkick.class);

	// Reflection-free constructors in the same order as WALLKICKS, for the
	// browser (TeaVM) target's ClassFactory registry. Kept in lockstep with
	// WALLKICKS by WallkickRegistryTest.
	private static final List<Supplier<? extends Wallkick>> SUPPLIERS = List.of(
			StandardWallkick::new,
			StandardMild180Wallkick::new,
			StandardSymmetricWallkick::new,
			StandardSymmetricMild180Wallkick::new,
			ClassicWallkick::new,
			ClassicPlusWallkick::new,
			DTETWallkick::new,
			GBCWallkick::new,
			WallOnlyWallkick::new,
			AvalancheWallkick::new,
			AvalancheClassicWallkick::new,
			PhysicianWallkick::new);

	private WallkickRegistry() {}

	public static List<Class<? extends Wallkick>> all() {
		return WALLKICKS;
	}

	/** Constructor suppliers, aligned by index with {@link #all()}. */
	public static List<Supplier<? extends Wallkick>> suppliers() {
		return SUPPLIERS;
	}

	public static List<String> classNames() {
		return WALLKICKS.stream()
				.map(Class::getName)
				.map(LegacyClassNames::toLegacy)
				.toList();
	}
}
