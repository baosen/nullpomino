package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link TimeAttackMode}'s private {@code setSpeed} branching by
 * {@code goaltype}. The 11 game types fall into three classes:
 * <ul>
 *   <li>fixed-speed (NORMAL/HISPEED1/HISPEED2 share 25/25/41/30/15;
 *       ANOTHER2 uses 6/6/4/13/7; HELL/HELL-X share 2/2/3/11/7),
 *   <li>table-driven (ANOTHER, ANOTHER200, NORMAL200, BASIC, VOID
 *       each look up ARE/areLine/lineDelay/lockDelay/DAS by level),
 *   <li>side-effects (HELL turns on outline-only and gates bone on at
 *       level >= 15; HELL-X always turns on bone + reads a fade table;
 *       VOID always turns on bone).
 * </ul>
 * Each branch also sets gravity from {@code tableGravity[goaltype]}
 * with level clamped into [0, length-1] and denominator from a flat
 * 11-entry table.
 */
class TimeAttackModeSetSpeedTest {

	@Test
	void normalGoaltypeUsesFixedNormalDelayValues() throws Exception {
		// goaltype 0 (NORMAL): ARE=25, lineDelay=41, lockDelay=30, DAS=15.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(25, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
		assertEquals(41, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(15, engine.speed.das);
		// Gravity level 0 is the first NORMAL entry.
		assertEquals(4, engine.speed.gravity,
				"NORMAL level 0 -> tableGravity[0][0]=4");
		assertEquals(256, engine.speed.denominator,
				"NORMAL denominator is 256");
	}

	@Test
	void normalGoaltypeWalksGravityTableByLevel() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0);
		engine.statistics.level = 5;

		invokeSetSpeed(mode, engine);

