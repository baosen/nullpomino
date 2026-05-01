package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LegacyClassNamesTest {

	@Test
	void rewritesLegacyPrefix() {
		assertEquals("nullpomino.game.subsystem.wallkick.StandardWallkick",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick"));
	}

	@Test
	void passesThroughCurrentNames() {
		assertEquals("nullpomino.game.subsystem.wallkick.StandardWallkick",
				LegacyClassNames.translate("nullpomino.game.subsystem.wallkick.StandardWallkick"));
	}

	@Test
	void rewritesLegacyRandomizerPrefix() {
		assertEquals("nullpomino.game.randomizer.BagRandomizer",
				LegacyClassNames.translate("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer"));
	}

	@Test
	void passesThroughCurrentRandomizerNames() {
		assertEquals("nullpomino.game.randomizer.BagRandomizer",
				LegacyClassNames.translate("nullpomino.game.randomizer.BagRandomizer"));
	}

	@Test
	void emitsLegacyPrefixForCurrentNames() {
		assertEquals("mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick",
				LegacyClassNames.toLegacy("nullpomino.game.subsystem.wallkick.StandardWallkick"));
	}

	@Test
	void emitsLegacyRandomizerPrefixForCurrentRandomizer() {
		assertEquals("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer",
				LegacyClassNames.toLegacy("nullpomino.game.randomizer.BagRandomizer"));
	}

	@Test
	void doesNotApplyGeneralFallbackToRandomizerNames() {
		String legacy = LegacyClassNames.toLegacy("nullpomino.game.randomizer.BagRandomizer");
		assertFalse(legacy.startsWith("mu.nu.nullpo."));
	}

	@Test
	void handlesNull() {
		assertNull(LegacyClassNames.translate(null));
		assertNull(LegacyClassNames.toLegacy(null));
	}

	@Test
	void rewritesLegacyAiInternalPrefix() {
		assertEquals("nullpomino.game.ai.BasicAI",
				LegacyClassNames.translate("nullpomino.game.subsystem.ai.BasicAI"));
	}

	@Test
	void rewritesLegacyAiPoochyPrefix() {
		assertEquals("nullpomino.game.ai.PoochyBot",
				LegacyClassNames.translate("net.tetrisconcept.poochy.nullpomino.ai.PoochyBot"));
	}

	@Test
	void rewritesDoubleLegacyAiPrefix() {
		assertEquals("nullpomino.game.ai.BasicAI",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.ai.BasicAI"));
		assertEquals("nullpomino.game.ai.RanksAI",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.ai.RanksAI"));
		assertEquals("nullpomino.game.ai.TSpinAI",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.ai.TSpinAI"));
	}

	@Test
	void passesThroughCurrentAiNames() {
		assertEquals("nullpomino.game.ai.BasicAI",
				LegacyClassNames.translate("nullpomino.game.ai.BasicAI"));
		assertEquals("nullpomino.game.ai.PoochyBot",
				LegacyClassNames.translate("nullpomino.game.ai.PoochyBot"));
	}
}
