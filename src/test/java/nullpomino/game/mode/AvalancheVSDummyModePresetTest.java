package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSDummyMode}'s shared {@code loadPreset} /
 * {@code savePreset} — the speed/cascade preset reader that every
 * AvalancheVS family subclass delegates to (the {@code name} suffix
 * argument lets each subclass scope its keys: {@code "fever"}, "spf",
 * "bombbattle", "digrace", or empty for the base AvalancheVSMode).
 * Defaults: gravity=4, denominator=256, are=30, areLine=30,
 * lineDelay=10, lockDelay=60, das=14, cascadeDelay=1,
 * cascadeClearDelay=10.
 */
class AvalancheVSDummyModePresetTest {

	@Test
	void loadPresetAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 0, "");

		assertEquals(4, engine.speed.gravity, "gravity default 4");
		assertEquals(256, engine.speed.denominator, "denominator default 256");
		assertEquals(30, engine.speed.are, "are default 30");
		assertEquals(30, engine.speed.areLine, "areLine default 30");
		assertEquals(10, engine.speed.lineDelay, "lineDelay default 10");
		assertEquals(60, engine.speed.lockDelay,
				"lockDelay default 60 (longer than tetromino's 30)");
		assertEquals(14, engine.speed.das, "das default 14");
		assertEquals(1, engine.cascadeDelay,
				"cascadeDelay default 1 (Avalanche-specific cascade timer)");
		assertEquals(10, engine.cascadeClearDelay,
				"cascadeClearDelay default 10");
	}

	@Test
	void loadPresetReadsAvalanchevsKeysFromProperties() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.gravity.0", 8);
		prop.setProperty("avalanchevs.denominator.0", 64);
		prop.setProperty("avalanchevs.are.0", 45);
		prop.setProperty("avalanchevs.areLine.0", 50);
		prop.setProperty("avalanchevs.lineDelay.0", 15);
		prop.setProperty("avalanchevs.lockDelay.0", 80);
		prop.setProperty("avalanchevs.das.0", 12);
		prop.setProperty("avalanchevs.fallDelay.0", 2);
		prop.setProperty("avalanchevs.clearDelay.0", 20);

		invokeLoadPreset(mode, engine, prop, 0, "");

		assertEquals(8, engine.speed.gravity);
		assertEquals(64, engine.speed.denominator);
		assertEquals(45, engine.speed.are);
		assertEquals(50, engine.speed.areLine);
		assertEquals(15, engine.speed.lineDelay);
		assertEquals(80, engine.speed.lockDelay);
		assertEquals(12, engine.speed.das);
		assertEquals(2, engine.cascadeDelay,
				"cascadeDelay reads from 'fallDelay' legacy key name");
		assertEquals(20, engine.cascadeClearDelay,
				"cascadeClearDelay reads from 'clearDelay' legacy key name");
	}

	@Test
	void loadPresetSubfamilyNameSeparatesNamespaces() throws Exception {
		// "fever" suffix scopes keys to avalanchevsfever.* — the same
		// preset number 0 stored under a different name doesn't leak
		// into the empty-suffix base namespace.
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsfever.gravity.0", 99);
		// avalanchevs.gravity.0 is NOT set; should fall through to default 4.

		invokeLoadPreset(mode, engine, prop, 0, "");

		assertEquals(4, engine.speed.gravity,
				"empty-suffix load doesn't pick up the fever-namespaced key");

		// Now load with name='fever' and the same prop -> picks up the 99.
		invokeLoadPreset(mode, engine, prop, 0, "fever");

		assertEquals(99, engine.speed.gravity,
				"'fever' suffix scopes the lookup to avalanchevsfever.gravity.0");
	}

	@Test
	void savePresetWritesAllSpeedAndCascadeKeysUnderSuffix() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.speed.gravity = 16;
		engine.speed.denominator = 128;
		engine.speed.are = 25;
		engine.speed.areLine = 27;
		engine.speed.lineDelay = 8;
		engine.speed.lockDelay = 50;
		engine.speed.das = 11;
		engine.cascadeDelay = 3;
		engine.cascadeClearDelay = 15;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(mode, engine, prop, 2, "spf");

		assertEquals(16, prop.getProperty("avalanchevsspf.gravity.2", -1));
		assertEquals(128, prop.getProperty("avalanchevsspf.denominator.2", -1));
		assertEquals(25, prop.getProperty("avalanchevsspf.are.2", -1));
		assertEquals(27, prop.getProperty("avalanchevsspf.areLine.2", -1));
		assertEquals(8, prop.getProperty("avalanchevsspf.lineDelay.2", -1));
		assertEquals(50, prop.getProperty("avalanchevsspf.lockDelay.2", -1));
		assertEquals(11, prop.getProperty("avalanchevsspf.das.2", -1));
		assertEquals(3, prop.getProperty("avalanchevsspf.fallDelay.2", -1),
				"savePreset uses 'fallDelay' legacy key for cascadeDelay");
		assertEquals(15, prop.getProperty("avalanchevsspf.clearDelay.2", -1),
				"savePreset uses 'clearDelay' legacy key for cascadeClearDelay");
	}

	@Test
	void saveAndLoadPresetRoundTripPreservesEverySpeedAndCascadeField() throws Exception {
		AvalancheVSMode source = new AvalancheVSMode();
		GameEngine sourceEngine = freshEngine(source);
		sourceEngine.speed.gravity = 7;
		sourceEngine.speed.denominator = 256;
		sourceEngine.speed.are = 32;
		sourceEngine.speed.areLine = 34;
		sourceEngine.speed.lineDelay = 12;
		sourceEngine.speed.lockDelay = 55;
		sourceEngine.speed.das = 13;
		sourceEngine.cascadeDelay = 4;
		sourceEngine.cascadeClearDelay = 11;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, sourceEngine, prop, 1, "digrace");

		AvalancheVSMode dest = new AvalancheVSMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1, "digrace");

		assertEquals(7, destEngine.speed.gravity);
		assertEquals(256, destEngine.speed.denominator);
		assertEquals(32, destEngine.speed.are);
		assertEquals(34, destEngine.speed.areLine);
		assertEquals(12, destEngine.speed.lineDelay);
		assertEquals(55, destEngine.speed.lockDelay);
		assertEquals(13, destEngine.speed.das);
		assertEquals(4, destEngine.cascadeDelay);
		assertEquals(11, destEngine.cascadeClearDelay);
	}

	@Test
	void presetSlotsAreScopedByNumber() throws Exception {
		// Save under preset 0 and 1 with different gravities -> load
		// each back independently.
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.gravity = 100;
		CustomProperties prop = new CustomProperties();
		invokeSavePreset(mode, engine, prop, 0, "");

		engine.speed.gravity = 200;
		invokeSavePreset(mode, engine, prop, 1, "");

		invokeLoadPreset(mode, engine, prop, 0, "");
		assertEquals(100, engine.speed.gravity, "preset 0");

		invokeLoadPreset(mode, engine, prop, 1, "");
		assertEquals(200, engine.speed.gravity, "preset 1");
	}

	private static GameEngine freshEngine(AvalancheVSMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeLoadPreset(AvalancheVSMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class,
				int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}

	private static void invokeSavePreset(AvalancheVSMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class,
				int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}
}
