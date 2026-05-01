package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.BooleanMenuItem;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * Pins the AbstractMode protected helpers that every concrete mode
 * relies on for menu I/O and the SETTING / RESULT screens. Driven
 * through reflection because the helpers are intentionally
 * package-private to discourage misuse from outside the mode hierarchy.
 *
 * <p>The receiver is a vanilla {@link EventReceiver} whose drawMenuFont
 * overloads are no-op stubs, so the draw helpers exercise their full
 * routing without touching SDL.
 */
class AbstractModeRenderTest {

	@Test
	void addMenuItemsAppendsInDeclarationOrder() throws Exception {
		StubMode mode = new StubMode();
		IntegerMenuItem first = integer("startlevel", 0);
		BooleanMenuItem second = bool("big", false);

		invokeAddMenuItems(mode, first, second);

		assertEquals(2, modeMenu(mode).size());
		assertEquals("startlevel", modeMenu(mode).get(0).name);
		assertEquals("big", modeMenu(mode).get(1).name);
		assertEquals(2, mode.getMenuItemCount(),
				"getMenuItemCount must report the populated menu length");
	}

	@Test
	void loadSettingAndSaveSettingUseTheSubclassPropertyName() throws Exception {
		StubMode source = new StubMode();
		source.setPropName("rumbledtest");
		IntegerMenuItem level = integer("startlevel", 0);
		level.value = 11;
		BooleanMenuItem enableB2B = bool("enableB2B", false);
		enableB2B.value = true;
		invokeAddMenuItems(source, level, enableB2B);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		// Both keys land under the propName.
		assertEquals(11, prop.getProperty("rumbledtest.startlevel", -1));
		assertEquals(true, prop.getProperty("rumbledtest.enableB2B", false));

		// Round-trip into a fresh mode with matching menu items reproduces
		// the field values.
		StubMode destination = new StubMode();
		destination.setPropName("rumbledtest");
		IntegerMenuItem level2 = integer("startlevel", 0);
		BooleanMenuItem enableB2B2 = bool("enableB2B", false);
		invokeAddMenuItems(destination, level2, enableB2B2);

		invokeLoadSetting(destination, prop);

		assertEquals(11, level2.value);
		assertEquals(true, enableB2B2.value);
	}

	@Test
	void loadSettingFallsBackToDefaultsWhenPropertiesAreMissing() throws Exception {
		StubMode mode = new StubMode();
		mode.setPropName("missing");
		IntegerMenuItem level = integer("startlevel", 7);
		level.value = 99;
		invokeAddMenuItems(mode, level);

		// Empty property file → load applies the menu item's default.
		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(7, level.value, "missing key must restore the menu default");
	}

	@Test
	void initMenuFourArgFormSetsYColorAndStatcAndDoesNotTouchOtherFields() throws Exception {
		StubMode mode = new StubMode();

		invokeInitMenu(mode, 12, EventReceiver.COLOR_GREEN, 5);

		assertEquals(12, readInt(mode, "menuY"));
		assertEquals(EventReceiver.COLOR_GREEN, readInt(mode, "menuColor"));
		assertEquals(5, readInt(mode, "statcMenu"));
	}

	@Test
	void initMenuThreeArgFormZeroesYAndOnlyTouchesColorAndStatc() throws Exception {
		StubMode mode = new StubMode();
		setInt(mode, "menuY", 99); // sentinel that the 3-arg form must reset

		invokeInitMenuShort(mode, EventReceiver.COLOR_RED, 3);

		assertEquals(0, readInt(mode, "menuY"),
				"the 3-arg initMenu must zero menuY (legacy contract)");
		assertEquals(EventReceiver.COLOR_RED, readInt(mode, "menuColor"));
		assertEquals(3, readInt(mode, "statcMenu"));
	}

	@Test
	void drawMenuPairwiseAdvancesYAndStatcMenuForEveryLabelValueRow() throws Exception {
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		EventReceiver receiver = engine.owner.receiver;
		setInt(mode, "menuY", 0);
		setInt(mode, "statcMenu", 0);
		setInt(mode, "menuCursor", 999); // != statcMenu so the highlight branch is skipped

		invokeDrawMenu(mode, engine, receiver, "LEVEL", "1", "BIG", "OFF");

		// menuY advances on every row (both label and value); statcMenu
		// only advances on the odd-indexed value rows.
		assertEquals(4, readInt(mode, "menuY"));
		assertEquals(2, readInt(mode, "statcMenu"));
	}

