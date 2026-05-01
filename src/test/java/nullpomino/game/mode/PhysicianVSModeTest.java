package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link PhysicianVSMode}: the registry
 * surface (PHYSICIAN style + 2-player) and the per-preset speed I/O
 * round-trip under the 'physicianvs.*' prefix.
 */
class PhysicianVSModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("PHYSICIAN VS-BATTLE (RC1)", new PhysicianVSMode().getName());
	}

	@Test
	void getPlayersIsTwoSoTheModeRegistryRequestsTwoEngines() {
		assertEquals(2, new PhysicianVSMode().getPlayers());
	}

	@Test
	void getGameStyleIsPhysicianForCascadeFieldLayout() {
		assertEquals(GameEngine.GAMESTYLE_PHYSICIAN, new PhysicianVSMode().getGameStyle());
	}

	@Test
	void loadPresetReadsAllSpeedKeysFromTheGivenPreset() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("physicianvs.gravity.5", 64);
		prop.setProperty("physicianvs.denominator.5", 256);
		prop.setProperty("physicianvs.are.5", 30);
		prop.setProperty("physicianvs.areLine.5", 25);
		prop.setProperty("physicianvs.lineDelay.5", 40);
		prop.setProperty("physicianvs.lockDelay.5", 30);
		prop.setProperty("physicianvs.das.5", 14);

		invokeLoadPreset(mode, engine, prop, 5);

		assertEquals(64, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(30, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
	}

	@Test
	void loadPresetAppliesDocumentedDefaultsForMissingPresetEntries() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(24, engine.speed.are,
				"PHYSICIAN VS defaults ARE to 24 (slower than tetromino's 0)");
		assertEquals(24, engine.speed.areLine);
		assertEquals(10, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderPhysicianVSPrefix() throws Exception {
		PhysicianVSMode source = new PhysicianVSMode();
		GameEngine sourceEngine = freshEngine(source);
		sourceEngine.speed.gravity = 99;
		sourceEngine.speed.are = 12;
		sourceEngine.speed.lockDelay = 25;
		sourceEngine.speed.das = 9;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, sourceEngine, prop, 1);

		assertEquals(99, prop.getProperty("physicianvs.gravity.1", -1));
		assertEquals(12, prop.getProperty("physicianvs.are.1", -1));
		assertEquals(25, prop.getProperty("physicianvs.lockDelay.1", -1));
		assertEquals(9, prop.getProperty("physicianvs.das.1", -1));

		PhysicianVSMode dest = new PhysicianVSMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(12, destEngine.speed.are);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(9, destEngine.speed.das);
	}

	@Test
	void presetSlotsAreScopedByNumberSoTwoPresetsDoNotCollide() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		engine.speed.gravity = 1;
		invokeSavePreset(mode, engine, prop, 0);
		engine.speed.gravity = 999;
		invokeSavePreset(mode, engine, prop, 1);

		assertEquals(1, prop.getProperty("physicianvs.gravity.0", -1));
		assertEquals(999, prop.getProperty("physicianvs.gravity.1", -1));
	}

	private static GameEngine freshEngine(PhysicianVSMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(PhysicianVSMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(PhysicianVSMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}
}
