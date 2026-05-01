package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.subsystem.wallkick.StandardWallkick;
import nullpomino.game.subsystem.wallkick.Wallkick;
import nullpomino.game.randomizer.MemorylessRandomizer;
import nullpomino.game.randomizer.Randomizer;

import org.junit.jupiter.api.Test;

class ClassFactoryTest {

	@Test
	void createsTypedInstances() throws Exception {
		Randomizer randomizer = ClassFactory.create(
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer",
				Randomizer.class);

		assertTrue(randomizer instanceof MemorylessRandomizer);
	}

	@Test
	void translatesLegacyNamesBeforeCreating() throws Exception {
		Wallkick wallkick = ClassFactory.create(
				"mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick",
				Wallkick.class);

		assertTrue(wallkick instanceof StandardWallkick);
	}

	@Test
	void rejectsUnexpectedTypes() {
		assertThrows(ClassCastException.class, () -> ClassFactory.create(
				"mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick",
				Randomizer.class));
	}
}
