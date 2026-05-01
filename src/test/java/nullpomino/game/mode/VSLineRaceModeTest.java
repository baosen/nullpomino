package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link VSLineRaceMode}: registry surface,
 * the two-player count, and the per-preset speed I/O round-trip under
 * the 'vslinerace.*' prefix.
 */
class VSLineRaceModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("VS-LINE RACE", new VSLineRaceMode().getName());
	}

	@Test
	void getPlayersIsTwoSoTheModeRegistryRequestsTwoEngines() {
		assertEquals(2, new VSLineRaceMode().getPlayers());
	}

	@Test
	void modeIsTetrominoStyle() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new VSLineRaceMode().getGameStyle());
	}

	@Test
	void loadPresetReadsAllSpeedKeysFromTheGivenPreset() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("vslinerace.gravity.5", 64);
		prop.setProperty("vslinerace.denominator.5", 256);
		prop.setProperty("vslinerace.are.5", 30);
		prop.setProperty("vslinerace.areLine.5", 25);
		prop.setProperty("vslinerace.lineDelay.5", 40);
		prop.setProperty("vslinerace.lockDelay.5", 30);
		prop.setProperty("vslinerace.das.5", 14);

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
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(0, engine.speed.are);
		assertEquals(0, engine.speed.areLine);
		assertEquals(0, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderVSLineRacePrefix() throws Exception {
		VSLineRaceMode source = new VSLineRaceMode();
		GameEngine sourceEngine = freshEngine(source);
		sourceEngine.speed.gravity = 99;
		sourceEngine.speed.denominator = 60;
		sourceEngine.speed.are = 12;
		sourceEngine.speed.areLine = 11;
		sourceEngine.speed.lineDelay = 5;
		sourceEngine.speed.lockDelay = 25;
		sourceEngine.speed.das = 9;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, sourceEngine, prop, 1);

		assertEquals(99, prop.getProperty("vslinerace.gravity.1", -1));
		assertEquals(60, prop.getProperty("vslinerace.denominator.1", -1));
		assertEquals(25, prop.getProperty("vslinerace.lockDelay.1", -1));

		VSLineRaceMode dest = new VSLineRaceMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(60, destEngine.speed.denominator);
		assertEquals(12, destEngine.speed.are);
		assertEquals(11, destEngine.speed.areLine);
		assertEquals(5, destEngine.speed.lineDelay);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(9, destEngine.speed.das);
	}

	@Test
	void presetSlotsAreScopedByNumberSoTwoPresetsDoNotCollide() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		engine.speed.gravity = 1;
		invokeSavePreset(mode, engine, prop, 0);
		engine.speed.gravity = 999;
		invokeSavePreset(mode, engine, prop, 1);

		assertEquals(1, prop.getProperty("vslinerace.gravity.0", -1));
		assertEquals(999, prop.getProperty("vslinerace.gravity.1", -1));
	}

	private static GameEngine freshEngine(VSLineRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(VSLineRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = VSLineRaceMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(VSLineRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = VSLineRaceMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}
}
