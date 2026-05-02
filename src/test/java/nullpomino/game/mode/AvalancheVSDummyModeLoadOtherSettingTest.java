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
 * Pins {@link AvalancheVSDummyMode#loadOtherSetting} (the load
 * counterpart to the save tests in {@link
 * AvalancheVSDummyModeOtherSettingTest}). Reads the family-wide
 * settings under {@code avalanchevs<name>.*}: bgmno=0,
 * ojamaCounterMode=OJAMA_COUNTER_ON (1), big=false, enableSE=true,
 * hurryupSeconds=192 (pre-hurry-up grace), useMap=false, mapSet=0,
 * mapNumber=-1, feverMapSet=0, presetNumber=0, maxAttack=30,
 * numColors=5, rensaShibari=1, zenKeshiType=1, outlineType=1,
 * dangerColumnDouble=false, dangerColumnShowX=false, chainDisplayType=1,
 * newChainPower=false, cascadeSlow=false, bigDisplay=false,
 * engine.colorClearSize=4. Engine state for clearSize is materialised
 * onto {@code engine.colorClearSize}, not a mode field.
 */
class AvalancheVSDummyModeLoadOtherSettingTest {

	@Test
	void loadOtherSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadOther(mode, engine, new CustomProperties(), "");

		assertEquals(0, getInt(mode, "bgmno"),
				"bgmno default 0");
		assertEquals(AvalancheVSDummyMode.OJAMA_COUNTER_ON,
				getIntArray(mode, "ojamaCounterMode")[0],
				"ojamaCounterMode default ON");
		assertFalse(getBooleanArray(mode, "big")[0],
				"big default false");
		assertTrue(getBooleanArray(mode, "enableSE")[0],
				"enableSE default true");
		assertEquals(192, getIntArray(mode, "hurryupSeconds")[0],
				"hurryupSeconds default 192 (pre-hurry-up grace)");
		assertFalse(getBooleanArray(mode, "useMap")[0]);
		assertEquals(0, getIntArray(mode, "mapSet")[0]);
		assertEquals(-1, getIntArray(mode, "mapNumber")[0],
				"mapNumber default -1 (random/none)");
		assertEquals(0, getIntArray(mode, "feverMapSet")[0],
				"feverMapSet default 0 (first fever-map set)");
		assertEquals(0, getIntArray(mode, "presetNumber")[0]);
		assertEquals(30, getIntArray(mode, "maxAttack")[0],
				"maxAttack default 30");
		assertEquals(5, getIntArray(mode, "numColors")[0],
				"numColors default 5 (full puyo color set)");
		assertEquals(1, getIntArray(mode, "rensaShibari")[0],
				"rensaShibari default 1 (1-chain attack threshold)");
		assertEquals(1, getIntArray(mode, "zenKeshiType")[0]);
		assertEquals(1, getIntArray(mode, "outlineType")[0]);
		assertFalse(getBooleanArray(mode, "dangerColumnDouble")[0]);
		assertFalse(getBooleanArray(mode, "dangerColumnShowX")[0]);
		assertEquals(1, getIntArray(mode, "chainDisplayType")[0]);
		assertFalse(getBooleanArray(mode, "newChainPower")[0]);
		assertFalse(getBooleanArray(mode, "cascadeSlow")[0]);
		assertFalse(getBoolean(mode, "bigDisplay"));
		assertEquals(4, engine.colorClearSize,
				"engine.colorClearSize default 4 (puyo standard)");
	}

	@Test
	void loadOtherSettingReadsAvalanchevsKeysFromProperties() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.bgmno", 5);
		prop.setProperty("avalanchevs.ojamaCounterMode", 2);
		prop.setProperty("avalanchevs.big.p0", true);
		prop.setProperty("avalanchevs.enableSE.p0", false);
		prop.setProperty("avalanchevs.hurryupSeconds.p0", 60);
		prop.setProperty("avalanchevs.useMap.p0", true);
		prop.setProperty("avalanchevs.mapSet.p0", 7);
		prop.setProperty("avalanchevs.mapNumber.p0", 11);
		prop.setProperty("avalanchevs.presetNumber.p0", 4);
		prop.setProperty("avalanchevs.maxAttack.p0", 99);
		prop.setProperty("avalanchevs.numColors.p0", 4);
		prop.setProperty("avalanchevs.rensaShibari.p0", 3);
		prop.setProperty("avalanchevs.zenKeshiType.p0", 2);
		prop.setProperty("avalanchevs.outlineType.p0", 0);
		prop.setProperty("avalanchevs.dangerColumnDouble.p0", true);
		prop.setProperty("avalanchevs.dangerColumnShowX.p0", true);
		prop.setProperty("avalanchevs.chainDisplayType.p0", 3);
		prop.setProperty("avalanchevs.newChainPower.p0", true);
		prop.setProperty("avalanchevs.cascadeSlow.p0", true);
		prop.setProperty("avalanchevs.bigDisplay", true);
		prop.setProperty("avalanchevs.clearSize.p0", 7);

		invokeLoadOther(mode, engine, prop, "");

		assertEquals(5, getInt(mode, "bgmno"));
		assertEquals(2, getIntArray(mode, "ojamaCounterMode")[0]);
		assertTrue(getBooleanArray(mode, "big")[0]);
		assertFalse(getBooleanArray(mode, "enableSE")[0]);
		assertEquals(60, getIntArray(mode, "hurryupSeconds")[0]);
		assertTrue(getBooleanArray(mode, "useMap")[0]);
		assertEquals(7, getIntArray(mode, "mapSet")[0]);
		assertEquals(11, getIntArray(mode, "mapNumber")[0]);
		assertEquals(4, getIntArray(mode, "presetNumber")[0]);
		assertEquals(99, getIntArray(mode, "maxAttack")[0]);
		assertEquals(4, getIntArray(mode, "numColors")[0]);
		assertEquals(3, getIntArray(mode, "rensaShibari")[0]);
		assertEquals(2, getIntArray(mode, "zenKeshiType")[0]);
		assertEquals(0, getIntArray(mode, "outlineType")[0]);
		assertTrue(getBooleanArray(mode, "dangerColumnDouble")[0]);
		assertTrue(getBooleanArray(mode, "dangerColumnShowX")[0]);
		assertEquals(3, getIntArray(mode, "chainDisplayType")[0]);
		assertTrue(getBooleanArray(mode, "newChainPower")[0]);
		assertTrue(getBooleanArray(mode, "cascadeSlow")[0]);
		assertTrue(getBoolean(mode, "bigDisplay"));
		assertEquals(7, engine.colorClearSize,
				"clearSize.pN materialises onto engine.colorClearSize");
	}

	@Test
	void loadOtherSettingNonEmptySuffixScopesKeys() throws Exception {
		// 'fever' suffix -> avalanchevsfever.* keys; the empty-suffix
		// keyspace doesn't leak through.
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsfever.bgmno", 8);
		prop.setProperty("avalanchevs.bgmno", 99);

		invokeLoadOther(mode, engine, prop, "fever");

		assertEquals(8, getInt(mode, "bgmno"),
				"'fever' suffix scopes the lookup; the unscoped "
						+ "'avalanchevs.bgmno' is ignored");
	}

	private static GameEngine freshEngine(AvalancheVSDummyMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeLoadOther(AvalancheVSDummyMode mode, GameEngine engine,
			CustomProperties prop, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, name);
	}

	private static int getInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean getBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
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
