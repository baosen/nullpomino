package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers private helpers in {@link GradeMania2Mode}: setAverageSectionTime,
 * mrollCheck (all section-time and 4-line branches), stMedalCheck (all tiers
 * and no-downgrade), roMedalCheck, and getMedalFontColor.
 */
class GradeMania2ModeHelpersTest {

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverage() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		startlevelValue(mode, 0);
		setInt(mode, "sectionscomp", 3);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 100;
		st[1] = 200;
		st[2] = 300;

		invokeSetAverageSectionTime(mode);

		assertEquals(200, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenSectionsCompIsZero() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 7777);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenStartlevelIsTenOrMore() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		startlevelValue(mode, 10);
		setInt(mode, "sectionscomp", 2);
		setInt(mode, "sectionavgtime", 4444);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// mrollCheck
	// -----------------------------------------------------------------------

	@Test
	void mrollCheckSetsTimeFalseWhenLevelBelow500AndTimeExceedsThreshold() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "mrollSectiontime", true);
		setInt(mode, "sectionlasttime", 4000);  // > 3900

		invokeMrollCheck(mode, 400);  // levelb < 500

		assertFalse(readBoolean(mode, "mrollSectiontime"));
	}

	@Test
	void mrollCheckKeepsTimeTrueWhenLevelBelow500AndTimeWithinThreshold() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "mrollSectiontime", true);
		setInt(mode, "sectionlasttime", 3800);  // <= 3900

		invokeMrollCheck(mode, 400);

		assertTrue(readBoolean(mode, "mrollSectiontime"));
	}

	@Test
	void mrollCheckEvaluatesAverageOfFirstFiveSectionsForLevel500To600() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// sectiontime[0..4] = 1000 each -> avg = 1000, threshold = 1000+120=1120
		int[] st = (int[]) readField(mode, "sectiontime");
		for(int i = 0; i < 5; i++) st[i] = 1000;
		setBoolean(mode, "mrollSectiontime", true);
		setInt(mode, "sectionlasttime", 1200);  // > 1000+120 = 1120

		invokeMrollCheck(mode, 550);  // 500 <= levelb < 600

		assertFalse(readBoolean(mode, "mrollSectiontime"));
	}

	@Test
	void mrollCheckEvaluatesPreviousSectionTimeForLevel600AndAbove() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// levelb=700 -> section 6; uses sectiontime[6] = prev section = sectiontime[(700/100)-1]=sectiontime[6]
		int[] st = (int[]) readField(mode, "sectiontime");
		st[6] = 2000;
		setBoolean(mode, "mrollSectiontime", true);
		setInt(mode, "sectionlasttime", 2200);  // > 2000+120 = 2120

		invokeMrollCheck(mode, 700);

		assertFalse(readBoolean(mode, "mrollSectiontime"));
	}

	@Test
	void mrollCheckSets4LineFalseWhenSectionFourlineCountBelowRequired() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// levelb < 500 -> required4line = 2; section 0 -> sectionfourline[0] = 1 < 2
		int[] sfl = (int[]) readField(mode, "sectionfourline");
		sfl[0] = 1;
		setBoolean(mode, "mrollFourline", true);
		setInt(mode, "sectionlasttime", 3000);  // ok time

		invokeMrollCheck(mode, 400);

		assertFalse(readBoolean(mode, "mrollFourline"));
	}

	@Test
	void mrollCheckRequiresOnlyOne4LineForLevel500To900() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// levelb = 700 -> required4line = 1; sectionfourline[7] = 1 is enough
		int[] sfl = (int[]) readField(mode, "sectionfourline");
		sfl[7] = 1;
		setBoolean(mode, "mrollFourline", true);
		setInt(mode, "sectionlasttime", 1000);  // fast enough

		invokeMrollCheck(mode, 700);

		assertTrue(readBoolean(mode, "mrollFourline"), "1 >= required 1 -> mrollFourline stays true");
	}

	@Test
	void mrollCheckRequiresZero4LinesForLevel900AndAbove() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// levelb = 900 -> required4line = 0; sectionfourline[9] = 0 is fine
		int[] sfl = (int[]) readField(mode, "sectionfourline");
		sfl[9] = 0;
		setBoolean(mode, "mrollFourline", true);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[8] = 1000;
		setInt(mode, "sectionlasttime", 1000);  // not > 1000+120

		invokeMrollCheck(mode, 900);

		assertTrue(readBoolean(mode, "mrollFourline"), "required4line=0 -> never cleared by 4-line check");
	}

	// -----------------------------------------------------------------------
	// stMedalCheck
	// -----------------------------------------------------------------------

	@Test
	void stMedalCheckAwardsBronze() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 3500);
		setInt(mode, "medalST", 0);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(1, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckAwardsSilver() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 3200);
		setInt(mode, "medalST", 1);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(2, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckAwardsGold() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 2999);
		setInt(mode, "medalST", 0);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckDoesNotDowngradeFromGold() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 3500);
		setInt(mode, "medalST", 3);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"));
	}

	// -----------------------------------------------------------------------
	// roMedalCheck
	// -----------------------------------------------------------------------

	@Test
	void roMedalCheckIncrementsMedalAtThreshold() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "rotateCount", 12);
		engine.statistics.totalPieceLocked = 10;
		setInt(mode, "medalRO", 0);

		invokeRoMedalCheck(mode, engine);

		assertEquals(1, readInt(mode, "medalRO"));
	}

	@Test
	void roMedalCheckCapsAtThree() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "rotateCount", 12);
		engine.statistics.totalPieceLocked = 10;
		setInt(mode, "medalRO", 3);

		invokeRoMedalCheck(mode, engine);

		assertEquals(3, readInt(mode, "medalRO"));
	}

	// -----------------------------------------------------------------------
	// getMedalFontColor
	// -----------------------------------------------------------------------

	@Test
	void getMedalFontColorReturnsCorrectColors() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		assertEquals(EventReceiver.COLOR_RED, invokeGetMedalFontColor(mode, 1));
		assertEquals(EventReceiver.COLOR_WHITE, invokeGetMedalFontColor(mode, 2));
		assertEquals(EventReceiver.COLOR_YELLOW, invokeGetMedalFontColor(mode, 3));
		assertEquals(-1, invokeGetMedalFontColor(mode, 0));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GradeMania2Mode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void startlevelValue(GradeMania2Mode mode, int value) throws Exception {
		Field f = findField(mode.getClass(), "startlevel");
		f.setAccessible(true);
		IntegerMenuItem item = (IntegerMenuItem) f.get(mode);
		Field vf = findField(item.getClass(), "value");
		vf.setAccessible(true);
		vf.set(item, value);
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

	private static Object readField(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
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

	private static void invokeSetAverageSectionTime(GradeMania2Mode mode) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeMrollCheck(GradeMania2Mode mode, int levelb) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod("mrollCheck", int.class);
		m.setAccessible(true);
		m.invoke(mode, levelb);
	}

	private static void invokeStMedalCheck(GradeMania2Mode mode, GameEngine engine, int sectionNumber) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod("stMedalCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, sectionNumber);
	}

	private static void invokeRoMedalCheck(GradeMania2Mode mode, GameEngine engine) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod("roMedalCheck", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static int invokeGetMedalFontColor(GradeMania2Mode mode, int medalColor) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod("getMedalFontColor", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, medalColor);
	}
}
