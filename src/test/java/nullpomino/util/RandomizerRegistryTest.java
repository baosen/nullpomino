package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import nullpomino.game.randomizer.Randomizer;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class RandomizerRegistryTest {

	@Test
	void classNamesKeepToolMenuOrder() {
		assertEquals(List.of(
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.BagNoSZORandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.BagBonusRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.BagBonusBagRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.DoubleBagRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.NineBagRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.BagMinusRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.BagMinusTwoRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.History4RollsRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.History6RollsRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.StrictHistoryRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.NintendoRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.GameBoyRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.LinearDistWeightRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.QuadraticDistWeightRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.ExpDistWeightRandomizer",
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.FixedSequenceRandomizer"),
				RandomizerRegistry.classNames());
	}

	@TestFactory
	Stream<DynamicTest> everyRegistryEntryInstantiates() {
		assertTrue(!RandomizerRegistry.all().isEmpty(), "randomizer registry is empty");

		return RandomizerRegistry.all().stream().map(cls -> DynamicTest.dynamicTest(cls.getName(), () -> {
			Object instance = cls.getDeclaredConstructor().newInstance();
			assertTrue(instance instanceof Randomizer,
					cls.getName() + " is listed but does not implement "
							+ Randomizer.class.getName());
		}));
	}
}
