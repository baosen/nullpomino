package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashSet;
import java.util.Set;

import nullpomino.game.subsystem.mode.GameMode;

import org.junit.jupiter.api.Test;

/**
 * Pins the contract that every class in {@link ModeRegistry#all()} loads,
 * instantiates, and that its {@code getName()} string round-trips through
 * {@link ModeManager#getMode(String)}. {@code getName()} strings are a wire
 * key — replays store {@code name.mode} and netplay leaderboards URL-encode
 * it — so any refactor that shifts class hierarchy or accidentally shadows
 * {@code getName()} will fail this test before it ships.
 */
class ModeRegistryTest {

	@Test
	void registryEntriesAllLoadAndRoundTripByName() {
		ModeManager mm = new ModeManager();
		mm.loadGameModes(ModeRegistry.all());

		assertEquals(ModeRegistry.all().size(), mm.getSize(),
				"every registry entry must instantiate");

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
