package nullpomino.gui.sdl;

import java.util.List;

/**
 * The folder layout shown on the Mode-Select screen. Replaces the
 * pre-existing {@code config/list/modefolder.lst} text file: a colon /
 * comment-prefixed mini-format with no validation that referenced names
 * matched any registered mode.
 *
 * <p>Mode names here must match the {@code getName()} string of a registered
 * {@link nullpomino.game.subsystem.mode.GameMode}; this is enforced by
 * {@code ModeFolderRegistryTest}.
 *
 * <p>The synthetic {@code [MORE...]} (in {@link StateSelectModeSDL}) and
 * {@code [ALL MODES]} (in {@link StateSelectModeFolderSDL}) entries are UI
 * affordances and live in the screen code, not here.
 */
public final class ModeFolderRegistry {

	public record Folder(String name, List<String> modes) {}

	/** Top-level mode list shown immediately under PLAY, in cursor order. */
	public static final List<String> TOP_LEVEL = List.of(
			"MARATHON",
			"LINE RACE",
			"DIG RACE",
			"DIG CHALLENGE",
			"ULTRA",
			"GRADE MANIA 3",
			"SPEED MANIA 2",
			"PRACTICE",
			"GEM MANIA",
			"VS-BATTLE");

	/** Folders shown after picking [MORE...], in cursor order. */
	public static final List<Folder> FOLDERS = List.of(
			new Folder("STANDARD MODES", List.of(
					"MARATHON",
					"MARATHON+",
					"EXTREME",
					"TECHNICIAN")),
			new Folder("RACE MODES", List.of(
					"LINE RACE",
					"SCORE RACE",
					"DIG RACE",
					"COMBO RACE",
					"ULTRA")),
			new Folder("MANIA MODES", List.of(
					"GRADE MANIA",
					"GRADE MANIA 2",
					"GRADE MANIA 3",
					"SCORE ATTACK",
					"SPEED MANIA",
					"SPEED MANIA 2",
					"GARBAGE MANIA",
					"PHANTOM MANIA",
					"FINAL",
					"TIME ATTACK",
					"GEM MANIA")),
			new Folder("RETRO MODES", List.of(
					"RETRO MARATHON",
					"RETRO MASTERY",
					"RETRO MANIA")),
			new Folder("MISC MODES", List.of(
					"SQUARE",
					"DIG CHALLENGE",
					"VS-LINE RACE",
					"VS-DIG RACE",
					"VS-BATTLE",
					"TOOL-VS MAP EDIT")),
			new Folder("AVALANCHE", List.of(
					"AVALANCHE 1P (RC2)",
					"AVALANCHE 1P FEVER MARATHON (RC2)",
					"AVALANCHE VS-BATTLE (RC1)",
					"AVALANCHE VS FEVER MARATHON (RC1)",
					"AVALANCHE VS DIG RACE (RC1)",
					"AVALANCHE VS BOMB BATTLE (RC1)",
					"AVALANCHE-SPF VS-BATTLE (BETA)")),
			new Folder("PHYSICIAN", List.of(
					"PHYSICIAN (RC1)",
					"PHYSICIAN VS-BATTLE (RC1)")),
			new Folder("SPF", List.of(
					"SPF VS-BATTLE (BETA)")));

	private ModeFolderRegistry() {}
}
