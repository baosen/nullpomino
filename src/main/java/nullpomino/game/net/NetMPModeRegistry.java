package nullpomino.game.net;

import java.util.List;

/**
 * The multiplayer netplay mode list, indexed by game style. Replaces the
 * pre-existing {@code config/list/netlobby_multimode.lst} text file: a colon
 * / comma-prefixed mini-format that listed the same data without any
 * validation against the registered mode roster.
 *
 * <p>Each entry's {@code name} must match the {@code getName()} string of a
 * registered netplay {@link nullpomino.game.mode.GameMode} — i.e.
 * one whose {@code isNetplayMode()} is true; this is enforced by
 * {@code NetMPModeRegistryTest}.
 */
public final class NetMPModeRegistry {

	public record Entry(String name, boolean isRace) {}

	private static final List<List<Entry>> BY_STYLE = List.of(
			// TETROMINO
			List.of(
					new Entry("NET-VS-BATTLE", false),
					new Entry("NET-VS-LINE RACE", true),
					new Entry("NET-VS-DIG RACE", true)),
			// AVALANCHE
			List.of(),
			// PHYSICIAN
			List.of(),
			// SPF
			List.of());

	private NetMPModeRegistry() {}

	public static List<Entry> forStyle(int gamestyle) {
		return BY_STYLE.get(gamestyle);
	}
}
