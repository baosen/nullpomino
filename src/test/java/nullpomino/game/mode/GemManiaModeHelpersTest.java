package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers private helpers in {@link GemManiaMode}: getStageName, loadMap /
 * saveMap round-trip, and checkStageEnd branching.
 */
class GemManiaModeHelpersTest {

	// -----------------------------------------------------------------------
	// getStageName
	// -----------------------------------------------------------------------

	@Test
	void getStageNameReturnsOneBasedIndexForNormalStages() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		assertEquals("1", invokeGetStageName(mode, 0));
		assertEquals("10", invokeGetStageName(mode, 9));
		assertEquals("20", invokeGetStageName(mode, 19));
	}

	@Test
	void getStageNameReturnsEXPrefixForExtraStages() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		// MAX_STAGE_NORMAL = 20; stageNumber >= 20 -> EX(stageNumber+1-20)
		assertEquals("EX1", invokeGetStageName(mode, 20));
		assertEquals("EX7", invokeGetStageName(mode, 26));
	}

	// -----------------------------------------------------------------------
	// loadMap / saveMap round-trip
	// -----------------------------------------------------------------------

	@Test
	void loadMapAndSaveMapRoundTrip() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Build a property bag with stage id=0 map metadata
		CustomProperties prop = new CustomProperties();
		prop.setProperty("0.gemmania.limittimeStart", 7200);
		prop.setProperty("0.gemmania.stagetimeStart", 1800);
		prop.setProperty("0.gemmania.stagebgm", BGMStatus.BGM_PUZZLE2);
		prop.setProperty("0.gemmania.gimmickMirror", 3);
		prop.setProperty("0.gemmania.gimmickRoll", 5);
		prop.setProperty("0.gemmania.gimmickBig", 2);
		prop.setProperty("0.gemmania.gimmickXRay", 1);
		prop.setProperty("0.gemmania.gimmickColor", 4);

		engine.createFieldIfNeeded();

		invokeLoadMap(mode, engine.field, prop, 0);

		assertEquals(7200, readInt(mode, "limittimeStart"));
		assertEquals(1800, readInt(mode, "stagetimeStart"));
		assertEquals(BGMStatus.BGM_PUZZLE2, readInt(mode, "stagebgm"));
		assertEquals(3, readInt(mode, "gimmickMirror"));
		assertEquals(5, readInt(mode, "gimmickRoll"));
		assertEquals(2, readInt(mode, "gimmickBig"));
		assertEquals(1, readInt(mode, "gimmickXRay"));
		assertEquals(4, readInt(mode, "gimmickColor"));

		// Now saveMap into a fresh prop and check the key round-trips
		CustomProperties out = new CustomProperties();
		invokeSaveMap(mode, engine.field, out, 0);

		assertEquals(7200, out.getProperty("0.gemmania.limittimeStart", -1));
		assertEquals(1800, out.getProperty("0.gemmania.stagetimeStart", -1));
		assertEquals(BGMStatus.BGM_PUZZLE2, out.getProperty("0.gemmania.stagebgm", -1));
		assertEquals(3, out.getProperty("0.gemmania.gimmickMirror", -1));
		assertEquals(5, out.getProperty("0.gemmania.gimmickRoll", -1));
		assertEquals(2, out.getProperty("0.gemmania.gimmickBig", -1));
		assertEquals(1, out.getProperty("0.gemmania.gimmickXRay", -1));
		assertEquals(4, out.getProperty("0.gemmania.gimmickColor", -1));
	}

	@Test
	void loadMapUsesDefaultsWhenPropIsEmpty() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.createFieldIfNeeded();
		invokeLoadMap(mode, engine.field, new CustomProperties(), 0);

		// Documented defaults: limittimeStart=3600*3, stagetimeStart=3600*1
		assertEquals(3600 * 3, readInt(mode, "limittimeStart"));
		assertEquals(3600 * 1, readInt(mode, "stagetimeStart"));
	}

	// -----------------------------------------------------------------------
	// checkStageEnd
	// -----------------------------------------------------------------------

	@Test
	void checkStageEndTransitionsToCustomWhenClearFlagIsTrue() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "clearflag", true);
		engine.timerActive = true;
		engine.stat = GameEngine.Status.MOVE;

		invokeCheckStageEnd(mode, engine);

		assertEquals(GameEngine.Status.CUSTOM, engine.stat);
		assertFalse(engine.timerActive);
		assertFalse(readBoolean(mode, "skipflag"));
	}

	@Test
	void checkStageEndTransitionsToCustomWhenStageTimeExpires() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "clearflag", false);
		setInt(mode, "stagetimeNow", 0);
		setInt(mode, "stagetimeStart", 3600);
		engine.timerActive = true;
		engine.stat = GameEngine.Status.MOVE;

		invokeCheckStageEnd(mode, engine);

		assertEquals(GameEngine.Status.CUSTOM, engine.stat);
	}

	@Test
	void checkStageEndDoesNothingWhenStageTimeIsZeroButStartIsAlsoZero() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "clearflag", false);
		setInt(mode, "stagetimeNow", 0);
		setInt(mode, "stagetimeStart", 0);  // no time limit
		setInt(mode, "limittimeNow", 9999);
		engine.timerActive = true;
		engine.stat = GameEngine.Status.MOVE;

		invokeCheckStageEnd(mode, engine);

		assertEquals(GameEngine.Status.MOVE, engine.stat, "no transition without clear or real time limit");
	}

	@Test
	void checkStageEndTransitionsToGameOverWhenLimitTimeExpires() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "clearflag", false);
		setInt(mode, "stagetimeNow", 3600);
		setInt(mode, "stagetimeStart", 3600);
		setInt(mode, "limittimeNow", 0);
		engine.timerActive = true;
		engine.stat = GameEngine.Status.MOVE;

		invokeCheckStageEnd(mode, engine);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	@Test
	void checkStageEndDoesNothingWhenNoConditionMet() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "clearflag", false);
		setInt(mode, "stagetimeNow", 100);
		setInt(mode, "stagetimeStart", 3600);
		setInt(mode, "limittimeNow", 100);
		engine.timerActive = true;
		engine.stat = GameEngine.Status.MOVE;

		invokeCheckStageEnd(mode, engine);

		assertEquals(GameEngine.Status.MOVE, engine.stat);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GemManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static int readInt(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static boolean readBoolean(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(instance);
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static void setBoolean(Object instance, String name, boolean value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(instance, value);
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

	private static String invokeGetStageName(GemManiaMode mode, int stageNumber) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod("getStageName", int.class);
		m.setAccessible(true);
		return (String) m.invoke(mode, stageNumber);
	}

	private static void invokeLoadMap(GemManiaMode mode, nullpomino.game.component.Field field,
			CustomProperties prop, int id) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod("loadMap",
				nullpomino.game.component.Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}

	private static void invokeSaveMap(GemManiaMode mode, nullpomino.game.component.Field field,
			CustomProperties prop, int id) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod("saveMap",
				nullpomino.game.component.Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}

	private static void invokeCheckStageEnd(GemManiaMode mode, GameEngine engine) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod("checkStageEnd", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
