package nullpomino.game.net;

import java.util.List;

/**
 * The single-player netplay mode list, indexed by game style. Replaces the
 * pre-existing {@code config/list/netlobby_singlemode.lst} text file: a colon
 * / comma-prefixed mini-format that listed the same data without any
 * validation against the registered mode roster.
 *
 * <p>Each entry's {@code name} must match the {@code getName()} string of a
 * registered local {@link nullpomino.game.mode.GameMode} — these
 * modes are played offline, with online ranking overlaid; this is enforced
 * by {@code NetSPModeRegistryTest}.
 */
public final class NetSPModeRegistry {

	public record Entry(String name, int rankingType, int maxGameType) {}

	private static final List<List<Entry>> BY_STYLE = List.of(
			// TETROMINO
			List.of(
					new Entry("MARATHON", 0, 2),
					new Entry("MARATHON+", 0, 1),
					new Entry("EXTREME", 0, 1),
					new Entry("LINE RACE", 1, 2),
					new Entry("SCORE RACE", 2, 2),
					new Entry("DIG RACE", 3, 2),
					new Entry("COMBO RACE", 5, 3),
					new Entry("ULTRA", 4, 4),
					new Entry("TECHNICIAN", 0, 4),
					new Entry("DIG CHALLENGE", 6, 1),
					new Entry("TIME ATTACK", 7, 10)),
			// AVALANCHE
			List.of(),
			// PHYSICIAN
			List.of(),
			// SPF
			List.of());

	private NetSPModeRegistry() {}

	public static List<Entry> forStyle(int gamestyle) {
		return BY_STYLE.get(gamestyle);
	}
}
