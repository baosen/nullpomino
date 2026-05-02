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
 * Pins {@link AvalancheVSBombBattleMode}'s private
 * {@code loadOtherSetting} / {@code saveOtherSetting} per-player
 * persistence under the {@code avalanchevsbombbattle.*} prefix. The
 * load/save surface is asymmetric: loadOtherSetting reads ojamaRate
 * (default 60) and ojamaHard (default 1) — note BombBattle's defaults
 * differ from the parent AvalancheVS's 120 and 0. The subclass-only
 * {@code newChainPower} (boolean, default false) and
 * {@code ojamaCountdown} (int, default 5) are read AND written.
 * ojamaRate/ojamaHard are NOT written by saveOtherSetting — those go
 * through {@code super.saveOtherSetting} with the "bombbattle" suffix.
 */
class AvalancheVSBombBattleModeOtherSettingTest {

	@Test
	void loadOtherSettingReadsBombBattleDefaults() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		invokeLoadOther(mode, engine, new CustomProperties());

		assertEquals(60, getIntArray(mode, "ojamaRate")[0],
				"BombBattle ojamaRate default 60 (not 120 like the base AvalancheVS)");
		assertEquals(1, getIntArray(mode, "ojamaHard")[0],
				"BombBattle ojamaHard default 1 (not 0 like the base AvalancheVS)");
		assertFalse(getBooleanArray(mode, "newChainPower")[0],
				"newChainPower default false");
		assertEquals(5, getIntArray(mode, "ojamaCountdown")[0],
				"ojamaCountdown default 5");
	}

	@Test
	void loadOtherSettingReadsBombBattleKeysFromProperties() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsbombbattle.ojamaRate.p0", 200);
		prop.setProperty("avalanchevsbombbattle.ojamaHard.p0", 4);
		prop.setProperty("avalanchevsbombbattle.newChainPower.p0", true);
		prop.setProperty("avalanchevsbombbattle.ojamaCountdown.p0", 10);

		invokeLoadOther(mode, engine, prop);

		assertEquals(200, getIntArray(mode, "ojamaRate")[0]);
		assertEquals(4, getIntArray(mode, "ojamaHard")[0]);
		assertTrue(getBooleanArray(mode, "newChainPower")[0]);
		assertEquals(10, getIntArray(mode, "ojamaCountdown")[0]);
	}

	@Test
	void saveOtherSettingWritesNewChainPowerAndOjamaCountdownButNotRateOrHard() throws Exception {
		// Subclass-only saveOtherSetting writes newChainPower and
		// ojamaCountdown directly. ojamaRate / ojamaHard pass through
		// super.saveOtherSetting with the "bombbattle" suffix.
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 1);

		getBooleanArray(mode, "newChainPower")[1] = true;
		getIntArray(mode, "ojamaCountdown")[1] = 12;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(mode, engine, prop);

		assertEquals(true, prop.getProperty(
				"avalanchevsbombbattle.newChainPower.p1", false));
		assertEquals(12, prop.getProperty(
				"avalanchevsbombbattle.ojamaCountdown.p1", -1));
	}

	@Test
	void saveAndLoadOtherSettingRoundTripBombBattleSpecificFields() throws Exception {
		AvalancheVSBombBattleMode source = new AvalancheVSBombBattleMode();
		GameManager sourceMgr = new GameManager(new EventReceiver());
		source.modeInit(sourceMgr);
		GameEngine sourceEngine = freshEngine(source, sourceMgr, 0);

		getBooleanArray(source, "newChainPower")[0] = true;
		getIntArray(source, "ojamaCountdown")[0] = 8;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		AvalancheVSBombBattleMode dest = new AvalancheVSBombBattleMode();
		GameManager destMgr = new GameManager(new EventReceiver());
		dest.modeInit(destMgr);
		GameEngine destEngine = freshEngine(dest, destMgr, 0);

		invokeLoadOther(dest, destEngine, prop);

		assertTrue(getBooleanArray(dest, "newChainPower")[0],
				"BombBattle-specific newChainPower round-trips");
		assertEquals(8, getIntArray(dest, "ojamaCountdown")[0],
				"BombBattle-specific ojamaCountdown round-trips");
	}

	private static GameEngine freshEngine(AvalancheVSBombBattleMode mode,
			GameManager manager, int playerID) {
		manager.mode = mode;
		manager.init();
		manager.engine[playerID].init();
		return manager.engine[playerID];
	}

	private static void invokeLoadOther(AvalancheVSBombBattleMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(AvalancheVSBombBattleMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (int[]) f.get(obj);
	}

	private static boolean[] getBooleanArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (boolean[]) f.get(obj);
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
