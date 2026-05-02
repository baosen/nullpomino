package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import nullpomino.game.component.Block;

import org.junit.jupiter.api.Test;

/**
 * Pins the public static contract surface on {@link Avalanche1PDummyMode}.
 * The 1P branch differs from the VS branch in two places: the chain-power
 * curve (the 1P CHAIN_POWERS_FEVERTYPE has its own values) and the
 * FEVER_MAPS list (1P ships "Poochy7" where VS ships "Compendium" as the
 * fifth map). The shared constants — PIECE_ENABLE, BLOCK_COLORS, DAS —
 * stay byte-stable across both branches.
 */
class Avalanche1PDummyModeConstantsTest {

	@Test
	void pieceEnableActivatesOnlyI3JustLikeTheVSBranch() {
		// Avalanche family drops 1x2 colored bombs (the I3 piece at
		// index 8) — none of the standard tetrominoes apply.
		assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
				Avalanche1PDummyMode.PIECE_ENABLE);
	}

	@Test
	void chainPowersFeverTypeFollowsThe1PFeverAttackTable() {
		// 16-step fever attack curve. Pin both length and values so a
		// re-balance has to re-confirm the byte shape.
		assertArrayEquals(
				new int[] {
						4, 12, 24, 32, 48, 96, 160, 240,
						320, 400, 500, 600, 700, 800, 900, 999
				},
				Avalanche1PDummyMode.CHAIN_POWERS_FEVERTYPE);
		assertEquals(16, Avalanche1PDummyMode.CHAIN_POWERS_FEVERTYPE.length);
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
				Avalanche1PDummyMode.BLOCK_COLORS);
	}

	@Test
	void blockColorsMatchTheVSDummyModeBlockColors() {
		// Both branches must use the same five-color palette so save
		// data round-trips between 1P and VS.
		assertArrayEquals(
				AvalancheVSDummyMode.BLOCK_COLORS,
				Avalanche1PDummyMode.BLOCK_COLORS);
	}

	@Test
	void feverMapsReplacesCompendiumWithPoochy7AsTheFifthEntry() {
		// 1P maps: Fever, 15th, 15thDS, 7, Poochy7.
		// VS maps:  Fever, 15th, 15thDS, 7, Compendium.
		// Pin the 1P-specific tail so a future merge doesn't accidentally
		// reuse the VS list.
		assertArrayEquals(
				new String[] {"Fever", "15th", "15thDS", "7", "Poochy7"},
				Avalanche1PDummyMode.FEVER_MAPS);
	}

	@Test
	void feverMapsLastEntryDivergesFromVSFeverMaps() {
		String[] onePlayer = Avalanche1PDummyMode.FEVER_MAPS;
		String[] versus = AvalancheVSDummyMode.FEVER_MAPS;

		assertEquals(versus.length, onePlayer.length,
				"both branches must list five fever maps");
		// First four entries match.
		for(int i = 0; i < 4; i++) {
			assertEquals(versus[i], onePlayer[i],
					"FEVER_MAPS[" + i + "] must match between 1P and VS");
		}
		// Last entry differs: VS=Compendium, 1P=Poochy7.
		assertNotEquals(versus[4], onePlayer[4],
				"the 5th fever-map slot is the documented divergence");
		assertEquals("Poochy7", onePlayer[4]);
	}

	@Test
	void chainPowersFeverTypeDivergesFromVSDummyChainPowers() {
		// VS: {4,12,24,33,50,101,169,254,341,428,538,648,763,876,990,999}
		// 1P: {4,12,24,32,48, 96,160,240,320,400,500,600,700,800,900,999}
		// They share length and the first three entries, but diverge
		// from index 3 on. Pin that the two tables aren't identical.
		assertEquals(AvalancheVSDummyMode.CHAIN_POWERS.length,
				Avalanche1PDummyMode.CHAIN_POWERS_FEVERTYPE.length);
		boolean differ = false;
		for(int i = 0; i < AvalancheVSDummyMode.CHAIN_POWERS.length; i++) {
			if(AvalancheVSDummyMode.CHAIN_POWERS[i]
					!= Avalanche1PDummyMode.CHAIN_POWERS_FEVERTYPE[i]) {
				differ = true;
				break;
			}
		}
		assert(differ) : "1P and VS chain-power tables must not be identical";
	}

	@Test
	void dasIsTen() {
		assertEquals(10, Avalanche1PDummyMode.DAS,
				"DAS = 10 frames is the documented Avalanche-1P horizontal "
						+ "auto-shift delay");
	}
}
