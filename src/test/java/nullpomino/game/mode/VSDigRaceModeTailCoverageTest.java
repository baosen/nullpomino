package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Tail coverage for {@link VSDigRaceMode}: onSetting preset-number clamp
 * (case 7/8), fillGarbage sticky-skin connection branch, renderLast remaining-
 * line digit branches, 1ST/2ND comparison, and the big-side-next layout.
 */
class VSDigRaceModeTailCoverageTest {

	/** Receiver that can be told to use sticky skin and/or big-side-next. */
	static class TestReceiver extends EventReceiver {
		boolean sticky = false;
		int nextType = 0;
		@Override public boolean isStickySkin(GameEngine engine) { return sticky; }
		@Override public boolean isStickySkin(int skin) { return sticky; }
		@Override public int getNextDisplayType() { return nextType; }
	}

	// ---- onSetting case 7/8 preset clamp (lines 231-233) ----
	@Test
	void onSettingPresetNumberClamps() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = freshManager(mode, new EventReceiver());
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 7);
		setFieldInt(mode, "menuTime", 10);
		setIntArray(mode, "presetNumber", 0, 0);

		// LEFT from 0 -> wraps to 99
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertTrue(getIntArray(mode, "presetNumber")[0] == 99);

		// RIGHT from 99 -> wraps to 0
		setIntArray(mode, "presetNumber", 99, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertTrue(getIntArray(mode, "presetNumber")[0] == 0);
	}

	// ---- fillGarbage sticky-skin connection branch (lines 401-406) ----
	@Test
	void onReadyFillsGarbageWithStickyConnections() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		TestReceiver rec = new TestReceiver();
		rec.sticky = true;
		GameManager manager = freshManager(mode, rec);
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		setIntArray(mode, "goalLines", 10, 0);
		setIntArray(mode, "garbagePercent", 100, 0);
		engine.statc[0] = 0;

		mode.onReady(engine, 0);

		// Garbage was placed (field is no longer empty)
		assertTrue(engine.field != null && !engine.field.isEmpty());
	}

	// ---- renderLast digit branches + 1ST/2ND (lines 467-472, 478, 480) ----
	@Test
	void renderLastSingleDigitAnd1st() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = freshManager(mode, new EventReceiver());
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		mode.playerInit(manager.engine[1], 1);
		// Give player 0 a small garbage stack (1 digit remaining), enemy a large one
		setIntArray(mode, "goalLines", 3, 0);
		setIntArray(mode, "goalLines", 18, 1);
		setIntArray(mode, "garbagePercent", 100, 0);
		setIntArray(mode, "garbagePercent", 100, 1);
		engine.createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();
		fillFor(mode, engine, 0);
		fillFor(mode, manager.engine[1], 1);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastTwoDigitAnd2nd() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = freshManager(mode, new EventReceiver());
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		mode.playerInit(manager.engine[1], 1);
		// Player 0 has a large stack (2 digits), enemy small -> player 0 is 2ND
		setIntArray(mode, "goalLines", 18, 0);
		setIntArray(mode, "goalLines", 2, 1);
		setIntArray(mode, "garbagePercent", 100, 0);
		setIntArray(mode, "garbagePercent", 100, 1);
		engine.createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();
		fillFor(mode, engine, 0);
		fillFor(mode, manager.engine[1], 1);

		mode.renderLast(engine, 0);
	}

	// ---- renderLast big-side-next layout (lines 505-513) ----
	@Test
	void renderLastBigSideNextLayout() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		TestReceiver rec = new TestReceiver();
		rec.nextType = 2;
		GameManager manager = freshManager(mode, rec);
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		mode.playerInit(manager.engine[1], 1);
		manager.replayMode = false;
		engine.owner.replayMode = false;
		setIntArray(mode, "goalLines", 5, 0);
		setIntArray(mode, "goalLines", 5, 1);
		engine.createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();
		// winCount large enough to exercise the >=10 branch (line 511)
		setIntArray(mode, "winCount", 12, 0);

		mode.renderLast(engine, 0);
	}

	// ---- helpers ----

	private static GameManager freshManager(VSDigRaceMode mode, EventReceiver rec) {
		GameManager manager = new GameManager(rec);
		manager.mode = mode;
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		return manager;
	}

	private static void fillFor(VSDigRaceMode mode, GameEngine engine, int playerID) throws Exception {
		java.lang.reflect.Method m = VSDigRaceMode.class.getDeclaredMethod(
				"fillGarbage", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		return (int[]) findField(obj.getClass(), name).get(obj);
	}

	private static void setIntArray(Object obj, String name, int value, int index) throws Exception {
		((int[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
