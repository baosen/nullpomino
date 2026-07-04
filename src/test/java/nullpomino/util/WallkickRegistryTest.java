package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import nullpomino.game.wallkick.Wallkick;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class WallkickRegistryTest {

	@Test
	void classNamesKeepLegacyToolMenuOrder() {
		assertEquals(List.of(
				"mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.StandardMild180Wallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.StandardSymmetricWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.StandardSymmetricMild180Wallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.ClassicWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.ClassicPlusWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.DTETWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.GBCWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.WallOnlyWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.AvalancheWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.AvalancheClassicWallkick",
				"mu.nu.nullpo.game.subsystem.wallkick.PhysicianWallkick"),
				WallkickRegistry.classNames());
	}

	@TestFactory
	Stream<DynamicTest> everyRegistryEntryInstantiates() {
		assertTrue(!WallkickRegistry.all().isEmpty(), "wallkick registry is empty");

		return WallkickRegistry.all().stream().map(cls -> DynamicTest.dynamicTest(cls.getName(), () -> {
			Object instance = cls.getDeclaredConstructor().newInstance();
			assertTrue(instance instanceof Wallkick,
					cls.getName() + " is listed but does not implement "
							+ Wallkick.class.getName());
		}));
	}

	@Test
	void suppliersAlignWithClasses() {
		assertEquals(WallkickRegistry.all().size(), WallkickRegistry.suppliers().size(),
				"suppliers() and all() must stay in lockstep");
		for (int i = 0; i < WallkickRegistry.all().size(); i++) {
			assertEquals(WallkickRegistry.all().get(i), WallkickRegistry.suppliers().get(i).get().getClass(),
					"supplier at index " + i + " builds the wrong class");
		}
	}
}
