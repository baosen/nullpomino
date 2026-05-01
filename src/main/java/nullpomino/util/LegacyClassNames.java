package nullpomino.util;

/**
 * Translates fully-qualified class names read from external files (replays, user
 * rule/config overrides, lst-listed class paths) so files written against any of
 * these older package layouts still resolve:
 * <ol>
 *   <li>{@code mu.nu.nullpo.*} (pre-package-flatten)</li>
 *   <li>{@code nullpomino.game.subsystem.ai.*} (pre-AI-consolidation)</li>
 *   <li>{@code net.tetrisconcept.poochy.nullpomino.ai.*} (pre-AI-consolidation)</li>
 *   <li>{@code net.omegaboshi.nullpomino.game.subsystem.randomizer.*} (pre-randomizer-move)</li>
 * </ol>
 * All target the current layout {@code nullpomino.*}, with AI sub-packages
 * collapsing to {@code nullpomino.game.ai.*} and randomizers to
 * {@code nullpomino.game.randomizer.*}. The reverse {@link #toLegacy} mapping
 * keeps wire/file output readable by older builds.
 */
public final class LegacyClassNames {
	private static final String LEGACY_PREFIX = "mu.nu.nullpo.";
	private static final String CURRENT_PREFIX = "nullpomino.";
	private static final String LEGACY_AI_INTERNAL = "nullpomino.game.subsystem.ai.";
	private static final String LEGACY_AI_POOCHY = "net.tetrisconcept.poochy.nullpomino.ai.";
	private static final String CURRENT_AI_PREFIX = "nullpomino.game.ai.";
	private static final String LEGACY_RANDOMIZER = "net.omegaboshi.nullpomino.game.subsystem.randomizer.";
	private static final String CURRENT_RANDOMIZER = "nullpomino.game.randomizer.";

	private LegacyClassNames() {}

	public static String translate(String className) {
		if (className == null) return null;
		if (className.startsWith(LEGACY_RANDOMIZER)) {
			return CURRENT_RANDOMIZER + className.substring(LEGACY_RANDOMIZER.length());
		}
		if (className.startsWith(LEGACY_PREFIX)) {
			className = CURRENT_PREFIX + className.substring(LEGACY_PREFIX.length());
		}
		if (className.startsWith(LEGACY_AI_INTERNAL)) {
			return CURRENT_AI_PREFIX + className.substring(LEGACY_AI_INTERNAL.length());
		}
		if (className.startsWith(LEGACY_AI_POOCHY)) {
			return CURRENT_AI_PREFIX + className.substring(LEGACY_AI_POOCHY.length());
		}
		return className;
	}

	public static String toLegacy(String className) {
		if (className == null) return null;
		if (className.startsWith(CURRENT_RANDOMIZER)) {
			return LEGACY_RANDOMIZER + className.substring(CURRENT_RANDOMIZER.length());
		}
		if (className.startsWith(CURRENT_PREFIX)) {
			return LEGACY_PREFIX + className.substring(CURRENT_PREFIX.length());
		}
		return className;
	}
}
