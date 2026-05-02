package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers private helpers in {@link SpeedManiaMode}: setAverageSectionTime,
 * stMedalCheck (all tiers and no-downgrade), roMedalCheck, getMedalFontColor.
 */
class SpeedManiaModeHelpersTest {

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverage() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		startlevelValue(mode, 0);
		setInt(mode, "sectionscomp", 3);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 300;
		st[1] = 600;
		st[2] = 900;

		invokeSetAverageSectionTime(mode);

		assertEquals(600, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenNoSectionsCompleted() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 1234);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// stMedalCheck
	// -----------------------------------------------------------------------

	@Test
	void stMedalCheckAwardsBronzeForTimeWithinSixHundred() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
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
	void stMedalCheckAwardsSilverForTimeWithinThreeHundred() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
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
	void stMedalCheckAwardsGoldWhenTimeBeatsBest() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
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
	void stMedalCheckDoesNotDowngradeMedalAlreadyAtGold() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 3500);
		setInt(mode, "medalST", 3);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"), "gold must not be downgraded");
	}

	// -----------------------------------------------------------------------
	// roMedalCheck
	// -----------------------------------------------------------------------

	@Test
	void roMedalCheckIncrementsMedalAtThreshold() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "rotateCount", 12);
		engine.statistics.totalPieceLocked = 10;  // 1.2 >= 1.2
		setInt(mode, "medalRO", 0);

		invokeRoMedalCheck(mode, engine);

		assertEquals(1, readInt(mode, "medalRO"));
	}

	@Test
	void roMedalCheckDoesNotIncrementBelowThreshold() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "rotateCount", 10);
		engine.statistics.totalPieceLocked = 10;  // 1.0 < 1.2
		setInt(mode, "medalRO", 0);

		invokeRoMedalCheck(mode, engine);

		assertEquals(0, readInt(mode, "medalRO"));
	}

	@Test
	void roMedalCheckCapsAtThree() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
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
		SpeedManiaMode mode = new SpeedManiaMode();
		assertEquals(EventReceiver.COLOR_RED, invokeGetMedalFontColor(mode, 1));
		assertEquals(EventReceiver.COLOR_WHITE, invokeGetMedalFontColor(mode, 2));
		assertEquals(EventReceiver.COLOR_YELLOW, invokeGetMedalFontColor(mode, 3));
		assertEquals(-1, invokeGetMedalFontColor(mode, 0));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SpeedManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void startlevelValue(SpeedManiaMode mode, int value) throws Exception {
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

	private static void invokeSetAverageSectionTime(SpeedManiaMode mode) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStMedalCheck(SpeedManiaMode mode, GameEngine engine, int sectionNumber) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod("stMedalCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, sectionNumber);
	}

	private static void invokeRoMedalCheck(SpeedManiaMode mode, GameEngine engine) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod("roMedalCheck", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static int invokeGetMedalFontColor(SpeedManiaMode mode, int medalColor) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod("getMedalFontColor", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, medalColor);
	}
}
