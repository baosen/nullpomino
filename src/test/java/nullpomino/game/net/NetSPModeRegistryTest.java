package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nullpomino.game.net.NetSPModeRegistry.Entry;
import nullpomino.game.play.GameEngine;
import nullpomino.util.ModeManager;
import nullpomino.util.ModeRegistry;

import org.junit.jupiter.api.Test;

/**
 * Pins the single-player netplay mode list against the registered mode roster.
 * Catches typos in {@link NetSPModeRegistry} and silent renames in local mode
 * classes that would otherwise produce unreachable SP leaderboards.
 */
class NetSPModeRegistryTest {

	@Test
	void tetrominoEntriesMatchSpec() {
		assertEquals(List.of(
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
				NetSPModeRegistry.forStyle(GameEngine.GAMESTYLE_TETROMINO),
				"tetromino SP entries are wire-visible (rankingType / maxGameType "
						+ "drive the on-disk ranking grid in NetServer.loadSPRankingList)");
	}

	@Test
	void nonTetrominoStylesAreEmpty() {
		assertTrue(NetSPModeRegistry.forStyle(GameEngine.GAMESTYLE_AVALANCHE).isEmpty());
		assertTrue(NetSPModeRegistry.forStyle(GameEngine.GAMESTYLE_PHYSICIAN).isEmpty());
		assertTrue(NetSPModeRegistry.forStyle(GameEngine.GAMESTYLE_SPF).isEmpty());
	}

	@Test
	void oneBucketPerGamestyleExactly() {
		assertThrows(IndexOutOfBoundsException.class,
				() -> NetSPModeRegistry.forStyle(GameEngine.MAX_GAMESTYLE),
				"BY_STYLE must have exactly one bucket per gamestyle");
	}

	@Test
	void everyReferencedNameResolvesToARegisteredMode() {
		// SP-netplay mode names refer to LOCAL game-mode classes (offline play
		// with online ranking), so we don't assert isNetplayMode here — only
		// that the name resolves to something in the registered roster.
		ModeManager mm = new ModeManager();
		mm.loadGameModes(ModeRegistry.all());

		for(int style = 0; style < GameEngine.MAX_GAMESTYLE; style++) {
			for(Entry entry : NetSPModeRegistry.forStyle(style)) {
				assertNotNull(mm.getMode(entry.name()),
						"SP entry '" + entry.name() + "' (style " + style
								+ ") has no matching registered mode");
			}
		}
	}

	@Test
	void noDuplicateNamesPerStyle() {
		for(int style = 0; style < GameEngine.MAX_GAMESTYLE; style++) {
			Set<String> seen = new HashSet<>();
			for(Entry entry : NetSPModeRegistry.forStyle(style)) {
				assertTrue(seen.add(entry.name()),
						"duplicate name '" + entry.name() + "' in style " + style);
			}
		}
	}
}
