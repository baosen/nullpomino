package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link VSDigRaceMode}: registry surface,
 * the two-player count, and the per-preset speed I/O round-trip under
 * the 'vsdigrace.*' prefix.
 */
class VSDigRaceModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("VS-DIG RACE", new VSDigRaceMode().getName());
	}

	@Test
	void getPlayersIsTwoSoTheModeRegistryRequestsTwoEngines() {
		assertEquals(2, new VSDigRaceMode().getPlayers());
	}

	@Test
	void modeIsTetrominoStyle() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new VSDigRaceMode().getGameStyle());
	}

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(new VSDigRaceMode().isVSMode());
	}

	@Test
	void loadOtherSettingReadsDefaultsWhenPropertyIsEmpty() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadOther(mode, engine, new CustomProperties());

		assertEquals(18, getIntArray(mode, "goalLines")[0]);
		assertEquals(100, getIntArray(mode, "garbagePercent")[0]);
		assertEquals(0, getIntField(mode, "bgmno"));
		assertTrue(getBoolArray(mode, "enableSE")[0]);
		assertEquals(0, getIntArray(mode, "presetNumber")[0]);
	}

	@Test
	void saveAndLoadOtherSettingRoundTripForPlayer0() throws Exception {
		VSDigRaceMode source = new VSDigRaceMode();
		GameEngine sourceEngine = freshEngine(source);
		getIntArray(source, "goalLines")[0] = 25;
		getIntArray(source, "garbagePercent")[0] = 75;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		assertEquals(25, prop.getProperty("vsdigrace.goalLines.p0", -1));
		assertEquals(75, prop.getProperty("vsdigrace.garbagePercent.p0", -1));

		VSDigRaceMode dest = new VSDigRaceMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadOther(dest, destEngine, prop);

		assertEquals(25, getIntArray(dest, "goalLines")[0]);
		assertEquals(75, getIntArray(dest, "garbagePercent")[0]);
	}

	@Test
	void loadPresetReadsAllSpeedKeysFromTheGivenPreset() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("vsdigrace.gravity.5", 64);
		prop.setProperty("vsdigrace.denominator.5", 256);
		prop.setProperty("vsdigrace.are.5", 30);
		prop.setProperty("vsdigrace.areLine.5", 25);
		prop.setProperty("vsdigrace.lineDelay.5", 40);
		prop.setProperty("vsdigrace.lockDelay.5", 30);
		prop.setProperty("vsdigrace.das.5", 14);

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
		VSDigRaceMode mode = new VSDigRaceMode();
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
	void savePresetAndLoadPresetRoundTripUnderVSDigRacePrefix() throws Exception {
		VSDigRaceMode source = new VSDigRaceMode();
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

		assertEquals(99, prop.getProperty("vsdigrace.gravity.1", -1));
		assertEquals(60, prop.getProperty("vsdigrace.denominator.1", -1));
		assertEquals(25, prop.getProperty("vsdigrace.lockDelay.1", -1));

		VSDigRaceMode dest = new VSDigRaceMode();
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
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		engine.speed.gravity = 1;
		invokeSavePreset(mode, engine, prop, 0);
		engine.speed.gravity = 999;
		invokeSavePreset(mode, engine, prop, 1);

		assertEquals(1, prop.getProperty("vsdigrace.gravity.0", -1));
		assertEquals(999, prop.getProperty("vsdigrace.gravity.1", -1));
	}

	private static GameEngine freshEngine(VSDigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(VSDigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(VSDigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeLoadOther(VSDigRaceMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(VSDigRaceMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (int[]) f.get(obj);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (boolean[]) f.get(obj);
	}

	private static int getIntField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
