package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link VSBattleMode}: registry surface,
 * the two-player setup, and the per-preset speed I/O round-trip under
 * the 'vsbattle.*' prefix. The full playerInit / settings UI / render
 * path needs an SDL renderer plus the matching engine pair, so they
 * stay out of scope here.
 */
class VSBattleModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("VS-BATTLE", new VSBattleMode().getName());
	}

	@Test
	void getPlayersIsTwoSoTheModeRegistryRequestsTwoEngines() {
		assertEquals(2, new VSBattleMode().getPlayers());
	}

	@Test
	void modeIsTetrominoStyle() {
		// VS-BATTLE inherits the AbstractMode default getGameStyle so the
		// mode registry uses the standard TETROMINO field layout.
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new VSBattleMode().getGameStyle());
	}

	@Test
	void loadPresetReadsAllSpeedKeysFromTheGivenPreset() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("vsbattle.gravity.5", 64);
		prop.setProperty("vsbattle.denominator.5", 256);
		prop.setProperty("vsbattle.are.5", 30);
		prop.setProperty("vsbattle.areLine.5", 25);
		prop.setProperty("vsbattle.lineDelay.5", 40);
		prop.setProperty("vsbattle.lockDelay.5", 30);
		prop.setProperty("vsbattle.das.5", 14);

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
		VSBattleMode mode = new VSBattleMode();
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
	void savePresetAndLoadPresetRoundTripUnderVSBattlePrefix() throws Exception {
		VSBattleMode source = new VSBattleMode();
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

		assertEquals(99, prop.getProperty("vsbattle.gravity.1", -1));
		assertEquals(60, prop.getProperty("vsbattle.denominator.1", -1));
		assertEquals(25, prop.getProperty("vsbattle.lockDelay.1", -1));

		VSBattleMode dest = new VSBattleMode();
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
		// loadPreset / savePreset key on '<key>.<presetNumber>', so two
		// distinct preset numbers must keep separate values in the same
		// CustomProperties.
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		engine.speed.gravity = 1;
		invokeSavePreset(mode, engine, prop, 0);
		engine.speed.gravity = 999;
		invokeSavePreset(mode, engine, prop, 1);

		assertEquals(1, prop.getProperty("vsbattle.gravity.0", -1));
		assertEquals(999, prop.getProperty("vsbattle.gravity.1", -1));
	}

	@Test
	void saveOtherSettingWritesAllFieldsUnderVSBattlePrefix() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);
		setIntArray(mode, "garbagePercent")[0] = 80;
		setIntArray(mode, "garbageType")[0] = 1;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(mode, engine, prop);

		assertEquals(80, prop.getProperty("vsbattle.garbagePercent.p0", -1));
		assertEquals(1, prop.getProperty("vsbattle.garbageType.p0", -1));
	}

	@Test
	void saveAndLoadOtherSettingRoundTripForPlayer0() throws Exception {
		VSBattleMode source = new VSBattleMode();
		GameEngine sourceEngine = freshEngine(source);
		setIntArray(source, "garbagePercent")[0] = 65;
		setBoolArray(source, "garbageCounter")[0] = false;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		VSBattleMode dest = new VSBattleMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadOther(dest, destEngine, prop);

		assertEquals(65, getIntArray(dest, "garbagePercent")[0]);
		assertFalse(getBoolArray(dest, "garbageCounter")[0]);
	}

	@Test
	void loadOtherSettingLegacyVersionUsesGlobalKeyForGarbageType() throws Exception {
		// version < 5 reads 'vsbattle.garbageType' (no player suffix) — legacy format
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);
		setIntField(mode, "version", 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("vsbattle.garbageType", 1);  // GARBAGE_TYPE_NOCHANGE_ONE_RISE

		invokeLoadOther(mode, engine, prop);

		assertEquals(1, getIntArray(mode, "garbageType")[0],
				"legacy key 'vsbattle.garbageType' (no player suffix) must be read when version < 5");
	}

	@Test
	void loadOtherSettingLegacyVersionConvertsEnableB2BFlagToB2bType() throws Exception {
		// version < 5 reads boolean 'vsbattle.enableB2B.p<n>' and maps false -> b2bType=0
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);
		setIntField(mode, "version", 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("vsbattle.enableB2B.p0", false);

		invokeLoadOther(mode, engine, prop);

		assertEquals(0, getIntArray(mode, "b2bType")[0],
				"false enableB2B maps to b2bType=0 in legacy format");
	}

	private static GameEngine freshEngine(VSBattleMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(VSBattleMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = VSBattleMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(VSBattleMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = VSBattleMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeLoadOther(VSBattleMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = VSBattleMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(VSBattleMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = VSBattleMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static int[] setIntArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (int[]) f.get(obj);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		return setIntArray(obj, name);
	}

	private static boolean[] setBoolArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (boolean[]) f.get(obj);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		return setBoolArray(obj, name);
	}

	private static void setIntField(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
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
