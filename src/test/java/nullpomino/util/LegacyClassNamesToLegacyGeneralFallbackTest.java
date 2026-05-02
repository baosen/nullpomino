package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Covers the general CURRENT_PREFIX fallback branch in
 * {@link LegacyClassNames#toLegacy(String)} (line 82) — class names that start
 * with "nullpomino." but are not in any of the specific sub-packages
 * (randomizer, mode menu, mode, wallkick).
 */
class LegacyClassNamesToLegacyGeneralFallbackTest {

	@Test
	void toLegacyRewritesCurrentPrefixForNonSpecificSubPackages() {
		// "nullpomino.game.play.GameEngine" starts with CURRENT_PREFIX but is
		// not randomizer, mode-menu, mode, or wallkick → hits the general
		// fallback at line 82.
		assertEquals("mu.nu.nullpo.game.play.GameEngine",
				LegacyClassNames.toLegacy("nullpomino.game.play.GameEngine"));
	}

	@Test
	void toLegacyRewritesUtilPackageViaGeneralFallback() {
		assertEquals("mu.nu.nullpo.util.GeneralUtil",
				LegacyClassNames.toLegacy("nullpomino.util.GeneralUtil"));
	}

	@Test
	void toLegacyRewritesComponentPackageViaGeneralFallback() {
		assertEquals("mu.nu.nullpo.game.component.Field",
				LegacyClassNames.toLegacy("nullpomino.game.component.Field"));
	}

	@Test
	void toLegacyRewritesEventPackageViaGeneralFallback() {
		assertEquals("mu.nu.nullpo.game.event.EventReceiver",
				LegacyClassNames.toLegacy("nullpomino.game.event.EventReceiver"));
	}
}
