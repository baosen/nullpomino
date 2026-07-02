package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers private helpers in {@link FinalMode}: setAverageSectionTime (positive
 * and zero-sectionscomp branches), stMedalCheck (bronze/silver/gold and
 * no-downgrade), and getMedalFontColor.
 */
class FinalModeHelpersTest {

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverage() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 0);
		setInt(mode, "sectionscomp", 2);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 1000;
		st[1] = 3000;

		invokeSetAverageSectionTime(mode);

		assertEquals(2000, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenNoSectionsCompleted() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 9999);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// stMedalCheck
	// -----------------------------------------------------------------------

	@Test
	void stMedalCheckAwardsBronzeForTimeWithinSixHundred() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 1800;
		setInt(mode, "sectionlasttime", 2300);  // < 1800+600
		setInt(mode, "medalST", 0);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(1, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckAwardsSilverForTimeWithinThreeHundred() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 1800;
		setInt(mode, "sectionlasttime", 2000);  // < 1800+300
		setInt(mode, "medalST", 1);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(2, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckAwardsGoldWhenTimeBeatsBest() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 1800;
		setInt(mode, "sectionlasttime", 1799);
		setInt(mode, "medalST", 0);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckDoesNotDowngradeFromGold() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 1800;
		setInt(mode, "sectionlasttime", 2300);
		setInt(mode, "medalST", 3);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"), "gold must not be downgraded");
	}

	// -----------------------------------------------------------------------
	// getMedalFontColor
	// -----------------------------------------------------------------------

	@Test
	void getMedalFontColorReturnsCorrectColors() throws Exception {
		FinalMode mode = new FinalMode();
		assertEquals(EventReceiver.COLOR_RED, invokeGetMedalFontColor(mode, 1));
		assertEquals(EventReceiver.COLOR_WHITE, invokeGetMedalFontColor(mode, 2));
		assertEquals(EventReceiver.COLOR_YELLOW, invokeGetMedalFontColor(mode, 3));
		assertEquals(-1, invokeGetMedalFontColor(mode, 0));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(FinalMode mode) {
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

	private static void invokeSetAverageSectionTime(FinalMode mode) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStMedalCheck(FinalMode mode, GameEngine engine, int sectionNumber) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("stMedalCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, sectionNumber);
	}

	private static int invokeGetMedalFontColor(FinalMode mode, int medalColor) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("getMedalFontColor", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, medalColor);
	}
}
