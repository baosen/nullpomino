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
 * Covers private helpers in {@link GradeMania3Mode}: loadSetting/saveSetting
 * round-trip, setAverageSectionTime, stMedalCheck, getMedalFontColor,
 * checkCool, checkRegret, getGradeName, and isAnyExam.
 */
class GradeMania3ModeHelpersTest {

	// -----------------------------------------------------------------------
	// loadSetting / saveSetting
	// -----------------------------------------------------------------------

	@Test
	void loadSettingRoundTrip() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("grademania3.startlevel", 5);
		prop.setProperty("grademania3.internalLevel", 600);
		prop.setProperty("grademania3.alwaysghost", true);
		prop.setProperty("grademania3.always20g", true);
		prop.setProperty("grademania3.lvstopse", false);
		prop.setProperty("grademania3.showsectiontime", true);
		prop.setProperty("grademania3.big", true);
		prop.setProperty("grademania3.gradedisp", true);
		prop.setProperty("grademania3.lv500torikan", 12345);
		prop.setProperty("grademania3.enableexam", true);
		prop.setProperty("grademania3.stcolor", 3);

		invokeLoadSetting(mode, prop);

		assertEquals(5, readInt(mode, "startlevel"));
		assertEquals(600, readInt(mode, "internalStartLevel"));
		assertTrue(readBoolean(mode, "alwaysghost"));
		assertTrue(readBoolean(mode, "always20g"));
		assertFalse(readBoolean(mode, "lvstopse"));
		assertTrue(readBoolean(mode, "showsectiontime"));
		assertTrue(readBoolean(mode, "big"));
		assertTrue(readBoolean(mode, "gradedisp"));
		assertEquals(12345, readInt(mode, "lv500torikan"));
		assertTrue(readBoolean(mode, "enableexam"));
		assertEquals(3, readInt(mode, "stcolor"));
	}

	@Test
	void saveSettingThenLoadSettingRoundTrips() throws Exception {
		GradeMania3Mode src = new GradeMania3Mode();
		GameEngine engine = freshEngine(src);
		src.playerInit(engine, 0);

		setInt(src, "startlevel", 7);
		setInt(src, "internalStartLevel", 800);
		setBoolean(src, "alwaysghost", true);
		setBoolean(src, "always20g", false);
		setBoolean(src, "lvstopse", true);
		setBoolean(src, "showsectiontime", true);
		setBoolean(src, "big", false);
		setBoolean(src, "gradedisp", true);
		setInt(src, "lv500torikan", 9999);
		setBoolean(src, "enableexam", true);
		setInt(src, "stcolor", 2);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(src, prop);

		GradeMania3Mode dest = new GradeMania3Mode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(7, readInt(dest, "startlevel"));
		assertEquals(800, readInt(dest, "internalStartLevel"));
		assertTrue(readBoolean(dest, "alwaysghost"));
		assertFalse(readBoolean(dest, "always20g"));
		assertTrue(readBoolean(dest, "lvstopse"));
		assertTrue(readBoolean(dest, "showsectiontime"));
		assertFalse(readBoolean(dest, "big"));
		assertTrue(readBoolean(dest, "gradedisp"));
		assertEquals(9999, readInt(dest, "lv500torikan"));
		assertTrue(readBoolean(dest, "enableexam"));
		assertEquals(2, readInt(dest, "stcolor"));
	}

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverageWhenSectionsCompIsPositive() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel=0, sectionscomp=3, sectiontime[0..2] = 100, 200, 300
		setInt(mode, "startlevel", 0);
		setInt(mode, "sectionscomp", 3);
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 100;
		sectiontime[1] = 200;
		sectiontime[2] = 300;

		invokeSetAverageSectionTime(mode);

		assertEquals(200, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenSectionsCompIsZero() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 0);
		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 9999);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenStartlevelIsTen() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel >= 10 skips the sum and sets 0
		setInt(mode, "startlevel", 10);
		setInt(mode, "sectionscomp", 2);
		setInt(mode, "sectionavgtime", 7777);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// stMedalCheck
	// -----------------------------------------------------------------------

	@Test
	void stMedalCheckAwardsBronzeWhenSectionLastTimeIsWithinSixHundred() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// bestSectionTime[0][0] = 3000, sectionlasttime = 3500 (<3000+600)
		int[][] bst = (int[][]) readField(mode, "bestSectionTime");
		bst[0][0] = 3000;
		setInt(mode, "sectionlasttime", 3500);
		setInt(mode, "medalST", 0);
		setBoolean(mode, "enableexam", false);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(1, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckAwardsSilverWhenSectionLastTimeIsWithinThreeHundred() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] bst = (int[][]) readField(mode, "bestSectionTime");
		bst[0][0] = 3000;
		setInt(mode, "sectionlasttime", 3200);  // < 3000+300
		setInt(mode, "medalST", 1);
		setBoolean(mode, "enableexam", false);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(2, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckAwardsGoldWhenSectionLastTimeBeatsRecord() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] bst = (int[][]) readField(mode, "bestSectionTime");
		bst[0][0] = 3000;
		setInt(mode, "sectionlasttime", 2999);  // < best
		setInt(mode, "medalST", 0);
		setBoolean(mode, "enableexam", false);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckDoesNotDowngradeMedalAlreadyGold() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] bst = (int[][]) readField(mode, "bestSectionTime");
		bst[0][0] = 3000;
		setInt(mode, "sectionlasttime", 3500);  // only qualifies for bronze
		setInt(mode, "medalST", 3);             // already gold
		setBoolean(mode, "enableexam", false);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"), "gold is never downgraded");
	}

	@Test
	void stMedalCheckUsesExamTrackWhenEnableExamIsTrue() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] bst = (int[][]) readField(mode, "bestSectionTime");
		// normal track [0][0] is very lenient; exam track [0][1] is strict
		bst[0][0] = 9000;   // normal: sectionlasttime would beat this
		bst[0][1] = 2000;   // exam: best=2000, sectionlasttime=2200 < 2000+300 -> silver
		setInt(mode, "sectionlasttime", 2200);
		setInt(mode, "medalST", 0);
		setBoolean(mode, "enableexam", true);

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(2, readInt(mode, "medalST"), "exam track used when enableexam=true");
	}

	// -----------------------------------------------------------------------
	// getMedalFontColor
	// -----------------------------------------------------------------------

	@Test
	void getMedalFontColorReturnsCorrectColorsForAllLevels() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();

		assertEquals(EventReceiver.COLOR_RED, invokeGetMedalFontColor(mode, 1));
		assertEquals(EventReceiver.COLOR_WHITE, invokeGetMedalFontColor(mode, 2));
		assertEquals(EventReceiver.COLOR_YELLOW, invokeGetMedalFontColor(mode, 3));
		assertEquals(-1, invokeGetMedalFontColor(mode, 0));
		assertEquals(-1, invokeGetMedalFontColor(mode, 4));
	}

	// -----------------------------------------------------------------------
	// checkCool
	// -----------------------------------------------------------------------

	@Test
	void checkCoolSetsCooltTrueWhenTimeMeetsCriteria() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// section 0, tableTimeCool[0] = 3120
		// level 70 (% 100 >= 70), previouscool=false
		engine.statistics.level = 70;
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 3000;  // <= 3120 -> cool
		setBoolean(mode, "coolchecked", false);
		setBoolean(mode, "previouscool", false);

		invokeCheckCool(mode, engine);

		assertTrue(readBoolean(mode, "cool"));
		assertTrue(readBoolean(mode, "coolchecked"));
	}

	@Test
	void checkCoolSetsFalseWhenTimeExceedsCriteria() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statistics.level = 70;
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 4000;  // > 3120 -> not cool
		setBoolean(mode, "coolchecked", false);
		setBoolean(mode, "previouscool", false);

		invokeCheckCool(mode, engine);

		assertFalse(readBoolean(mode, "cool"));
		assertFalse(((boolean[]) readField(mode, "coolsection"))[0]);
	}

	@Test
	void checkCoolDoesNothingWhenLevelModuloIsBelow70() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statistics.level = 60;  // 60 % 100 = 60 < 70
		setBoolean(mode, "coolchecked", false);
		setBoolean(mode, "cool", false);

		invokeCheckCool(mode, engine);

		assertFalse(readBoolean(mode, "cool"), "level % 100 < 70 skips cool check");
		assertFalse(readBoolean(mode, "coolchecked"));
	}

	@Test
	void checkCoolDisplaysSoundWhenLevelReaches82AndCoolIsTrue() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statistics.level = 82;
		setBoolean(mode, "cool", true);
		setBoolean(mode, "cooldisplayed", false);
		setBoolean(mode, "coolchecked", true);  // already checked at 70
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 3000;

		invokeCheckCool(mode, engine);

		assertTrue(readBoolean(mode, "cooldisplayed"));
		assertEquals(180, readInt(mode, "cooldispframe"));
	}

	// -----------------------------------------------------------------------
	// checkRegret
	// -----------------------------------------------------------------------

	@Test
	void checkRegretTriggersWhenSectionLastTimeExceedsThreshold() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// section 0, tableTimeRegret[0] = 5400
		setInt(mode, "sectionlasttime", 5500);
		setInt(mode, "grade", 5);
		setInt(mode, "coolcount", 2);
		setBoolean(mode, "previouscool", true);

		invokeCheckRegret(mode, engine, 0);  // levelb=0 -> section=0

		assertFalse(readBoolean(mode, "previouscool"));
		assertEquals(1, readInt(mode, "coolcount"), "coolcount decremented");
		assertEquals(4, readInt(mode, "grade"), "grade decremented");
		assertEquals(180, readInt(mode, "regretdispframe"));
		assertTrue(((boolean[]) readField(mode, "regretsection"))[0]);
	}

	@Test
	void checkRegretDoesNotTriggerWhenSectionLastTimeIsAtThreshold() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// section 0, tableTimeRegret[0] = 5400
		setInt(mode, "sectionlasttime", 5400);  // == threshold, not >
		setInt(mode, "grade", 5);

		invokeCheckRegret(mode, engine, 0);

		assertEquals(5, readInt(mode, "grade"), "grade not decremented when not exceeding threshold");
		assertFalse(((boolean[]) readField(mode, "regretsection"))[0]);
	}

	@Test
	void checkRegretClampsGradeToZero() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionlasttime", 5500);
		setInt(mode, "grade", 0);  // already at floor

		invokeCheckRegret(mode, engine, 0);

		assertEquals(0, readInt(mode, "grade"), "grade never goes below 0");
	}

	@Test
	void checkRegretClampsCoolcountToZero() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionlasttime", 5500);
		setInt(mode, "coolcount", 0);  // already at floor

		invokeCheckRegret(mode, engine, 0);

		assertEquals(0, readInt(mode, "coolcount"), "coolcount never goes below 0");
	}

	// -----------------------------------------------------------------------
	// getGradeName
	// -----------------------------------------------------------------------

	@Test
	void getGradeNameReturnsCorrectNamesForValidGrades() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		assertEquals("9", invokeGetGradeName(mode, 0));
		assertEquals("S1", invokeGetGradeName(mode, 9));
		assertEquals("M1", invokeGetGradeName(mode, 18));
		assertEquals("GM", invokeGetGradeName(mode, 32));
	}

	@Test
	void getGradeNameReturnsNAForOutOfRangeValues() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		assertEquals("N/A", invokeGetGradeName(mode, -1));
		assertEquals("N/A", invokeGetGradeName(mode, 33));
		assertEquals("N/A", invokeGetGradeName(mode, 100));
	}

	// -----------------------------------------------------------------------
	// isAnyExam
	// -----------------------------------------------------------------------

	@Test
	void isAnyExamReturnsTrueWhenPromotionFlagSet() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "promotionFlag", true);
		setBoolean(mode, "demotionFlag", false);

		assertTrue(invokeIsAnyExam(mode));
	}

	@Test
	void isAnyExamReturnsTrueWhenDemotionFlagSet() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "promotionFlag", false);
		setBoolean(mode, "demotionFlag", true);

		assertTrue(invokeIsAnyExam(mode));
	}

	@Test
	void isAnyExamReturnsFalseWhenNeitherFlagSet() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "promotionFlag", false);
		setBoolean(mode, "demotionFlag", false);

		assertFalse(invokeIsAnyExam(mode));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GradeMania3Mode mode) {
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

	private static void invokeLoadSetting(GradeMania3Mode mode, CustomProperties prop) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(GradeMania3Mode mode, CustomProperties prop) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSetAverageSectionTime(GradeMania3Mode mode) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStMedalCheck(GradeMania3Mode mode, GameEngine engine, int sectionNumber) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("stMedalCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, sectionNumber);
	}

	private static int invokeGetMedalFontColor(GradeMania3Mode mode, int medalColor) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("getMedalFontColor", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, medalColor);
	}

	private static void invokeCheckCool(GradeMania3Mode mode, GameEngine engine) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("checkCool", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeCheckRegret(GradeMania3Mode mode, GameEngine engine, int levelb) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("checkRegret", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, levelb);
	}

	private static String invokeGetGradeName(GradeMania3Mode mode, int g) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("getGradeName", int.class);
		m.setAccessible(true);
		return (String) m.invoke(mode, g);
	}

	private static boolean invokeIsAnyExam(GradeMania3Mode mode) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("isAnyExam");
		m.setAccessible(true);
		return (boolean) m.invoke(mode);
	}
}
