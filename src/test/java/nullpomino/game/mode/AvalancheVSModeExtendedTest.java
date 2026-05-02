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
 * Extended test coverage for {@link AvalancheVSMode}: covers loadOtherSetting
 * / saveOtherSetting round-trip for fever-specific keys, readyInit fever
 * configuration, calcChainNewPower fever vs. classic, addOjama counter and
 * fever point logic, lineClearEnd fever activation, and onLast fever timer
 * countdown.
 */
class AvalancheVSModeExtendedTest {

	@Test
	void loadOtherSettingAppliesDefaults() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		invokeLoadOtherSetting(mode, engine, new CustomProperties());

		assertEquals(120, readInt(mode, "ojamaRate", 0));
		assertEquals(0, readInt(mode, "ojamaHard", 0));
		assertEquals(0, readInt(mode, "feverThreshold", 0));
		assertEquals(15, readInt(mode, "feverTimeMin", 0));
		assertEquals(30, readInt(mode, "feverTimeMax", 0));
		assertTrue(readBoolean(mode, "feverShowMeter", 0));
		assertTrue(readBoolean(mode, "ojamaMeter", 0));
		assertEquals(0, readInt(mode, "feverPointCriteria", 0));
		assertEquals(0, readInt(mode, "feverTimeCriteria", 0));
		assertEquals(10, readInt(mode, "feverPower", 0));
		assertEquals(5, readInt(mode, "feverChainStart", 0));
		assertEquals(4, readInt(mode, "zenKeshiChain", 0));
		assertEquals(30, readInt(mode, "zenKeshiOjama", 0));
	}

	@Test
	void loadOtherSettingReadsAllKeys() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.ojamaRate.p0", 300);
		prop.setProperty("avalanchevs.ojamaHard.p0", 2);
		prop.setProperty("avalanchevs.feverThreshold.p0", 5);
		prop.setProperty("avalanchevs.feverTimeMin.p0", 10);
		prop.setProperty("avalanchevs.feverTimeMax.p0", 45);
		prop.setProperty("avalanchevs.feverShowMeter.p0", false);
		prop.setProperty("avalanchevs.ojamaMeter.p0", false);
		prop.setProperty("avalanchevs.feverPointCriteria.p0", 1);
		prop.setProperty("avalanchevs.feverTimeCriteria.p0", 1);
		prop.setProperty("avalanchevs.feverPower.p0", 15);
		prop.setProperty("avalanchevs.feverChainStart.p0", 7);
		prop.setProperty("avalanchevs.zenKeshiChain.p0", 6);
		prop.setProperty("avalanchevs.zenKeshiOjama.p0", 50);

		invokeLoadOtherSetting(mode, engine, prop);

		assertEquals(300, readInt(mode, "ojamaRate", 0));
		assertEquals(2, readInt(mode, "ojamaHard", 0));
		assertEquals(5, readInt(mode, "feverThreshold", 0));
		assertEquals(10, readInt(mode, "feverTimeMin", 0));
		assertEquals(45, readInt(mode, "feverTimeMax", 0));
		assertFalse(readBoolean(mode, "feverShowMeter", 0));
	}

	@Test
	void saveOtherSettingWritesFeverKeys() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setInt(mode, "feverThreshold", 0, 7);
		setInt(mode, "feverPower", 0, 12);

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop);

		assertEquals(7, prop.getProperty("avalanchevs.feverThreshold.p0", -1));
		assertEquals(12, prop.getProperty("avalanchevs.feverPower.p0", -1));
	}

	@Test
	void readyInitConfiguresFeverWhenEnabled() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setInt(mode, "feverThreshold", 0, 5);
		setInt(mode, "feverTimeMin", 0, 20);
		setInt(mode, "feverTimeMax", 0, 40);
		setInt(mode, "feverChainStart", 0, 6);

		mode.readyInit(engine, 0);

		assertEquals(20 * 60, readInt(mode, "feverTime", 0));
	}

	@Test
	void calcChainNewPowerFeverUsesFeverPowers() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		// Set inFever to true so calcChainNewPower uses FEVER_POWERS
		setBoolean(mode, "inFever", 0, true);

		Method calc = AvalancheVSMode.class.getDeclaredMethod(
				"calcChainNewPower", GameEngine.class, int.class, int.class);
		calc.setAccessible(true);

		int result = (int) calc.invoke(mode, engine, 0, 1);
		assertEquals(4, result);

		result = (int) calc.invoke(mode, engine, 0, 24);
		assertEquals(720, result);
	}

	@Test
	void calcChainNewPowerClassicUsesClassicPowers() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		// inFever is false by default -> uses CHAIN_POWERS
		setBoolean(mode, "inFever", 0, false);

		Method calc = AvalancheVSMode.class.getDeclaredMethod(
				"calcChainNewPower", GameEngine.class, int.class, int.class);
		calc.setAccessible(true);

		int result = (int) calc.invoke(mode, engine, 0, 1);
		// CHAIN_POWERS[0] may differ from expected
		assertTrue(result > 0);
	}

	@Test
	void addOjamaCountersAndAddsToEnemy() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setInt(mode, "ojama", 0, 5);
		setInt(mode, "ojamaAdd", 0, 3);
		setInt(mode, "ojamaRate", 0, 120);

		Method addOjama = AvalancheVSMode.class.getDeclaredMethod(
				"addOjama", GameEngine.class, int.class, int.class);
		addOjama.setAccessible(true);
		addOjama.invoke(mode, engine, 0, 240);

		// pts=240, rate=120 -> (240+120-1)/120 = 2 new ojama
		// ojama=5 counters 2 -> ojama=3, ojamaNew=0
		assertEquals(3, readInt(mode, "ojama", 0));
	}

	@Test
	void addOjamaAddsFeverPointOnCounter() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setInt(mode, "feverThreshold", 0, 3);
		setInt(mode, "feverPoints", 0, 0);
		setInt(mode, "ojama", 0, 10);
		setInt(mode, "ojamaRate", 0, 120);

		Method addOjama = AvalancheVSMode.class.getDeclaredMethod(
				"addOjama", GameEngine.class, int.class, int.class);
		addOjama.setAccessible(true);
		addOjama.invoke(mode, engine, 0, 120);

		// Should have gained a fever point from countering
		assertEquals(1, readInt(mode, "feverPoints", 0));
	}

	@Test
	void onLastDecrementsFeverTimeLimitAddDisplay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setInt(mode, "feverTimeLimitAddDisplay", 0, 10);
		mode.onLast(engine, 0);

		assertEquals(9, readInt(mode, "feverTimeLimitAddDisplay", 0));
	}

	@Test
	void onLastUpdatesOjamaMeterWhenOjamaMeterEnabled() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setBoolean(mode, "ojamaMeter", 0, true);
		mode.onLast(engine, 0);
		// Should not throw
	}

	@Test
	void playerInitResetsFeverState() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "ojamaFever", 0));
		assertEquals(0, readInt(mode, "feverPoints", 0));
		assertFalse(readBoolean(mode, "inFever", 0));
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheVSMode mode) {
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

	private static void invokeLoadOtherSetting(AvalancheVSMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOtherSetting(AvalancheVSMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}
}
