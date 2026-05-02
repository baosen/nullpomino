package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSFeverMode}'s private {@code loadOtherSetting}
 * / {@code saveOtherSetting} per-player persistence under the
 * {@code avalanchevsfever.*} prefix. Loadable defaults: ojamaRate=120
 * (matches base AvalancheVS), ojamaHard=0, ojamaHandicap=270 (the
 * Fever-specific big initial garbage budget), feverChainStart=5.
 * saveOtherSetting writes only ojamaHandicap and feverChainStart
 * directly; ojamaRate/ojamaHard pass through {@code super.saveOtherSetting}
 * with the "fever" suffix.
 */
class AvalancheVSFeverModeOtherSettingTest {

	@Test
	void loadOtherSettingReadsDocumentedDefaults() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		invokeLoadOther(mode, engine, new CustomProperties());

		assertEquals(120, getIntArray(mode, "ojamaRate")[0],
				"ojamaRate default 120 (same as base AvalancheVS)");
		assertEquals(0, getIntArray(mode, "ojamaHard")[0],
				"ojamaHard default 0");
		assertEquals(270, getIntArray(mode, "ojamaHandicap")[0],
				"ojamaHandicap default 270 (Fever-specific garbage budget)");
		assertEquals(5, getIntArray(mode, "feverChainStart")[0],
				"feverChainStart default 5");
	}

	@Test
	void loadOtherSettingReadsFeverKeysFromProperties() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsfever.ojamaRate.p0", 200);
		prop.setProperty("avalanchevsfever.ojamaHard.p0", 2);
		prop.setProperty("avalanchevsfever.ojamaHandicap.p0", 500);
		prop.setProperty("avalanchevsfever.feverChainStart.p0", 9);

		invokeLoadOther(mode, engine, prop);

		assertEquals(200, getIntArray(mode, "ojamaRate")[0]);
		assertEquals(2, getIntArray(mode, "ojamaHard")[0]);
		assertEquals(500, getIntArray(mode, "ojamaHandicap")[0]);
		assertEquals(9, getIntArray(mode, "feverChainStart")[0]);
	}

	@Test
	void saveOtherSettingWritesFeverHandicapAndChainStartButNotRateOrHard() throws Exception {
		// Subclass-only saveOtherSetting writes ojamaHandicap and
		// feverChainStart directly. ojamaRate / ojamaHard go through
		// super.saveOtherSetting with the "fever" suffix.
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 1);

		getIntArray(mode, "ojamaHandicap")[1] = 600;
		getIntArray(mode, "feverChainStart")[1] = 7;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(mode, engine, prop);

		assertEquals(600, prop.getProperty(
				"avalanchevsfever.ojamaHandicap.p1", -1));
		assertEquals(7, prop.getProperty(
				"avalanchevsfever.feverChainStart.p1", -1));
	}

	@Test
	void saveAndLoadOtherSettingRoundTripFeverSpecificFields() throws Exception {
		AvalancheVSFeverMode source = new AvalancheVSFeverMode();
		GameManager sourceMgr = new GameManager(new EventReceiver());
		source.modeInit(sourceMgr);
		GameEngine sourceEngine = freshEngine(source, sourceMgr, 0);

		getIntArray(source, "ojamaHandicap")[0] = 333;
		getIntArray(source, "feverChainStart")[0] = 8;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		AvalancheVSFeverMode dest = new AvalancheVSFeverMode();
		GameManager destMgr = new GameManager(new EventReceiver());
		dest.modeInit(destMgr);
		GameEngine destEngine = freshEngine(dest, destMgr, 0);

		invokeLoadOther(dest, destEngine, prop);

		assertEquals(333, getIntArray(dest, "ojamaHandicap")[0],
				"Fever-specific ojamaHandicap round-trips");
		assertEquals(8, getIntArray(dest, "feverChainStart")[0],
				"Fever-specific feverChainStart round-trips");
	}

	private static GameEngine freshEngine(AvalancheVSFeverMode mode,
			GameManager manager, int playerID) {
		manager.mode = mode;
		manager.init();
		manager.engine[playerID].init();
		return manager.engine[playerID];
	}

	private static void invokeLoadOther(AvalancheVSFeverMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(AvalancheVSFeverMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (int[]) f.get(obj);
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
