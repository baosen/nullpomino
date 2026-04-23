package mu.nu.nullpo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import mu.nu.nullpo.game.subsystem.mode.GameMode;

import org.junit.jupiter.api.Test;

/**
 * Pins the contract that every FQCN listed in config/list/mode.lst
 * loads, instantiates, and that its getName() string round-trips
 * through ModeManager.getMode(String). getName() strings are a wire
 * key — replays store name.mode and netplay leaderboards URL-encode
 * it — so any refactor that shifts class hierarchy or accidentally
 * shadows getName() will fail this test before it ships.
 */
class ModeRegistryTest {

	private static final Path MODE_LST = Paths.get("config/list/mode.lst");

	@Test
	void modeListEntriesAllLoadAndRoundTripByName() throws Exception {
		long expected = Files.lines(MODE_LST)
				.map(String::trim)
				.filter(s -> !s.isEmpty() && !s.startsWith("#"))
				.count();

		ModeManager mm = new ModeManager();
		try (BufferedReader r = new BufferedReader(new FileReader(MODE_LST.toFile()))) {
			mm.loadGameModes(r);
		}

		assertEquals(expected, mm.getSize(),
				"every non-comment entry in mode.lst must resolve via Class.forName");

		Set<String> seen = new HashSet<>();
		for (int i = 0; i < mm.getSize(); i++) {
			GameMode mode = mm.getMode(i);
			assertNotNull(mode, "modelist[" + i + "] must not be null");
			String name = mode.getName();
			assertNotNull(name, "mode " + mode.getClass().getName() + " returned null getName()");
			assertSame(mode, mm.getMode(name),
					"getMode(\"" + name + "\") must round-trip to the same instance as modelist[" + i + "]");
			if (!seen.add(name)) {
				throw new AssertionError("duplicate getName() string across modes: " + name);
			}
		}
	}
}
