package nullpomino.util;

import java.util.List;

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
