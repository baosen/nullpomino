package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void passesThroughForeignNames() {
		assertEquals("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer",
				LegacyClassNames.translate("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer"));
	}

	@Test
	void emitsLegacyPrefixForCurrentNames() {
		assertEquals("mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick",
				LegacyClassNames.toLegacy("nullpomino.game.subsystem.wallkick.StandardWallkick"));
	}

	@Test
	void leavesForeignNamesWhenEmittingLegacyNames() {
		assertEquals("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer",
				LegacyClassNames.toLegacy("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer"));
	}

	@Test
	void handlesNull() {
		assertNull(LegacyClassNames.translate(null));
		assertNull(LegacyClassNames.toLegacy(null));
	}

	@Test
	void rewritesLegacyAiInternalPrefix() {
		assertEquals("nullpomino.ai.BasicAI",
				LegacyClassNames.translate("nullpomino.game.subsystem.ai.BasicAI"));
	}

	@Test
	void rewritesLegacyAiPoochyPrefix() {
		assertEquals("nullpomino.ai.PoochyBot",
				LegacyClassNames.translate("net.tetrisconcept.poochy.nullpomino.ai.PoochyBot"));
	}

	@Test
	void rewritesDoubleLegacyAiPrefix() {
		assertEquals("nullpomino.ai.BasicAI",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.ai.BasicAI"));
		assertEquals("nullpomino.ai.RanksAI",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.ai.RanksAI"));
		assertEquals("nullpomino.ai.TSpinAI",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.ai.TSpinAI"));
	}

	@Test
	void passesThroughCurrentAiNames() {
		assertEquals("nullpomino.ai.BasicAI",
				LegacyClassNames.translate("nullpomino.ai.BasicAI"));
		assertEquals("nullpomino.ai.PoochyBot",
				LegacyClassNames.translate("nullpomino.ai.PoochyBot"));
	}
}
