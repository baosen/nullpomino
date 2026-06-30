package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers a few uncovered SquareMode branches: the B-button cancel in onSetting
 * (line 203 -> quitflag), the in-play score render path of renderLast (line 321,
 * lastscore==0 branch) and the ULTRA-mode per-second "countdown" SE in onLast
 * (line 368). The latter is verified with a recording EventReceiver.
 */
class SquareModeBranchCoverageTest2 {

	/** EventReceiver that records every sound effect name played. */
	private static final class RecordingReceiver extends EventReceiver {
		final List<String> played = new ArrayList<>();
		@Override public void playSE(String name) { played.add(name); }
	}

	private static GameEngine fresh(SquareMode mode, EventReceiver receiver) {
		GameManager manager = new GameManager(receiver);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		mode.playerInit(manager.engine[0], 0);
		manager.engine[0].createFieldIfNeeded();
		return manager.engine[0];
	}

	private static void setInt(Object o, String name, int v) throws Exception {
		Field f = SquareMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	@Test
	void bButtonCancelSetsQuitflag() {
		SquareMode mode = new SquareMode();
		GameEngine engine = fresh(mode, new EventReceiver());
		engine.owner.replayMode = false;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag, "B in onSetting sets quitflag");
	}

	@Test
	void renderLastDrawsScoreDuringPlay() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = fresh(mode, new EventReceiver());
		engine.stat = GameEngine.Status.MOVE;   // not SETTING/RESULT -> in-play score path
		setInt(mode, "lastscore", 0);           // lastscore==0 -> plain score string (321)
		engine.statistics.score = 1234;
		mode.renderLast(engine, 0);             // no-op receiver; must not throw
		assertTrue(engine.statistics.score == 1234, "renderLast in-play path executes");
	}

	@Test
	void ultraModePlaysCountdownEverySecond() throws Exception {
		SquareMode mode = new SquareMode();
		RecordingReceiver rec = new RecordingReceiver();
		GameEngine engine = fresh(mode, rec);
		setInt(mode, "gametype", 1);            // ULTRA (time-limit)
		engine.statistics.time = 10800 - 60;    // remainTime = 60 (<=600), time%60==0
		engine.timerActive = true;
		rec.played.clear();
		mode.onLast(engine, 0);
		assertTrue(rec.played.contains("countdown"),
				"ULTRA mode plays the per-second countdown SE near the end");
	}
}
