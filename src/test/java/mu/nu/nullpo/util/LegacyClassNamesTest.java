package mu.nu.nullpo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LegacyClassNamesTest {

	@Test
	void passesThroughUnknownNames() {
		assertEquals("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer",
				LegacyClassNames.translate("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer"));
	}

	@Test
	void identityWhileShimIsInactive() {
		assertEquals("mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick",
				LegacyClassNames.translate("mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick"));
	}
}
