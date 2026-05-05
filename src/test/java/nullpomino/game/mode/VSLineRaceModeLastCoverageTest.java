package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Targets remaining uncovered lines in VSLineRaceMode:
 * 199-248 (onSetting cursor cases), 376-377 (3-digit lines rendering),
 * 384 (2ND display), 410-417 (big-side-next layout wins display).
 */
class VSLineRaceModeLastCoverageTest {

	private GameEngine singleEngine(VSLineRaceMode mode, GameManager mgr) {
		mgr.mode = mode;
		mgr.init();
		mgr.engine[0].init();
		mode.playerInit(mgr.engine[0], 0);
		return mgr.engine[0];
	}

	@Test
	void onSettingAllCursorCases() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = singleEngine(mode, mgr);
		e.owner.replayMode = false;
		e.statc[4] = 0;
		setInt(mode, "menuTime", 5);

		// Case 1: denominator
		setInt(mode, "menuCursor", 1);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(e.speed.denominator < 99999);

		// Case 2: ARE
		setInt(mode, "menuCursor", 2);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);
		assertTrue(e.speed.are >= 0);

		// Case 3: ARE line
		setInt(mode, "menuCursor", 3);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);

		// Case 4: line delay
		setInt(mode, "menuCursor", 4);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);

		// Case 5: lockDelay
		setInt(mode, "menuCursor", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);

		// Case 6: DAS
		setInt(mode, "menuCursor", 6);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);

		// Cases 7-8: preset
		setInt(mode, "menuCursor", 7);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);

		// Case 9: goal lines
		setInt(mode, "menuCursor", 9);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);

		// Case 10: big toggle
		setInt(mode, "menuCursor", 10);
		setBoolOnArray(mode, "big", 0, false);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readBoolOnArray(mode, "big", 0));

		// Case 11: SE toggle
		setInt(mode, "menuCursor", 11);
		setBoolOnArray(mode, "enableSE", 0, true);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertFalse(readBoolOnArray(mode, "enableSE", 0));

		// Case 12: bgmno
		setInt(mode, "menuCursor", 12);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);
	}

	@Test
	void renderLastThreeDigitLines() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = singleEngine(mode, mgr);
		// Also init engine 1
		mgr.engine[1].init();
		mode.playerInit(mgr.engine[1], 1);
		e.gameStarted = true;
		e.stat = GameEngine.Status.MOVE;
		e.statistics.lines = 10;
		e.statistics.time = 3000;
		setIntOnArray(mode, "goalLines", 0, 100);
		mgr.engine[1].statistics.lines = 50;
		mgr.engine[1].gameStarted = true;
		e.owner.replayMode = false;
		assertDoesNotThrow(() -> mode.renderLast(e, 0));
	}

	@Test
	void renderLastOneDigitRemaining() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = singleEngine(mode, mgr);
		mgr.engine[1].init();
		mode.playerInit(mgr.engine[1], 1);
		e.gameStarted = true;
		e.stat = GameEngine.Status.MOVE;
		e.statistics.lines = 3;
		e.statistics.time = 3000;
		setIntOnArray(mode, "goalLines", 0, 5);
		mgr.engine[1].statistics.lines = 10;
		mgr.engine[1].gameStarted = true;
		e.owner.replayMode = false;
		assertDoesNotThrow(() -> mode.renderLast(e, 0));
	}

	@Test
	void renderLastSecondDisplay() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = singleEngine(mode, mgr);
		mgr.engine[1].init();
		mode.playerInit(mgr.engine[1], 1);
		e.gameStarted = true;
		e.stat = GameEngine.Status.MOVE;
		e.statistics.lines = 3;
		e.statistics.time = 3000;
		setIntOnArray(mode, "goalLines", 0, 5);
		setIntOnArray(mode, "goalLines", 1, 10);
		setIntOnArray(mode, "winCount", 0, 5);
		setIntOnArray(mode, "winCount", 1, 3);
		mgr.engine[1].statistics.lines = 8;
		mgr.engine[1].gameStarted = true;
		e.owner.replayMode = false;
		// Set sidenext and bigsidenext for nextDisplayType 2
		setField(e.owner.receiver, "sidenext", true);
		setField(e.owner.receiver, "bigsidenext", true);
		assertDoesNotThrow(() -> mode.renderLast(e, 0));
	}

	// ---- helpers ----

	private static java.lang.reflect.Field ff(Object o, String n) throws Exception {
		Class<?> c = o.getClass();
		while (c != null) {
			try { java.lang.reflect.Field f = c.getDeclaredField(n); f.setAccessible(true); return f; }
			catch (NoSuchFieldException ex) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(n);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		ff(o, n).setInt(o, v);
	}

	private static void setIntOnArray(Object o, String n, int idx, int v) throws Exception {
		java.lang.reflect.Field f = ff(o, n);
		((int[])f.get(o))[idx] = v;
	}

	private static boolean readBoolOnArray(Object o, String n, int idx) throws Exception {
		java.lang.reflect.Field f = ff(o, n);
		return ((boolean[])f.get(o))[idx];
	}

	private static void setBoolOnArray(Object o, String n, int idx, boolean v) throws Exception {
		java.lang.reflect.Field f = ff(o, n);
		((boolean[])f.get(o))[idx] = v;
	}

	private static void setField(Object o, String n, boolean v) throws Exception {
		java.lang.reflect.Field f = ff(o, n);
		f.setBoolean(o, v);
	}
}
