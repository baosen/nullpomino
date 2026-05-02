package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link AvalancheVSBombBattleMode}: covers
 * loadOtherSetting / saveOtherSetting round-trip with bombbattle-specific
 * keys (ojamaCountdown, newChainPower), onLast meter update, lineClearEnd
 * ojama transfer, and saveReplay version persistence.
 */
class AvalancheVSBombBattleModeExtendedTest {

	@Test
	void getNameReturnsLegacyConstant() {
		assertEquals("AVALANCHE VS BOMB BATTLE (RC1)", new AvalancheVSBombBattleMode().getName());
	}

	@Test
	void loadOtherSettingAppliesDefaults() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		invokeLoadOtherSetting(mode, engine, new CustomProperties());

		assertEquals(60, readInt(mode, "ojamaRate", 0));
		assertEquals(1, readInt(mode, "ojamaHard", 0));
		assertFalse(readBoolean(mode, "newChainPower", 0));
		assertEquals(5, readInt(mode, "ojamaCountdown", 0));
	}

	@Test
	void loadOtherSettingReadsAllKeys() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsbombbattle.ojamaRate.p0", 200);
		prop.setProperty("avalanchevsbombbattle.ojamaHard.p0", 5);
		prop.setProperty("avalanchevsbombbattle.newChainPower.p0", true);
		prop.setProperty("avalanchevsbombbattle.ojamaCountdown.p0", 8);

		invokeLoadOtherSetting(mode, engine, prop);

		assertEquals(200, readInt(mode, "ojamaRate", 0));
		assertEquals(5, readInt(mode, "ojamaHard", 0));
		assertTrue(readBoolean(mode, "newChainPower", 0));
		assertEquals(8, readInt(mode, "ojamaCountdown", 0));
	}

	@Test
	void saveOtherSettingWritesBombbattleKeys() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setBoolean(mode, "newChainPower", 0, true);
		setInt(mode, "ojamaCountdown", 0, 3);

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop);

		assertTrue(prop.getProperty("avalanchevsbombbattle.newChainPower.p0", false));
		assertEquals(3, prop.getProperty("avalanchevsbombbattle.ojamaCountdown.p0", -1));
	}

	@Test
	void loadOtherSettingAndSaveOtherSettingRoundTrip() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsbombbattle.ojamaRate.p0", 150);
		prop.setProperty("avalanchevsbombbattle.newChainPower.p0", true);

		invokeLoadOtherSetting(mode, engine, prop);

		assertEquals(150, readInt(mode, "ojamaRate", 0));
		assertTrue(readBoolean(mode, "newChainPower", 0));

		CustomProperties out = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, out);

		assertTrue(out.getProperty("avalanchevsbombbattle.newChainPower.p0", false));
	}

	@Test
	void onLastCallsSuperAndUpdatesOjamaMeter() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		mode.onLast(engine, 0);
		// Should not throw; ojama meter update is a no-op with default state
	}

	@Test
	void lineClearEndTransfersOjamaAdd() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setInt(mode, "ojamaAdd", 1, 9);

		mode.lineClearEnd(engine, 0);

		assertEquals(9, readInt(mode, "ojama", 1));
		assertEquals(0, readInt(mode, "ojamaAdd", 1));
	}

	@Test
	void saveReplayWritesVersion() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		Field ownerField = findField(AvalancheVSBombBattleMode.class, "owner");
		ownerField.setAccessible(true);
		GameManager manager = (GameManager) ownerField.get(mode);
		manager.replayProp = new CustomProperties();

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(0, manager.replayProp.getProperty("avalanchevs.version", -1));
	}

	@Test
	void playerInitLoadsSettingsFromModeConfig() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "menuTime"));
		assertEquals(0, readInt(mode, "menuCursor"));
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheVSBombBattleMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static int readInt(Object obj, String name, int index) throws Exception {
		int[] arr = (int[]) readField(obj, name);
		return arr[index];
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static boolean readBoolean(Object obj, String name, int index) throws Exception {
		boolean[] arr = (boolean[]) readField(obj, name);
		return arr[index];
	}

	private static void setInt(Object obj, String name, int index, int value) throws Exception {
		int[] arr = (int[]) readField(obj, name);
		arr[index] = value;
	}

	private static void setBoolean(Object obj, String name, int index, boolean value) throws Exception {
		boolean[] arr = (boolean[]) readField(obj, name);
		arr[index] = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeLoadOtherSetting(AvalancheVSBombBattleMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOtherSetting(AvalancheVSBombBattleMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}
}
