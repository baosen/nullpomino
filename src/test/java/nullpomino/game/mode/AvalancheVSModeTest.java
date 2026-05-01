package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link AvalancheVSMode}: the registry
 * surface (name, two-player count, AVALANCHE game style), and the
 * per-preset speed I/O round-trip from the {@link AvalancheVSDummyMode}
 * superclass under the 'avalanchevs.<key>.<preset>' shape.
 */
class AvalancheVSModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("AVALANCHE VS-BATTLE (RC1)", new AvalancheVSMode().getName());
	}

	@Test
	void getPlayersIsTwoSoTheModeRegistryRequestsTwoEngines() {
		assertEquals(2, new AvalancheVSMode().getPlayers());
	}

	@Test
	void getGameStyleIsAvalancheSoTheRendererPicksTheAvalancheField() {
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE, new AvalancheVSMode().getGameStyle());
	}

	@Test
	void loadPresetReadsAllSpeedAndCascadeKeysFromTheGivenPreset() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.gravity.5", 64);
		prop.setProperty("avalanchevs.denominator.5", 256);
		prop.setProperty("avalanchevs.are.5", 30);
		prop.setProperty("avalanchevs.areLine.5", 25);
		prop.setProperty("avalanchevs.lineDelay.5", 40);
		prop.setProperty("avalanchevs.lockDelay.5", 30);
		prop.setProperty("avalanchevs.das.5", 14);
		prop.setProperty("avalanchevs.fallDelay.5", 3);
		prop.setProperty("avalanchevs.clearDelay.5", 12);

		invokeLoadPreset(mode, engine, prop, 5, "");

		assertEquals(64, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(30, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		assertEquals(3, engine.cascadeDelay);
		assertEquals(12, engine.cascadeClearDelay);
	}

	@Test
	void loadPresetAppliesDocumentedDefaultsForMissingPresetEntries() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99, "");

		// Avalanche defaults differ from the standard tetromino mode set.
		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(30, engine.speed.are,
				"AVALANCHE-VS defaults ARE to 30 (slower than tetromino's 0)");
		assertEquals(30, engine.speed.areLine);
		assertEquals(10, engine.speed.lineDelay);
		assertEquals(60, engine.speed.lockDelay,
				"AVALANCHE-VS lockDelay defaults to 60 (twice the tetromino default)");
		assertEquals(14, engine.speed.das);
		assertEquals(1, engine.cascadeDelay);
		assertEquals(10, engine.cascadeClearDelay);
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderAvalancheVSPrefix() throws Exception {
		AvalancheVSMode source = new AvalancheVSMode();
		GameEngine sourceEngine = freshEngine(source);
		sourceEngine.speed.gravity = 99;
		sourceEngine.speed.denominator = 60;
		sourceEngine.speed.are = 12;
		sourceEngine.speed.areLine = 11;
		sourceEngine.speed.lineDelay = 5;
		sourceEngine.speed.lockDelay = 25;
		sourceEngine.speed.das = 9;
		sourceEngine.cascadeDelay = 2;
		sourceEngine.cascadeClearDelay = 8;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, sourceEngine, prop, 1, "");

		assertEquals(99, prop.getProperty("avalanchevs.gravity.1", -1));
		assertEquals(2, prop.getProperty("avalanchevs.fallDelay.1", -1));
		assertEquals(8, prop.getProperty("avalanchevs.clearDelay.1", -1));

		AvalancheVSMode dest = new AvalancheVSMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1, "");

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(60, destEngine.speed.denominator);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(2, destEngine.cascadeDelay);
		assertEquals(8, destEngine.cascadeClearDelay);
	}

	@Test
	void presetNameSuffixSeparatesNamedPresetSetsFromTheRoot() throws Exception {
		// The 'name' parameter of loadPreset / savePreset slots between
		// 'avalanchevs' and the key name, so a non-empty name like
		// '.fever' yields keys under 'avalanchevs.fever.gravity.<preset>'.
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		engine.speed.gravity = 100;
		invokeSavePreset(mode, engine, prop, 0, ".fever");

		assertEquals(100, prop.getProperty("avalanchevs.fever.gravity.0", -1));
		// The root keyspace is untouched.
		assertEquals(-1, prop.getProperty("avalanchevs.gravity.0", -1));
	}

	private static GameEngine freshEngine(AvalancheVSMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(AvalancheVSMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}

	private static void invokeSavePreset(AvalancheVSMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}
}