	@Test
	void drawMenuHighlightBranchAdvancesStatcMenuOnlyForValueRows() throws Exception {
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		EventReceiver receiver = engine.owner.receiver;
		setInt(mode, "menuY", 0);
		setInt(mode, "statcMenu", 0);
		setInt(mode, "menuCursor", 0); // matches statcMenu so the highlight branch fires

		invokeDrawMenu(mode, engine, receiver, "LEVEL", "1");

		// menuY advances on every row; statcMenu only on the value row.
		assertEquals(2, readInt(mode, "menuY"));
		assertEquals(1, readInt(mode, "statcMenu"));
	}

	@Test
	void drawMenuSevenArgFormResetsCountersBeforeDrawing() throws Exception {
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		EventReceiver receiver = engine.owner.receiver;
		setInt(mode, "menuY", 99);
		setInt(mode, "menuColor", -1);
		setInt(mode, "statcMenu", 99);

		invokeDrawMenuFull(mode, engine, receiver, 4, EventReceiver.COLOR_BLUE, 0,
				"GHOST", "ON");

		// 7-arg form resets to (y=4, color=BLUE, statc=0) before drawing,
		// then runs two rows of drawMenu — menuY advances on each, statcMenu
		// only on the odd value row.
		assertEquals(6, readInt(mode, "menuY"));
		assertEquals(EventReceiver.COLOR_BLUE, readInt(mode, "menuColor"));
		assertEquals(1, readInt(mode, "statcMenu"));
	}

	@Test
	void drawMenuCompactPairwiseAdvancesYAndStatcMenu() throws Exception {
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		EventReceiver receiver = engine.owner.receiver;
		setInt(mode, "menuY", 0);
		setInt(mode, "statcMenu", 0);
		setInt(mode, "menuCursor", 999);

		invokeDrawMenuCompact(mode, engine, receiver, "LEVEL", "1", "BIG", "OFF");

		assertEquals(2, readInt(mode, "menuY"));
		assertEquals(2, readInt(mode, "statcMenu"));
	}

	@Test
	void drawResultRankRendersOnlyWhenRankIsNonNegative() throws Exception {
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		EventReceiver receiver = engine.owner.receiver;

		// rank == -1 → both halves of the if-branch must be skipped.
		invokeDrawResultRank(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, -1);
		// rank >= 0 → method runs to completion (receiver is a no-op stub).
		invokeDrawResultRank(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, 0);
		invokeDrawResultRank(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, 9);
	}

	@Test
	void drawResultNetRankAndNetRankDailyAcceptUnrankedAndRankedInputs() throws Exception {
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		EventReceiver receiver = engine.owner.receiver;

		invokeDrawResultNetRank(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, -1);
		invokeDrawResultNetRank(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, 5);
		invokeDrawResultNetRankDaily(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, -1);
		invokeDrawResultNetRankDaily(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, 3);
	}

	@Test
	void drawResultStatsRoutesEachStatisticThroughItsOwnReceiverCall() throws Exception {
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		EventReceiver receiver = engine.owner.receiver;
		// Populate fields so each branch has a printable input — the
		// EventReceiver is still a no-op so we just need this not to throw.
		engine.statistics.score = 100;
		engine.statistics.lines = 5;
		engine.statistics.time = 360;
		engine.statistics.level = 2;
		engine.statistics.totalPieceLocked = 25;
		engine.statistics.maxCombo = 3;
		engine.statistics.maxChain = 4;
		engine.statistics.levelDispAdd = 1;

		// Cover every Statistic enum value in one call so each switch arm
		// runs against the populated fields.
		Object[] stats = readEnumValues();
		invokeDrawResultStats(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, stats);
	}

