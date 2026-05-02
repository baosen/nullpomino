package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.menu.IntegerMenuItem;

import org.junit.jupiter.api.Test;

/**
 * Covers private helpers in {@link PhantomManiaMode}: setAverageSectionTime,
 * stMedalCheck (all three award tiers and no-downgrade), roMedalCheck, and
 * getMedalFontColor.
 */
class PhantomManiaModeHelpersTest {

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverageWhenSectionsCompIsPositive() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel.value = 0, sectionscomp = 3, sectiontime[0..2] = 100, 200, 300
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
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 8888);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// stMedalCheck
	// -----------------------------------------------------------------------

	@Test
	void stMedalCheckAwardsBronzeWhenLastTimeIsWithinSixHundredOfBest() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 3500);  // < 3000+600
		setInt(mode, "medalST", 0);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(1, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckAwardsSilverWhenLastTimeIsWithinThreeHundredOfBest() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 3200);  // < 3000+300
		setInt(mode, "medalST", 1);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(2, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckAwardsGoldWhenLastTimeBeatsBest() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
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
	void stMedalCheckDoesNotDowngradeMedalAlreadyGold() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 3500);  // only qualifies for bronze
		setInt(mode, "medalST", 3);             // already gold

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"), "gold must not be downgraded");
	}

	// -----------------------------------------------------------------------
	// roMedalCheck
	// -----------------------------------------------------------------------

	@Test
	void roMedalCheckIncrementsMedalWhenAverageRotationsPerPieceIsHighEnough() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// rotateAverage = 12 / 10 = 1.2f, threshold >= 1.2
		setInt(mode, "rotateCount", 12);
		engine.statistics.totalPieceLocked = 10;
		setInt(mode, "medalRO", 0);

		invokeRoMedalCheck(mode, engine);

		assertEquals(1, readInt(mode, "medalRO"));
	}

	@Test
	void roMedalCheckDoesNotIncrementWhenAverageIsBelowThreshold() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "rotateCount", 10);
		engine.statistics.totalPieceLocked = 10;  // average = 1.0 < 1.2
		setInt(mode, "medalRO", 0);

		invokeRoMedalCheck(mode, engine);

		assertEquals(0, readInt(mode, "medalRO"), "1.0 < 1.2 -> no medal");
	}

	@Test
	void roMedalCheckCapsAtThreeIncrements() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "rotateCount", 12);
		engine.statistics.totalPieceLocked = 10;  // average = 1.2
		setInt(mode, "medalRO", 3);               // already at cap

		invokeRoMedalCheck(mode, engine);

		assertEquals(3, readInt(mode, "medalRO"), "medalRO must not exceed 3");
	}

	// -----------------------------------------------------------------------
	// getMedalFontColor
	// -----------------------------------------------------------------------

	@Test
	void getMedalFontColorReturnsCorrectColorsForAllLevels() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();

		assertEquals(EventReceiver.COLOR_RED, invokeGetMedalFontColor(mode, 1));
		assertEquals(EventReceiver.COLOR_WHITE, invokeGetMedalFontColor(mode, 2));
		assertEquals(EventReceiver.COLOR_YELLOW, invokeGetMedalFontColor(mode, 3));
		assertEquals(-1, invokeGetMedalFontColor(mode, 0));
		assertEquals(-1, invokeGetMedalFontColor(mode, 4));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(PhantomManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void startlevelValue(PhantomManiaMode mode, int value) throws Exception {
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

	private static void invokeSetAverageSectionTime(PhantomManiaMode mode) throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStMedalCheck(PhantomManiaMode mode, GameEngine engine, int sectionNumber) throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod("stMedalCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, sectionNumber);
	}

	private static void invokeRoMedalCheck(PhantomManiaMode mode, GameEngine engine) throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod("roMedalCheck", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static int invokeGetMedalFontColor(PhantomManiaMode mode, int medalColor) throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod("getMedalFontColor", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, medalColor);
	}
}
