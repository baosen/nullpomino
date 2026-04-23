package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Pins that every FQCN in config/list/randomizer.lst resolves to a
 * concrete net.omegaboshi.*.Randomizer. Guards the Phase 1b deletion
 * of the deprecated nullpomino.game.subsystem.randomizer package —
 * if a randomizer entry secretly depended on that package, loading
 * would fail and this test would catch it before the delete ships.
 */
class RandomizerRegistryTest {

	private static final Path LIST = Paths.get("config/list/randomizer.lst");

	@TestFactory
	Stream<DynamicTest> everyListedRandomizerInstantiates() throws Exception {
		assertTrue(Files.exists(LIST), "randomizer.lst must be on runfiles path");

		List<String> names;
		try (Stream<String> lines = Files.lines(LIST)) {
			names = lines.map(String::trim)
					.filter(s -> !s.isEmpty() && !s.startsWith("#"))
					.collect(Collectors.toList());
		}
		assertTrue(!names.isEmpty(), "randomizer.lst is empty");

		return names.stream().map(fqcn -> DynamicTest.dynamicTest(fqcn, () -> {
			Class<?> cls = Class.forName(fqcn);
			Object instance = cls.getDeclaredConstructor().newInstance();
			assertTrue(instance instanceof Randomizer,
					fqcn + " is listed in randomizer.lst but does not implement "
							+ Randomizer.class.getName());
		}));
	}
}
