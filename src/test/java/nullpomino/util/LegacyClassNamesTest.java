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
}
