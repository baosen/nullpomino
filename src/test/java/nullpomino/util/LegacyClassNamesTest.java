package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LegacyClassNamesTest {

	@Test
	void rewritesOldWallkickNames() {
		assertEquals("nullpomino.game.wallkick.StandardWallkick",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick"));
		assertEquals("nullpomino.game.wallkick.StandardWallkick",
				LegacyClassNames.translate("nullpomino.game.subsystem.wallkick.StandardWallkick"));
	}

	@Test
	void rewritesOldModeNames() {
		assertEquals("nullpomino.game.mode.MarathonMode",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.mode.MarathonMode"));
		assertEquals("nullpomino.game.mode.MarathonMode",
				LegacyClassNames.translate("nullpomino.game.subsystem.mode.MarathonMode"));
	}

	@Test
	void rewritesOldModeMenuNames() {
		assertEquals("nullpomino.game.menu.IntegerMenuItem",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.mode.menu.IntegerMenuItem"));
		assertEquals("nullpomino.game.menu.IntegerMenuItem",
				LegacyClassNames.translate("nullpomino.game.subsystem.mode.menu.IntegerMenuItem"));
	}

	@Test
	void passesThroughCurrentNames() {
		assertEquals("nullpomino.game.wallkick.StandardWallkick",
				LegacyClassNames.translate("nullpomino.game.wallkick.StandardWallkick"));
		assertEquals("nullpomino.game.mode.MarathonMode",
				LegacyClassNames.translate("nullpomino.game.mode.MarathonMode"));
		assertEquals("nullpomino.game.menu.IntegerMenuItem",
				LegacyClassNames.translate("nullpomino.game.menu.IntegerMenuItem"));
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
				LegacyClassNames.toLegacy("nullpomino.game.wallkick.StandardWallkick"));
		assertEquals("mu.nu.nullpo.game.subsystem.mode.MarathonMode",
				LegacyClassNames.toLegacy("nullpomino.game.mode.MarathonMode"));
		assertEquals("mu.nu.nullpo.game.subsystem.mode.menu.IntegerMenuItem",
				LegacyClassNames.toLegacy("nullpomino.game.menu.IntegerMenuItem"));
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
	void toLegacyPassesUnrelatedClassNamesThrough() {
		String unrelated = "com.example.foo.Bar";
		assertEquals(unrelated, LegacyClassNames.toLegacy(unrelated));
	}

	@Test
	void passesThroughCurrentAiNames() {
		assertEquals("nullpomino.game.ai.BasicAI",
				LegacyClassNames.translate("nullpomino.game.ai.BasicAI"));
		assertEquals("nullpomino.game.ai.PoochyBot",
				LegacyClassNames.translate("nullpomino.game.ai.PoochyBot"));
	}
}
