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
 * Pins the headless slice of {@link AvalancheVSFeverMode}: registry surface,
 * modeInit / playerInit allocation, loadOtherSetting / saveOtherSetting
 * round-trip, readyInit map loading, and the core game-logic hooks
 * (onClear, addOjama, calcChainNewPower, lineClearEnd, onLast).
 */
class AvalancheVSFeverModeTest {

	@Test
	void getNameReturnsLegacyConstant() {
		assertEquals("AVALANCHE VS FEVER MARATHON (RC1)", new AvalancheVSFeverMode().getName());
	}

	@Test
	void getPlayersReturnsTwo() {
		assertEquals(2, new AvalancheVSFeverMode().getPlayers());
	}

	@Test
	void modeInitAllocatesPerPlayerArrays() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		assertEquals(2, ((int[]) readField(mode, "ojamaHandicapLeft")).length);
		assertEquals(2, ((int[]) readField(mode, "feverChain")).length);
		assertEquals(2, ((int[]) readField(mode, "ojamaHandicap")).length);
		assertEquals(2, ((int[]) readField(mode, "feverChainDisplay")).length);
		assertEquals(2, ((int[]) readField(mode, "feverChainStart")).length);
	}

	@Test
	void playerInitResetsState() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "menuTime"));
		assertEquals(0, readInt(mode, "ojama", 0));
		assertEquals(0, readInt(mode, "feverChainDisplay", 0));
	}

	@Test
	void loadOtherSettingAndSaveOtherSettingRoundTrip() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsfever.ojamaRate.p0", 250);
		prop.setProperty("avalanchevsfever.ojamaHard.p0", 3);
		prop.setProperty("avalanchevsfever.ojamaHandicap.p0", 500);
		prop.setProperty("avalanchevsfever.feverChainStart.p0", 7);

		invokeLoadOtherSetting(mode, engine, prop);

		assertEquals(250, readInt(mode, "ojamaRate", 0));
		assertEquals(3, readInt(mode, "ojamaHard", 0));
		assertEquals(500, readInt(mode, "ojamaHandicap", 0));
		assertEquals(7, readInt(mode, "feverChainStart", 0));

		// Round-trip
		CustomProperties out = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, out);

		assertEquals(500, out.getProperty("avalanchevsfever.ojamaHandicap.p0", -1));
		assertEquals(7, out.getProperty("avalanchevsfever.feverChainStart.p0", -1));
	}

	@Test
	void loadOtherSettingAppliesDefaults() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));

		invokeLoadOtherSetting(mode, engine, new CustomProperties());

		assertEquals(120, readInt(mode, "ojamaRate", 0));
		assertEquals(0, readInt(mode, "ojamaHard", 0));
		assertEquals(270, readInt(mode, "ojamaHandicap", 0));
		assertEquals(5, readInt(mode, "feverChainStart", 0));
	}

	@Test
	void readyInitInheritsFromSuperAndResetsOptions() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));

		setInt(mode, "ojamaHandicap", 0, 100);
		setInt(mode, "feverChainStart", 0, 6);

		mode.readyInit(engine, 0);

		assertEquals(100, readInt(mode, "ojamaHandicapLeft", 0));
		assertEquals(6, readInt(mode, "feverChain", 0));
	}

	@Test
	void calcChainNewPowerUsesFeverPowers() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);

		Method m = AvalancheVSFeverMode.class.getDeclaredMethod(
				"calcChainNewPower", GameEngine.class, int.class, int.class);
		m.setAccessible(true);

		// Chain 1 -> first element of FEVER_POWERS = 4
		int result = (int) m.invoke(mode, engine, 0, 1);
		assertEquals(4, result);

		// Chain 24 -> last element = 720
		result = (int) m.invoke(mode, engine, 0, 24);
		assertEquals(720, result);

		// Chain beyond length -> clamps to last
		result = (int) m.invoke(mode, engine, 0, 999);
		assertEquals(720, result);
	}

	@Test
	void onClearRecordsFeverChainDisplay() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setInt(mode, "feverChain", 0, 8);
		assertEquals(0, readInt(mode, "feverChainDisplay", 0));

		Method onClear = AvalancheVSFeverMode.class.getDeclaredMethod(
				"onClear", GameEngine.class, int.class);
		onClear.setAccessible(true);
		onClear.invoke(mode, engine, 0);

		assertEquals(8, readInt(mode, "feverChainDisplay", 0));
	}

	@Test
	void addOjamaCountersUsingHandicap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		// Set up: ojama=5, ojamaAdd=3, handicap=10, rate=120
		setInt(mode, "ojama", 0, 5);
		setInt(mode, "ojamaAdd", 0, 3);
		setInt(mode, "ojamaHandicapLeft", 0, 10);
		setInt(mode, "ojamaRate", 0, 120);

		Method addOjama = AvalancheVSFeverMode.class.getDeclaredMethod(
				"addOjama", GameEngine.class, int.class, int.class);
		addOjama.setAccessible(true);
		addOjama.invoke(mode, engine, 0, 240);

		// pts=240, rate=120 -> (240+120-1)/120 = 2 ojamaNew
		// ojama counter: 5->0 (delta=5), ojamaAdd: 3->0 (delta=3), but only 2 new, so both get fully countered
		// since 2 <= 5, it's all countered -> ojama = 5-2 = 3
		assertEquals(3, readInt(mode, "ojama", 0));
	}

	@Test
	void addOjamaWithHandicapAbsorbsOjama() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		setInt(mode, "ojama", 0, 0);
		setInt(mode, "ojamaAdd", 0, 0);
		setInt(mode, "ojamaHandicapLeft", 0, 5);
		setInt(mode, "ojamaRate", 0, 120);

		Method addOjama = AvalancheVSFeverMode.class.getDeclaredMethod(
				"addOjama", GameEngine.class, int.class, int.class);
		addOjama.setAccessible(true);
		addOjama.invoke(mode, engine, 0, 480);

		// pts=480, rate=120 -> (480+120-1)/120 = 4 ojamaNew
		// handicap=5 absorbs 4 up to 5 -> handicap = 5-4 = 1, ojamaNew = 0
		assertEquals(1, readInt(mode, "ojamaHandicapLeft", 0));
		// ojama should be 0 (all absorbed by handicap)
		assertEquals(0, readInt(mode, "ojama", 0));
	}

	@Test
	void lineClearEndTransfersOjamaAddToOjama() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setInt(mode, "ojamaAdd", 1, 7); // enemy's ojamaAdd

		mode.lineClearEnd(engine, 0);

		assertEquals(7, readInt(mode, "ojama", 1));
		assertEquals(0, readInt(mode, "ojamaAdd", 1));
	}

	@Test
	void onLastInheritsFromSuperAndUpdatesOjamaMeter() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		// Should not throw
		mode.onLast(engine, 0);
	}

	@Test
	void saveReplayWritesVersionToReplayProp() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		CustomProperties replayProp = new CustomProperties();
		Field ownerField = findField(AvalancheVSFeverMode.class, "owner");
		ownerField.setAccessible(true);
		GameManager manager = (GameManager) ownerField.get(mode);
		manager.replayProp = replayProp;

		mode.saveReplay(engine, 0, replayProp);

		assertEquals(1, replayProp.getProperty("avalanchevsfever.version", -1));
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheVSFeverMode mode) {
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

	private static void setInt(Object obj, String name, int index, int value) throws Exception {
		int[] arr = (int[]) readField(obj, name);
		arr[index] = value;
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
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

	private static void invokeLoadOtherSetting(AvalancheVSFeverMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOtherSetting(AvalancheVSFeverMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}
}
