package nullpomino.util;

import java.util.List;

import nullpomino.game.subsystem.wallkick.AvalancheClassicWallkick;
import nullpomino.game.subsystem.wallkick.AvalancheWallkick;
import nullpomino.game.subsystem.wallkick.ClassicPlusWallkick;
import nullpomino.game.subsystem.wallkick.ClassicWallkick;
import nullpomino.game.subsystem.wallkick.DTETWallkick;
import nullpomino.game.subsystem.wallkick.GBCWallkick;
import nullpomino.game.subsystem.wallkick.PhysicianWallkick;
import nullpomino.game.subsystem.wallkick.StandardMild180Wallkick;
import nullpomino.game.subsystem.wallkick.StandardSymmetricMild180Wallkick;
import nullpomino.game.subsystem.wallkick.StandardSymmetricWallkick;
import nullpomino.game.subsystem.wallkick.StandardWallkick;
import nullpomino.game.subsystem.wallkick.WallOnlyWallkick;
import nullpomino.game.subsystem.wallkick.Wallkick;

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

	private WallkickRegistry() {}

	public static List<Class<? extends Wallkick>> all() {
		return WALLKICKS;
	}

	public static List<String> classNames() {
		return WALLKICKS.stream()
				.map(Class::getName)
				.map(LegacyClassNames::toLegacy)
				.toList();
	}
}
