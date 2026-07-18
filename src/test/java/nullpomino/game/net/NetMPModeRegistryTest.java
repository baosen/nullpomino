package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nullpomino.game.net.NetMPModeRegistry.Entry;
import nullpomino.game.play.GameEngine;
import nullpomino.game.mode.GameMode;
import nullpomino.util.ModeManager;
import nullpomino.util.ModeRegistry;

import org.junit.jupiter.api.Test;

/**
 * Pins the multiplayer netplay mode list against the registered mode roster.
 * Catches typos in {@link NetMPModeRegistry} and silent renames in
 * {@code NetVS*Mode} classes that would otherwise produce unreachable
 * matchmaking dropdown entries.
 */
class NetMPModeRegistryTest {

	@Test
	void tetrominoEntriesMatchSpec() {
		assertEquals(List.of(
				new Entry("NET-VS-BATTLE", false),
				new Entry("NET-VS-LINE RACE", true),
				new Entry("NET-VS-DIG RACE", true)),
				NetMPModeRegistry.forStyle(GameEngine.GAMESTYLE_TETROMINO),
				"tetromino MP entries are wire-visible (isRace drives ranking-table "
						+ "shape in NetServer.loadMPRankingList)");
	}

	@Test
	void nonTetrominoStylesAreEmpty() {
		assertTrue(NetMPModeRegistry.forStyle(GameEngine.GAMESTYLE_AVALANCHE).isEmpty());
		assertTrue(NetMPModeRegistry.forStyle(GameEngine.GAMESTYLE_PHYSICIAN).isEmpty());
		assertTrue(NetMPModeRegistry.forStyle(GameEngine.GAMESTYLE_SPF).isEmpty());
	}

	@Test
	void oneBucketPerGamestyleExactly() {
		assertThrows(IndexOutOfBoundsException.class,
				() -> NetMPModeRegistry.forStyle(GameEngine.MAX_GAMESTYLE),
				"BY_STYLE must have exactly one bucket per gamestyle");
	}

	@Test
	void everyReferencedNameResolvesToARegisteredNetplayMode() {
		ModeManager mm = new ModeManager();
		mm.loadGameModes(ModeRegistry.all());

		for(int style = 0; style < GameEngine.MAX_GAMESTYLE; style++) {
			for(Entry entry : NetMPModeRegistry.forStyle(style)) {
				GameMode mode = mm.getMode(entry.name());
				assertNotNull(mode, "MP entry '" + entry.name() + "' (style " + style
						+ ") has no matching registered mode");
				assertTrue(mode.isNetplayMode(), "MP entry '" + entry.name()
						+ "' must resolve to a netplay mode (isNetplayMode==true)");
			}
		}
	}

	@Test
	void noDuplicateNamesPerStyle() {
		for(int style = 0; style < GameEngine.MAX_GAMESTYLE; style++) {
			Set<String> seen = new HashSet<>();
			for(Entry entry : NetMPModeRegistry.forStyle(style)) {
				assertTrue(seen.add(entry.name()),
						"duplicate name '" + entry.name() + "' in style " + style);
			}
		}
	}
}
