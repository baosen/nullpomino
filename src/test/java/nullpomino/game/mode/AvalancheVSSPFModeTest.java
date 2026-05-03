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
 * Pins the headless slice of {@link AvalancheVSSPFMode}: registry surface,
 * modeInit / playerInit array allocation, loadOtherSetting / saveOtherSetting
 * round-trip, drop-map multipliers, ptsToOjama conversion, onClear logic,
 * onMove flags, and lineClearEnd countdown/game-over handling.
 */
class AvalancheVSSPFModeTest {

	@Test
	void getNameReturnsLegacyConstant() {
		assertEquals("AVALANCHE-SPF VS-BATTLE (BETA)", new AvalancheVSSPFMode().getName());
	}

	@Test
	void getPlayersReturnsTwo() {
		assertEquals(2, new AvalancheVSSPFMode().getPlayers());
	}

	@Test
	void modeInitAllocatesPerPlayerArrays() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		assertEquals(2, ((int[]) readField(mode, "ojamaCountdown")).length);
		assertEquals(2, ((int[]) readField(mode, "dropSet")).length);
		assertEquals(2, ((int[]) readField(mode, "dropMap")).length);
		assertEquals(2, ((int[][][]) readField(mode, "dropPattern")).length);
		assertEquals(2, ((double[]) readField(mode, "attackMultiplier")).length);
		assertEquals(2, ((double[]) readField(mode, "defendMultiplier")).length);
		assertEquals(2, ((boolean[]) readField(mode, "countdownDecremented")).length);
		assertEquals(2, ((boolean[]) readField(mode, "ojamaChecked")).length);
	}

	@Test
	void playerInitResetsState() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.owner.modeConfig = new CustomProperties();

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "menuTime"));
		assertTrue(readInt(mode, "numColors", 0) >= 4);
		assertEquals(4, readInt(mode, "ojamaHard", 0));
		assertTrue(readBoolean(mode, "countdownDecremented", 0));
		assertFalse(readBoolean(mode, "ojamaChecked", 0));
	}

	@Test
	void loadOtherSettingAppliesDefaults() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		invokeLoadOtherSetting(mode, engine, new CustomProperties());

		assertEquals(120, readInt(mode, "ojamaRate", 0));
		assertEquals(3, readInt(mode, "ojamaCountdown", 0));
		assertEquals(4, readInt(mode, "dropSet", 0));
		assertEquals(0, readInt(mode, "dropMap", 0));
	}

	@Test
	void loadOtherSettingAndSaveOtherSettingRoundTrip() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsspf.ojamaRate.p0", 300);
		prop.setProperty("avalanchevsspf.ojamaCountdown.p0", 7);
		prop.setProperty("avalanchevsspf.dropSet.p0", 2);
		prop.setProperty("avalanchevsspf.dropMap.p0", 3);

		invokeLoadOtherSetting(mode, engine, prop);

		assertEquals(300, readInt(mode, "ojamaRate", 0));
		assertEquals(7, readInt(mode, "ojamaCountdown", 0));
		assertEquals(2, readInt(mode, "dropSet", 0));
		assertEquals(3, readInt(mode, "dropMap", 0));

		CustomProperties out = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, out);

		assertEquals(7, out.getProperty("avalanchevsspf.ojamaCountdown.p0", -1));
		assertEquals(2, out.getProperty("avalanchevsspf.dropSet.p0", -1));
		assertEquals(3, out.getProperty("avalanchevsspf.dropMap.p0", -1));
	}

	@Test
	void onMoveResetsClearedAndOjamaDropAndCountdownDecremented() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setBoolean(mode, "cleared", 0, true);
		setBoolean(mode, "ojamaDrop", 0, true);
		setBoolean(mode, "countdownDecremented", 0, true);

		mode.onMove(engine, 0);

		assertFalse(readBoolean(mode, "cleared", 0));
		assertFalse(readBoolean(mode, "ojamaDrop", 0));
		assertFalse(readBoolean(mode, "countdownDecremented", 0));
	}

	@Test
	void onClearSetsOjamaCheckedToFalse() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setBoolean(mode, "ojamaChecked", 0, true);

		Method onClear = AvalancheVSSPFMode.class.getDeclaredMethod(
				"onClear", GameEngine.class, int.class);
		onClear.setAccessible(true);
		onClear.invoke(mode, engine, 0);

		assertFalse(readBoolean(mode, "ojamaChecked", 0));
	}

	@Test
	void getAttackMultiplierReturnsCorrectValues() {
		// Set 0 (CLASSIC), default map 0 -> 1.0
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(0, 0), 0.001);
		// Set 1 (REMIX), map 0 -> 1.0
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(1, 0), 0.001);
		// Set 1, map 9 (last) -> 0.85
		assertEquals(0.85, AvalancheVSSPFMode.getAttackMultiplier(1, 9), 0.001);
		// Out of bounds -> 1.0
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(99, 99), 0.001);
	}

	@Test
	void getDefendMultiplierReturnsCorrectValues() {
		// Set 0 -> 1.0 everywhere
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(0, 0), 0.001);
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(0, 10), 0.001);
		// Set 1 -> map 8 = 1.2
		assertEquals(1.2, AvalancheVSSPFMode.getDefendMultiplier(1, 8), 0.001);
		// Out of bounds -> 1.0
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(99, 99), 0.001);
	}

	@Test
	void ptsToOjamaUsesAttackAndDefendMultipliers() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		// Set attack and defend multipliers
		setDouble(mode, "attackMultiplier", 0, 1.5);
		setDouble(mode, "defendMultiplier", 1, 2.0);

		Method ptsToOjama = AvalancheVSSPFMode.class.getDeclaredMethod(
				"ptsToOjama", GameEngine.class, int.class, int.class, int.class);
		ptsToOjama.setAccessible(true);

		// pts=100, rate=120 -> (100 * 1.5 * 2.0 + 120 - 1) / 120 = (300 + 119) / 120 = 3
		int result = (int) ptsToOjama.invoke(mode, engine, 0, 100, 120);
		assertEquals(3, result);
	}

	@Test
	void ptsToOjamaWithDefaultMultipliers() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setDouble(mode, "attackMultiplier", 0, 1.0);
		setDouble(mode, "defendMultiplier", 1, 1.0);

		Method ptsToOjama = AvalancheVSSPFMode.class.getDeclaredMethod(
				"ptsToOjama", GameEngine.class, int.class, int.class, int.class);
		ptsToOjama.setAccessible(true);

		// pts=120, rate=120 -> (120 * 1.0 * 1.0 + 119) / 120 = 1
		int result = (int) ptsToOjama.invoke(mode, engine, 0, 120, 120);
		assertEquals(1, result);

		// pts=240, rate=120 -> (240 + 119) / 120 = 2
		result = (int) ptsToOjama.invoke(mode, engine, 0, 240, 120);
		assertEquals(2, result);
	}

	@Test
	void lineClearEndTransfersOjamaAddToOjama() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setInt(mode, "ojamaAdd", 1, 5);

		mode.lineClearEnd(engine, 0);

		assertEquals(5, readInt(mode, "ojama", 1));
		assertEquals(0, readInt(mode, "ojamaAdd", 1));
	}

	@Test
	void readyInitSetsDropPatternAndMultipliers() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setInt(mode, "dropSet", 0, 0);
		setInt(mode, "dropMap", 0, 0);

		mode.readyInit(engine, 0);

		assertEquals(1.0, readDouble(mode, "attackMultiplier", 0), 0.001);
		assertEquals(1.0, readDouble(mode, "defendMultiplier", 0), 0.001);
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheVSSPFMode mode) {
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

	private static double readDouble(Object obj, String name, int index) throws Exception {
		double[] arr = (double[]) readField(obj, name);
		return arr[index];
	}

	private static boolean readBoolean(Object obj, String name, int index) throws Exception {
		boolean[] arr = (boolean[]) readField(obj, name);
		return arr[index];
	}

	private static void setInt(Object obj, String name, int index, int value) throws Exception {
		int[] arr = (int[]) readField(obj, name);
		arr[index] = value;
	}

	private static void setDouble(Object obj, String name, int index, double value) throws Exception {
		double[] arr = (double[]) readField(obj, name);
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

	private static void invokeLoadOtherSetting(AvalancheVSSPFMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSSPFMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOtherSetting(AvalancheVSSPFMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSSPFMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}
}
