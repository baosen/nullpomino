package nullpomino.util;

/**
 * Translates fully-qualified class names read from external files (replays, user
 * rule/config overrides, lst-listed class paths) so files written against the
 * pre-flatten {@code mu.nu.nullpo.*} package layout still resolve after the
 * rename to {@code nullpomino.*}.
 */
public final class LegacyClassNames {
	private static final String LEGACY_PREFIX = "mu.nu.nullpo.";
	private static final String CURRENT_PREFIX = "nullpomino.";

	private LegacyClassNames() {}

	public static String translate(String className) {
		if (className == null) return null;
		if (className.startsWith(LEGACY_PREFIX)) {
			return CURRENT_PREFIX + className.substring(LEGACY_PREFIX.length());
		}
		return className;
	}

	public static String toLegacy(String className) {
		if (className == null) return null;
		if (className.startsWith(CURRENT_PREFIX)) {
			return LEGACY_PREFIX + className.substring(CURRENT_PREFIX.length());
		}
		return className;
	}
}
