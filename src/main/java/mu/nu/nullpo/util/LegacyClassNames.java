package mu.nu.nullpo.util;

/**
 * Translates fully-qualified class names read from external files (replays, user
 * rule/config overrides, lst-listed class paths) so that files written against
 * pre-rename package layouts still resolve. Currently an identity no-op; a
 * follow-up commit will flatten {@code mu.nu.nullpo.*} to {@code nullpomino.*}.
 */
public final class LegacyClassNames {
	private LegacyClassNames() {}

	public static String translate(String className) {
		return className;
	}
}
