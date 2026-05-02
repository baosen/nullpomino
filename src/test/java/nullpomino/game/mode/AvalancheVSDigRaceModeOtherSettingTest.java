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
 * Pins {@link AvalancheVSDigRaceMode}'s private
 * {@code loadOtherSetting} / {@code saveOtherSetting} per-player
 * persistence under the {@code avalanchevsdigrace.*} prefix.
 * Loadable defaults: ojamaRate=420 (much higher than the base
 * AvalancheVS=120 — DigRace floods you with garbage to dig out from),
 * ojamaHard=0, handicapRows=6 (initial garbage rows). saveOtherSetting
 * writes only handicapRows (under the {@code ojamaHandicap} legacy key
 * name) directly; ojamaRate/ojamaHard pass through
 * {@code super.saveOtherSetting} with the "digrace" suffix.
 */
class AvalancheVSDigRaceModeOtherSettingTest {

	@Test
	void loadOtherSettingReadsDigRaceDefaults() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		invokeLoadOther(mode, engine, new CustomProperties());

		assertEquals(420, getIntArray(mode, "ojamaRate")[0],
				"DigRace ojamaRate default 420 (3.5× base AvalancheVS=120 — "
						+ "DigRace pours garbage on you)");
		assertEquals(0, getIntArray(mode, "ojamaHard")[0],
				"ojamaHard default 0");
		assertEquals(6, getIntArray(mode, "handicapRows")[0],
				"handicapRows default 6 (initial dig pile rows)");
	}

	@Test
	void loadOtherSettingReadsDigRaceKeysFromProperties() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsdigrace.ojamaRate.p0", 600);
		prop.setProperty("avalanchevsdigrace.ojamaHard.p0", 5);
		prop.setProperty("avalanchevsdigrace.ojamaHandicap.p0", 11);

		invokeLoadOther(mode, engine, prop);

		assertEquals(600, getIntArray(mode, "ojamaRate")[0]);
		assertEquals(5, getIntArray(mode, "ojamaHard")[0]);
		assertEquals(11, getIntArray(mode, "handicapRows")[0],
				"handicapRows reads from 'avalanchevsdigrace.ojamaHandicap' "
						+ "(legacy key name, not the field name)");
	}

	@Test
	void saveOtherSettingWritesHandicapRowsUnderLegacyOjamaHandicapKey() throws Exception {
		// Subclass-only saveOtherSetting writes handicapRows under the
		// 'ojamaHandicap' legacy key. ojamaRate/ojamaHard go through
		// super.saveOtherSetting with the "digrace" suffix.
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 1);

		getIntArray(mode, "handicapRows")[1] = 9;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(mode, engine, prop);

		assertEquals(9, prop.getProperty(
				"avalanchevsdigrace.ojamaHandicap.p1", -1),
				"handicapRows saves to legacy 'ojamaHandicap' key");
	}

	@Test
	void saveAndLoadOtherSettingRoundTripDigRaceSpecificFields() throws Exception {
		AvalancheVSDigRaceMode source = new AvalancheVSDigRaceMode();
		GameManager sourceMgr = new GameManager(new EventReceiver());
		source.modeInit(sourceMgr);
		GameEngine sourceEngine = freshEngine(source, sourceMgr, 0);

		getIntArray(source, "handicapRows")[0] = 8;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		AvalancheVSDigRaceMode dest = new AvalancheVSDigRaceMode();
		GameManager destMgr = new GameManager(new EventReceiver());
		dest.modeInit(destMgr);
		GameEngine destEngine = freshEngine(dest, destMgr, 0);

		invokeLoadOther(dest, destEngine, prop);

		assertEquals(8, getIntArray(dest, "handicapRows")[0],
				"DigRace-specific handicapRows round-trips through legacy key");
	}

	private static GameEngine freshEngine(AvalancheVSDigRaceMode mode,
			GameManager manager, int playerID) {
		manager.mode = mode;
		manager.init();
		manager.engine[playerID].init();
		return manager.engine[playerID];
	}

	private static void invokeLoadOther(AvalancheVSDigRaceMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSDigRaceMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(AvalancheVSDigRaceMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSDigRaceMode.class.getDeclaredMethod(
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
