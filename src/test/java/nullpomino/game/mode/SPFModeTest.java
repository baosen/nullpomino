package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link SPFMode}: registry surface (SPF
 * style + 2-player count), and the per-preset speed I/O round-trip
 * under the 'spfvs.*' prefix.
 */
class SPFModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("SPF VS-BATTLE (BETA)", new SPFMode().getName());
	}

	@Test
	void getPlayersIsTwoSoTheModeRegistryRequestsTwoEngines() {
		assertEquals(2, new SPFMode().getPlayers());
	}

	@Test
	void getGameStyleIsSPFForTheRainbowGemFieldLayout() {
		assertEquals(GameEngine.GAMESTYLE_SPF, new SPFMode().getGameStyle());
	}

	@Test
	void loadPresetReadsAllSpeedKeysFromTheGivenPreset() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("spfvs.gravity.5", 64);
		prop.setProperty("spfvs.denominator.5", 256);
		prop.setProperty("spfvs.are.5", 30);
		prop.setProperty("spfvs.areLine.5", 25);
		prop.setProperty("spfvs.lineDelay.5", 40);
		prop.setProperty("spfvs.lockDelay.5", 30);
		prop.setProperty("spfvs.das.5", 14);

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
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(24, engine.speed.are,
				"SPF defaults ARE to 24 (slower than tetromino's 0, matching PHYSICIAN-VS)");
		assertEquals(24, engine.speed.areLine);
		assertEquals(10, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderSPFVSPrefix() throws Exception {
		SPFMode source = new SPFMode();
		GameEngine sourceEngine = freshEngine(source);
		sourceEngine.speed.gravity = 99;
		sourceEngine.speed.are = 12;
		sourceEngine.speed.lockDelay = 25;
		sourceEngine.speed.das = 9;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, sourceEngine, prop, 1);

		assertEquals(99, prop.getProperty("spfvs.gravity.1", -1));
		assertEquals(12, prop.getProperty("spfvs.are.1", -1));
		assertEquals(25, prop.getProperty("spfvs.lockDelay.1", -1));
		assertEquals(9, prop.getProperty("spfvs.das.1", -1));

		SPFMode dest = new SPFMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(12, destEngine.speed.are);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(9, destEngine.speed.das);
	}

	@Test
	void presetSlotsAreScopedByNumberSoTwoPresetsDoNotCollide() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		engine.speed.gravity = 1;
		invokeSavePreset(mode, engine, prop, 0);
		engine.speed.gravity = 999;
		invokeSavePreset(mode, engine, prop, 1);

		assertEquals(1, prop.getProperty("spfvs.gravity.0", -1));
		assertEquals(999, prop.getProperty("spfvs.gravity.1", -1));
	}

	private static GameEngine freshEngine(SPFMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(SPFMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(SPFMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}
}