		assertEquals(128, engine.speed.gravity,
				"NORMAL level 5 -> tableGravity[0][5]=128");
	}

	@Test
	void normalGoaltypeClampsAtMaxIndex() throws Exception {
		// Level >= length-1 saturates at the -1 sentinel (instant fall).
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0);
		engine.statistics.level = 50;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"NORMAL high level clamps to last entry -> -1 sentinel");
	}

	@Test
	void another2GoaltypeUsesAggressiveFixedValues() throws Exception {
		// ANOTHER 2 (goaltype 4) uses very tight fixed delays:
		// ARE=6, lineDelay=4, lockDelay=13, DAS=7.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 4);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(6, engine.speed.are);
		assertEquals(6, engine.speed.areLine);
		assertEquals(4, engine.speed.lineDelay);
		assertEquals(13, engine.speed.lockDelay);
		assertEquals(7, engine.speed.das);
	}

	@Test
	void anotherGoaltypeWalksTableAnotherByLevel() throws Exception {
		// ANOTHER (goaltype 3): ARE/areLine = tableAnother[0][lv].
		// At level 5: ARE=12, lineDelay=5, lockDelay=14, DAS=8.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 3);
		engine.statistics.level = 5;

		invokeSetSpeed(mode, engine);

		assertEquals(12, engine.speed.are,
				"tableAnother[0][5]=12");
		assertEquals(5, engine.speed.lineDelay,
				"tableAnother[1][5]=5");
		assertEquals(14, engine.speed.lockDelay,
				"tableAnother[2][5]=14");
		assertEquals(8, engine.speed.das,
				"tableAnother[3][5]=8");
	}

	@Test
	void anotherGoaltypeClampsSpeedTableAtLength() throws Exception {
		// tableAnother is 10 long; level 99 clamps to index 9.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 3);
		engine.statistics.level = 99;

		invokeSetSpeed(mode, engine);

		assertEquals(6, engine.speed.are,
				"tableAnother[0][9]=6 (last entry)");
		assertEquals(7, engine.speed.das,
				"tableAnother[3][9]=7 (last entry)");
	}

	@Test
	void hellGoaltypeUsesAggressiveFixedDelaysAndShowsOutlineOnly() throws Exception {
		// HELL (goaltype 8): 2/2/3/11/7 and outline-only on.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 8);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.are);
		assertEquals(2, engine.speed.areLine);
		assertEquals(3, engine.speed.lineDelay);
		assertEquals(11, engine.speed.lockDelay);
		assertEquals(7, engine.speed.das);
		assertTrue(engine.blockShowOutlineOnly,
				"HELL: blocks rendered as outlines only");
	}

	@Test
	void hellEnablesBoneAtLevelFifteenOrAbove() throws Exception {
		// HELL: bone OFF below level 15.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 8);
		engine.statistics.level = 14;

		invokeSetSpeed(mode, engine);

		assertFalse(engine.bone,
				"HELL level 14 -> bone still off");

		engine.statistics.level = 15;
		invokeSetSpeed(mode, engine);

		assertTrue(engine.bone,
				"HELL level 15 -> bone turns on");
	}

	@Test
	void hellXGoaltypeAlwaysEnablesBoneAndFadeTable() throws Exception {
		// HELL-X (goaltype 9): bone always on, blockHidden = tableHellXFade[lv].
		// tableHellXFade[0..4] = -1, [5..14] = 150, [15..18] = 120, [19] = 60.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 9);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertTrue(engine.bone, "HELL-X always uses bone blocks");
		assertEquals(-1, engine.blockHidden,
				"HELL-X level 0 -> tableHellXFade[0] = -1 (no fade)");

		engine.statistics.level = 5;
		invokeSetSpeed(mode, engine);
		assertEquals(150, engine.blockHidden,
				"HELL-X level 5 -> 150-frame fade");

		engine.statistics.level = 19;
		invokeSetSpeed(mode, engine);
		assertEquals(60, engine.blockHidden,
				"HELL-X level 19 -> 60-frame fade (fastest)");
	}

	@Test
	void voidGoaltypeAlwaysEnablesBone() throws Exception {
		// VOID (goaltype 10): always bone on.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 10);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertTrue(engine.bone, "VOID always uses bone blocks");
		// VOID gravity table is single-entry [-1] -> always instant fall.
		assertEquals(-1, engine.speed.gravity);
	}

	@Test
	void normal200GoaltypeUsesNormal200Table() throws Exception {
		// NORMAL200 (goaltype 5): tableNormal200[0][0]=25, [1][0]=25, [2][0]=30, [3][0]=15.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 5);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(25, engine.speed.are, "tableNormal200[0][0]=25");
		assertEquals(25, engine.speed.areLine, "areLine mirrors are for NORMAL200");
		assertEquals(25, engine.speed.lineDelay, "tableNormal200[1][0]=25");
		assertEquals(30, engine.speed.lockDelay, "tableNormal200[2][0]=30");
		assertEquals(15, engine.speed.das, "tableNormal200[3][0]=15");
	}

	@Test
	void basicGoaltypeUsesSixtyDenominatorAndBasicTable() throws Exception {
		// BASIC (goaltype 7): denominator = 60 (not 256).
		// At level 5: tableBasic[0][5]=26, [1][5]=25, [2][5]=26, [3][5]=15.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 7);
		engine.statistics.level = 5;

		invokeSetSpeed(mode, engine);

		assertEquals(60, engine.speed.denominator,
				"BASIC denominator is 60");
		assertEquals(26, engine.speed.are,
				"tableBasic[0][5]=26");
		assertEquals(25, engine.speed.lineDelay,
				"tableBasic[1][5]=25");
		assertEquals(26, engine.speed.lockDelay,
				"tableBasic[2][5]=26");
		assertEquals(15, engine.speed.das,
				"tableBasic[3][5]=15");
	}

	@Test
	void levelTimerSetFromTableLevelTimer() throws Exception {
		// NORMAL: tableLevelTimer[0] = {7200, 7200, 5400} -> level 2 = 5400.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0);
		engine.statistics.level = 2;

		invokeSetSpeed(mode, engine);

		assertEquals(5400, getInt(mode, "levelTimer"),
				"NORMAL level 2 -> levelTimer = 5400 frames");
		assertEquals(5400, getInt(mode, "levelTimerMax"),
				"levelTimerMax mirrors levelTimer");
	}

	@Test
	void levelTimerClampsAtTableLength() throws Exception {
		// VOID timer is 20 entries; level 50 clamps to index 19 = 300.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 10);
		engine.statistics.level = 50;

		invokeSetSpeed(mode, engine);

		assertEquals(300, getInt(mode, "levelTimer"),
				"VOID level 50 clamps to last entry = 300");
	}

	private static GameEngine freshEngine(TimeAttackMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(TimeAttackMode mode, GameEngine engine)
			throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void setInt(Object instance, String name, int value)
			throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static int getInt(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
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