	@Test
	void drawResultDelegatesToScaleVariantWithUnitScale() throws Exception {
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		EventReceiver receiver = engine.owner.receiver;

		invokeDrawResult(mode, engine, receiver, 0, EventReceiver.COLOR_WHITE, "SCORE", "100");
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static IntegerMenuItem integer(String name, int defaultValue) {
		return new IntegerMenuItem(name, name.toUpperCase(),
				EventReceiver.COLOR_WHITE, defaultValue, 0, 100);
	}

	private static BooleanMenuItem bool(String name, boolean defaultValue) {
		return new BooleanMenuItem(name, name.toUpperCase(),
				EventReceiver.COLOR_WHITE, defaultValue);
	}

	@SuppressWarnings("unchecked")
	private static java.util.ArrayList<nullpomino.game.menu.AbstractMenuItem<?>> modeMenu(StubMode mode) throws Exception {
		Field f = AbstractMode.class.getDeclaredField("menu");
		f.setAccessible(true);
		return (java.util.ArrayList<nullpomino.game.menu.AbstractMenuItem<?>>) f.get(mode);
	}

	private static int readInt(StubMode mode, String name) throws Exception {
		Field f = AbstractMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static void setInt(StubMode mode, String name, int value) throws Exception {
		Field f = AbstractMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void invokeAddMenuItems(StubMode mode, nullpomino.game.menu.AbstractMenuItem<?>... items) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("addMenuItems",
				nullpomino.game.menu.AbstractMenuItem[].class);
		m.setAccessible(true);
		m.invoke(mode, (Object) items);
	}

	private static void invokeLoadSetting(StubMode mode, CustomProperties prop) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(StubMode mode, CustomProperties prop) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeInitMenu(StubMode mode, int y, int color, int statc) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("initMenu", int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, y, color, statc);
	}

	private static void invokeInitMenuShort(StubMode mode, int color, int statc) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("initMenu", int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, color, statc);
	}

	private static void invokeDrawMenu(StubMode mode, GameEngine engine, EventReceiver receiver, String... str) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("drawMenu",
				GameEngine.class, int.class, EventReceiver.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, receiver, (Object) str);
	}

	private static void invokeDrawMenuFull(StubMode mode, GameEngine engine, EventReceiver receiver,
			int y, int color, int statc, String... str) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("drawMenu",
				GameEngine.class, int.class, EventReceiver.class,
				int.class, int.class, int.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, receiver, y, color, statc, (Object) str);
	}

	private static void invokeDrawMenuCompact(StubMode mode, GameEngine engine, EventReceiver receiver, String... str) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("drawMenuCompact",
				GameEngine.class, int.class, EventReceiver.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, receiver, (Object) str);
	}

	private static void invokeDrawResultRank(StubMode mode, GameEngine engine, EventReceiver receiver,
			int y, int color, int rank) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("drawResultRank",
				GameEngine.class, int.class, EventReceiver.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, receiver, y, color, rank);
	}

	private static void invokeDrawResultNetRank(StubMode mode, GameEngine engine, EventReceiver receiver,
			int y, int color, int rank) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("drawResultNetRank",
				GameEngine.class, int.class, EventReceiver.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, receiver, y, color, rank);
	}

	private static void invokeDrawResultNetRankDaily(StubMode mode, GameEngine engine, EventReceiver receiver,
			int y, int color, int rank) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("drawResultNetRankDaily",
				GameEngine.class, int.class, EventReceiver.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, receiver, y, color, rank);
	}

	private static void invokeDrawResultStats(StubMode mode, GameEngine engine, EventReceiver receiver,
			int y, int color, Object[] stats) throws Exception {
		Class<?> statClass = Class.forName("nullpomino.game.mode.AbstractMode$Statistic");
		Object array = java.lang.reflect.Array.newInstance(statClass, stats.length);
		for(int i = 0; i < stats.length; i++) java.lang.reflect.Array.set(array, i, stats[i]);

		Method m = AbstractMode.class.getDeclaredMethod("drawResultStats",
				GameEngine.class, int.class, EventReceiver.class, int.class, int.class, array.getClass());
		m.setAccessible(true);
		m.invoke(mode, engine, 0, receiver, y, color, array);
	}

	private static void invokeDrawResult(StubMode mode, GameEngine engine, EventReceiver receiver,
			int y, int color, String... str) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("drawResult",
				GameEngine.class, int.class, EventReceiver.class, int.class, int.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, receiver, y, color, (Object) str);
	}

	private static Object[] readEnumValues() throws Exception {
		Class<?> statClass = Class.forName("nullpomino.game.mode.AbstractMode$Statistic");
		Object[] values = (Object[]) statClass.getMethod("values").invoke(null);
		assertNotNull(values);
		assertTrue(values.length > 0);
		return values;
	}

	/** AbstractMode subclass that exposes propName for the I/O round-trips. */
	private static final class StubMode extends AbstractMode {
		void setPropName(String name) {
			try {
				Field f = AbstractMode.class.getDeclaredField("propName");
				f.setAccessible(true);
				f.set(this, name);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		}
	}
}
