package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.component.Block;
import nullpomino.game.play.GameEngine;

import org.junit.jupiter.api.Test;

/**
 * Pins the public static constants on {@link AvalancheVSDummyMode}.
 * These are the wire/save shapes for the AVALANCHE-VS family — each
 * concrete subclass loads its presets keyed by {@code MAX_PLAYERS},
 * its on-disk colors are encoded by {@code BLOCK_COLORS} indices, and
 * the fever-bomb subclasses look up map sets by name in
 * {@code FEVER_MAPS}. The string label arrays drive the on-screen
 * setting menus, so changing them shifts user-visible text.
 */
class AvalancheVSDummyModeConstantsTest {

	@Test
	void pieceEnableActivatesOnlySliceI3() {
		// The avalanche family uses the I3 piece (index 8) only — the
		// six standard tetrominoes are disabled because Avalanche drops
		// 1x2 colored bombs, not Tetris pieces. Pin the array shape so
		// a future piece-list edit has to acknowledge this.
		assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
				AvalancheVSDummyMode.PIECE_ENABLE);
	}

	@Test
	void blockColorsAreTheFiveAvalancheStandardColors() {
		assertArrayEquals(
				new int[] {
						Block.BLOCK_COLOR_RED,
						Block.BLOCK_COLOR_GREEN,
						Block.BLOCK_COLOR_BLUE,
						Block.BLOCK_COLOR_YELLOW,
						Block.BLOCK_COLOR_PURPLE
				},
				AvalancheVSDummyMode.BLOCK_COLORS);
	}

	@Test
	void feverMapsListsTheFiveBundledMapSets() {
		assertArrayEquals(
				new String[] {"Fever", "15th", "15thDS", "7", "Compendium"},
				AvalancheVSDummyMode.FEVER_MAPS);
	}

	@Test
	void chainPowersFollowTheArleAttackTable() {
		// 16-step Arle attack curve. Pin both length and values so a
		// future re-balance has to re-confirm the byte shape.
		assertArrayEquals(
				new int[] {
						4, 12, 24, 33, 50, 101, 169, 254,
						341, 428, 538, 648, 763, 876, 990, 999
				},
				AvalancheVSDummyMode.CHAIN_POWERS);
		assertEquals(16, AvalancheVSDummyMode.CHAIN_POWERS.length);
	}

	@Test
	void maxPlayersIsTwo() {
		assertEquals(2, AvalancheVSDummyMode.MAX_PLAYERS,
				"AVALANCHE-VS modes are 1v1 — preset/save arrays size to 2");
	}

	@Test
	void ojamaCounterConstantsAreOffOnFeverInOrder() {
		assertEquals(0, AvalancheVSDummyMode.OJAMA_COUNTER_OFF);
		assertEquals(1, AvalancheVSDummyMode.OJAMA_COUNTER_ON);
		assertEquals(2, AvalancheVSDummyMode.OJAMA_COUNTER_FEVER);
		assertArrayEquals(
				new String[] {"OFF", "ON", "FEVER"},
				AvalancheVSDummyMode.OJAMA_COUNTER_STRING);
	}

	@Test
	void zenkeshiConstantsAreOffOnFeverInOrder() {
		assertEquals(0, AvalancheVSDummyMode.ZENKESHI_MODE_OFF);
		assertEquals(1, AvalancheVSDummyMode.ZENKESHI_MODE_ON);
		assertEquals(2, AvalancheVSDummyMode.ZENKESHI_MODE_FEVER);
		assertArrayEquals(
				new String[] {"OFF", "ON", "FEVER"},
				AvalancheVSDummyMode.ZENKESHI_TYPE_NAMES);
	}

	@Test
	void outlineTypeNamesAreNormalColorNoneInOrder() {
		assertArrayEquals(
				new String[] {"NORMAL", "COLOR", "NONE"},
				AvalancheVSDummyMode.OUTLINE_TYPE_NAMES);
	}

	@Test
	void chainDisplayConstantsAreParallelToTheirNames() {
		assertEquals(0, AvalancheVSDummyMode.CHAIN_DISPLAY_NONE);
		assertEquals(1, AvalancheVSDummyMode.CHAIN_DISPLAY_YELLOW);
		assertEquals(2, AvalancheVSDummyMode.CHAIN_DISPLAY_PLAYER);
		assertEquals(3, AvalancheVSDummyMode.CHAIN_DISPLAY_SIZE);
		assertArrayEquals(
				new String[] {"OFF", "YELLOW", "PLAYER", "SIZE"},
				AvalancheVSDummyMode.CHAIN_DISPLAY_NAMES);
		assertEquals(4, AvalancheVSDummyMode.CHAIN_DISPLAY_NAMES.length,
				"name array must be parallel to the four CHAIN_DISPLAY_* constants");
	}

	@Test
	void playerColorFrameAlignsRedWithPlayerOneAndBlueWithPlayerTwo() {
		assertArrayEquals(
				new int[] {GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE},
				AvalancheVSDummyMode.PLAYER_COLOR_FRAME);
		assertEquals(AvalancheVSDummyMode.MAX_PLAYERS,
				AvalancheVSDummyMode.PLAYER_COLOR_FRAME.length,
				"frame-color array must stay parallel to MAX_PLAYERS");
	}
}
