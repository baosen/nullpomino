package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link AvalancheVSDigRaceMode}: registry
 * surface (name + AVALANCHE style + two-player count, all inherited
 * from {@link AvalancheVSDummyMode}), and the per-preset speed I/O
 * round-trip under the 'avalanchevsdigrace.<key>.<preset>' shape.
 */
class AvalancheVSDigRaceModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("AVALANCHE VS DIG RACE (RC1)",
				new AvalancheVSDigRaceMode().getName());
	}

	@Test
	void getPlayersIsTwoSoTheModeRegistryRequestsTwoEngines() {
		assertEquals(2, new AvalancheVSDigRaceMode().getPlayers());
	}

	@Test
	void getGameStyleIsAvalancheInheritedFromTheDummyParent() {
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE,
				new AvalancheVSDigRaceMode().getGameStyle());
	}

	@Test
	void loadPresetReadsAllSpeedAndCascadeKeysUnderTheDigraceConcatenatedSuffix() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsdigrace.gravity.5", 64);
		prop.setProperty("avalanchevsdigrace.denominator.5", 256);
		prop.setProperty("avalanchevsdigrace.are.5", 30);
		prop.setProperty("avalanchevsdigrace.areLine.5", 25);
		prop.setProperty("avalanchevsdigrace.lineDelay.5", 40);
		prop.setProperty("avalanchevsdigrace.lockDelay.5", 30);
		prop.setProperty("avalanchevsdigrace.das.5", 14);
		prop.setProperty("avalanchevsdigrace.fallDelay.5", 3);
		prop.setProperty("avalanchevsdigrace.clearDelay.5", 12);

		invokeLoadPreset(mode, engine, prop, 5, "digrace");

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
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99, "digrace");

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(30, engine.speed.are);
		assertEquals(30, engine.speed.areLine);
		assertEquals(10, engine.speed.lineDelay);
		assertEquals(60, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		assertEquals(1, engine.cascadeDelay);
		assertEquals(10, engine.cascadeClearDelay);
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderAvalancheVSDigRacePrefix() throws Exception {
		AvalancheVSDigRaceMode source = new AvalancheVSDigRaceMode();
		GameEngine sourceEngine = freshEngine(source);
		sourceEngine.speed.gravity = 99;
		sourceEngine.speed.lockDelay = 25;
		sourceEngine.cascadeDelay = 2;
		sourceEngine.cascadeClearDelay = 8;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, sourceEngine, prop, 1, "digrace");

		assertEquals(99, prop.getProperty("avalanchevsdigrace.gravity.1", -1));
		assertEquals(25, prop.getProperty("avalanchevsdigrace.lockDelay.1", -1));
		assertEquals(2, prop.getProperty("avalanchevsdigrace.fallDelay.1", -1));
		assertEquals(8, prop.getProperty("avalanchevsdigrace.clearDelay.1", -1));

		AvalancheVSDigRaceMode dest = new AvalancheVSDigRaceMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1, "digrace");

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(2, destEngine.cascadeDelay);
		assertEquals(8, destEngine.cascadeClearDelay);
	}

	@Test
	void digraceSuffixSeparatesFromTheRootAvalancheVSKeyspace() throws Exception {
		// Saves under 'digrace' name must not collide with the root
		// 'avalanchevs.<key>.<preset>' keyspace used by AvalancheVSMode
		// (which uses an empty name).
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		engine.speed.gravity = 100;
		invokeSavePreset(mode, engine, prop, 0, "digrace");

		// Parent concatenates 'avalanchevs' + 'digrace' + '.gravity.0'.
		assertEquals(100, prop.getProperty("avalanchevsdigrace.gravity.0", -1));
		// The empty-name keyspace stays untouched.
		assertEquals(-1, prop.getProperty("avalanchevs.gravity.0", -1));
	}

	private static GameEngine freshEngine(AvalancheVSDigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(AvalancheVSDigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}

	private static void invokeSavePreset(AvalancheVSDigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}
}
