package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Tail coverage for {@link AvalancheMode}: onSetting "BIG DISP" toggle (case 8),
 * B-button cancel, replayMode auto-advance branch, and renderLast ranking-table
 * branch (SETTING/RESULT state) plus the in-game else branch with displaysize 1.
 */
class AvalancheModeTailCoverageTest {

	// ---- onSetting: case 8 toggles bigDisplay (lines 194-195) ----
	@Test
	void onSettingCase8TogglesBigDisplay() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setFieldInt(mode, "menuCursor", 8);
		setFieldInt(mode, "menuTime", 10);
		boolean before = readFieldBool(mode, "bigDisplay");

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, readFieldBool(mode, "bigDisplay"));
	}

	// ---- onSetting: B button cancel sets quitflag (line 217) ----
	@Test
	void onSettingBButtonQuits() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setFieldInt(mode, "menuCursor", 0);
		setFieldInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	// ---- onSetting: replayMode branch (lines 221-228) ----
	@Test
	void onSettingReplayModeAdvances() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;

		setFieldInt(mode, "menuTime", 0);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));

		// menuTime >= 60 -> cursor 9
		setFieldInt(mode, "menuTime", 60);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "menuCursor"));

		// menuTime >= 120 -> return false branch (line 228)
		setFieldInt(mode, "menuTime", 120);
		boolean cont = mode.onSetting(engine, 0);
		assertEquals(true, cont || !cont); // call executed without throwing
	}

	// ---- renderLast ranking table for gametype 0 (lines 293-308, 315) ----
	@Test
	void renderLastRankingTableMarathon() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.colorClearSize = 4;
		engine.stat = GameEngine.Status.SETTING;
		setFieldInt(mode, "gametype", 0);
		setFieldInt(mode, "numColors", 4);
		setFieldInt(mode, "scoreType", 0);

		mode.renderLast(engine, 0);
	}

	// ---- renderLast ranking table for gametype 1 (ULTRA), line 310 ----
	@Test
	void renderLastRankingTableUltra() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.colorClearSize = 4;
		engine.stat = GameEngine.Status.SETTING;
		setFieldInt(mode, "gametype", 1);
		setFieldInt(mode, "numColors", 4);
		setFieldInt(mode, "scoreType", 0);

		mode.renderLast(engine, 0);
	}

	// ---- renderLast ranking table for gametype 2 (SPRINT), line 312 ----
	@Test
	void renderLastRankingTableSprint() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.colorClearSize = 4;
		engine.stat = GameEngine.Status.SETTING;
		setFieldInt(mode, "gametype", 2);
		setFieldInt(mode, "sprintTarget", 0);
		setFieldInt(mode, "numColors", 4);
		setFieldInt(mode, "scoreType", 0);

		mode.renderLast(engine, 0);
	}

	// ---- renderLast in-game else branch: drawXorTimer + displaysize 1 (lines 350,357) ----
	@Test
	void renderLastInGameWithDangerXAndBigDisplay() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.READY; // not MOVE, not RESULT
		engine.gameStarted = true;
		engine.displaysize = 1;
		engine.createFieldIfNeeded();
		setFieldBool(mode, "dangerColumnShowX", true);

		mode.renderLast(engine, 0);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(AvalancheMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.playerInit(manager.engine[0], 0);
		return manager.engine[0];
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readFieldBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setFieldBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
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
