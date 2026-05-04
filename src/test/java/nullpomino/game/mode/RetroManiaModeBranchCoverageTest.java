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
 * Branch coverage for {@link RetroManiaMode}: covers replay-mode playerInit,
 * menu case 2/3 (big/poweron toggle), and renderLast score display branches.
 */
class RetroManiaModeBranchCoverageTest {

	@Test
	void playerInitReplayModeLoadsReplayProp() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		mode.playerInit(e, 0);
		// loadSetting(owner.replayProp) should have been called without NPE
		// Verify defaults were loaded
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, readInt(mode, "gametype"));
	}

	@Test
	void onSettingTogglesBig() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(mode, "menuCursor", 2);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;

		mode.onSetting(e, 0);

		// big should toggle from false to true
		assertTrue(readBoolean(mode, "big"));
	}

	@Test
	void onSettingTogglesPoweron() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(mode, "menuCursor", 3);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;

		mode.onSetting(e, 0);

		// poweron should toggle from false to true
		assertTrue(readBoolean(mode, "poweron"));
	}

	@Test
	void renderLastScoreWithoutLastscore() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		// lastscore == 0 → condition at line 295 is true → line 296 (plain score)
		setInt(mode, "lastscore", 0);
		e.statistics.score = 5000;

		mode.renderLast(e, 0);

		assertEquals(5000, e.statistics.score);
	}

	@Test
	void renderLastScoreWithLastscoreSuffix() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		// lastscore != 0 && scgettime < 120 → else branch at line 298
		setInt(mode, "lastscore", 300);
		setInt(mode, "scgettime", 50);
		e.statistics.score = 5000;

		mode.renderLast(e, 0);

		assertEquals(300, readInt(mode, "lastscore"));
		assertEquals(50, readInt(mode, "scgettime"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(RetroManiaMode mode) {
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

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
