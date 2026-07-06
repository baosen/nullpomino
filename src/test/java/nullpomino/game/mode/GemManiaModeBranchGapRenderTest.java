package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Fills remaining branch-coverage gaps in {@link GemManiaMode} rendering:
 * renderSetting replay/training labels, renderReady pre-countdown,
 * renderLast ranking gate / next-display-type / flash and section-time
 * branches, renderCustom clear/skip/time-up screens, renderGameOver
 * continue window, and renderResult section pages.
 */
class GemManiaModeBranchGapRenderTest {

	// ---------------------------------------------------------------
	// renderSetting / renderReady
	// ---------------------------------------------------------------

	@Test
	void renderSettingReplayModeAndTrainingOn() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		setInt(mode, "editModeScreen", 0);
		setInt(mode, "trainingType", 1);

		assertDoesNotThrow(() -> mode.renderSetting(engine, 0),
				"Replay-mode setting screen with training ON renders");
	}

	@Test
	void renderReadyBeforeReadyStartDrawsNothing() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.readyStart = 45;
		engine.statc[0] = 0;

		assertDoesNotThrow(() -> mode.renderReady(engine, 0),
				"Nothing drawn before the ready countdown starts");
	}

	// ---------------------------------------------------------------
	// renderLast: ranking table branches
	// ---------------------------------------------------------------

	@Test
	void renderLastRandomNextHeaderAndType() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setBoolean(mode, "randomnext", true);

		assertDoesNotThrow(() -> mode.renderLast(engine, 0),
				"Random-next header and type-1 ranking table render");
	}

	@Test
	void renderLastNextDisplayType2UsesSmallScale() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		QuietReceiver rec = new QuietReceiver();
		rec.nextDisplayType = 2;
		GameEngine engine = freshEngine(mode, rec);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;

		assertDoesNotThrow(() -> mode.renderLast(engine, 0),
				"Side-next display type renders the compact ranking table");
	}

	@Test
	void renderLastResultStatusNonReplayShowsRanking() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.RESULT;

		assertDoesNotThrow(() -> mode.renderLast(engine, 0),
				"Result screen outside replay shows the ranking table");
	}

	@Test
	void renderLastResultStatusInReplayShowsScoreboard() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		engine.stat = GameEngine.Status.RESULT;

		assertDoesNotThrow(() -> mode.renderLast(engine, 0),
				"Result screen in replay falls back to the in-game scoreboard");
	}

	@Test
	void renderLastRankingGateEachConditionFalse() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;

		// 1) non-default start stage
		setInt(mode, "startstage", 1);
		mode.renderLast(engine, 0);
		setInt(mode, "startstage", 0);

		// 2) 20G mode
		setBoolean(mode, "always20g", true);
		mode.renderLast(engine, 0);
		setBoolean(mode, "always20g", false);

		// 3) training mode
		setInt(mode, "trainingType", 1);
		mode.renderLast(engine, 0);
		setInt(mode, "trainingType", 0);

		// 4) non-default next counter
		setInt(mode, "startnextc", 5);
		mode.renderLast(engine, 0);
		setInt(mode, "startnextc", 0);

		// 5) custom stage set
		setInt(mode, "stageset", 0);
		mode.renderLast(engine, 0);
		setInt(mode, "stageset", -1);

		// 6) AI in control
		engine.ai = new DummyAI();
		assertDoesNotThrow(() -> mode.renderLast(engine, 0));
		engine.ai = null;
	}

	// ---------------------------------------------------------------
	// renderLast: in-game scoreboard branches
	// ---------------------------------------------------------------

	@Test
	void renderLastLevelAndGravityBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;

		// negative level clamps to 0, negative gravity means 20G meter
		setInt(mode, "speedlevel", -1);
		engine.speed.gravity = -1;
		mode.renderLast(engine, 0);

		// positive level and finite gravity
		setInt(mode, "speedlevel", 5);
		engine.speed.gravity = 512;
		assertDoesNotThrow(() -> mode.renderLast(engine, 0));
	}

	@Test
	void renderLastStageTimeFlashBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "stagetimeStart", 100);

		// all flash conditions true
		engine.timerActive = true;
		setInt(mode, "stagetimeNow", 400);
		mode.renderLast(engine, 0);

		// wrong frame parity
		setInt(mode, "stagetimeNow", 599);
		mode.renderLast(engine, 0);

		// plenty of time left
		setInt(mode, "stagetimeNow", 1000);
		mode.renderLast(engine, 0);

		// timer inactive
		engine.timerActive = false;
		setInt(mode, "stagetimeNow", 400);
		assertDoesNotThrow(() -> mode.renderLast(engine, 0));
	}

	@Test
	void renderLastLimitTimeExtendAndFlash() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "limittimeStart", 100);

		// time-extend caption plus all flash conditions true
		engine.timerActive = true;
		setInt(mode, "timeextendDisp", 10);
		setInt(mode, "timeextendSeconds", 3);
		setInt(mode, "limittimeNow", 400);
		mode.renderLast(engine, 0);

		// no extend caption, wrong parity
		setInt(mode, "timeextendDisp", 0);
		setInt(mode, "limittimeNow", 599);
		mode.renderLast(engine, 0);

		// plenty of time left
		setInt(mode, "limittimeNow", 1000);
		mode.renderLast(engine, 0);

		// timer inactive
		engine.timerActive = false;
		setInt(mode, "limittimeNow", 400);
		assertDoesNotThrow(() -> mode.renderLast(engine, 0));
	}

	@Test
	void renderLastSectionTimeNullArray() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setBoolean(mode, "showsectiontime", true);
		findField(mode.getClass(), "sectiontime").set(mode, null);

		assertDoesNotThrow(() -> mode.renderLast(engine, 0),
				"Section time display skipped when the array is missing");
	}

	@Test
	void renderLastSectionTimeWithEndingNonzero() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.ending = 1;
		setBoolean(mode, "showsectiontime", true);
		setInt(mode, "stage", 0);
		int[] sectiontime = (int[]) findField(mode.getClass(), "sectiontime").get(mode);
		sectiontime[0] = 100;
		sectiontime[1] = -1;
		sectiontime[2] = -2;

		assertDoesNotThrow(() -> mode.renderLast(engine, 0),
				"Current-stage marker suppressed during the ending");
	}

	// ---------------------------------------------------------------
	// renderCustom
	// ---------------------------------------------------------------

	@Test
	void renderCustomClearBlinkBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "clearflag", true);
		setInt(mode, "timeextendStageClearSeconds", 10);

		// even frame, extension still counting: blink on
		engine.statc[0] = 2;
		engine.statc[1] = 0;
		mode.renderCustom(engine, 0);

		// odd frame: blink off
		engine.statc[0] = 1;
		mode.renderCustom(engine, 0);

		// extension finished: blink off
		engine.statc[0] = 2;
		engine.statc[1] = 1000;
		assertDoesNotThrow(() -> mode.renderCustom(engine, 0));
	}

	@Test
	void renderCustomSkipBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "clearflag", false);
		setBoolean(mode, "skipflag", true);

		// even frame, penalty still counting, normal mode
		engine.statc[0] = 2;
		engine.statc[1] = 0;
		setInt(mode, "trainingType", 0);
		mode.renderCustom(engine, 0);

		// odd frame, training mode hides the clear rate
		engine.statc[0] = 1;
		setInt(mode, "trainingType", 1);
		mode.renderCustom(engine, 0);

		// penalty finished
		engine.statc[0] = 2;
		engine.statc[1] = 2000;
		assertDoesNotThrow(() -> mode.renderCustom(engine, 0));
	}

	@Test
	void renderCustomTimeUpBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "clearflag", false);
		setBoolean(mode, "skipflag", false);
		engine.statc[0] = 2;
		engine.statc[1] = 0;

		// stage time still remaining: no screen at all
		setInt(mode, "stagetimeNow", 5);
		setInt(mode, "stagetimeStart", 100);
		mode.renderCustom(engine, 0);

		// stage has no time limit: no screen either
		setInt(mode, "stagetimeNow", 0);
		setInt(mode, "stagetimeStart", 0);
		mode.renderCustom(engine, 0);

		// genuine time-up in training mode hides the clear rate
		setInt(mode, "stagetimeNow", 0);
		setInt(mode, "stagetimeStart", 100);
		setInt(mode, "trainingType", 1);
		assertDoesNotThrow(() -> mode.renderCustom(engine, 0));
	}

	// ---------------------------------------------------------------
	// renderGameOver / renderResult
	// ---------------------------------------------------------------

	@Test
	void renderGameOverEndingOrNoContinueDrawsNothing() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 1;
		setBoolean(mode, "noContinue", false);
		mode.renderGameOver(engine, 0);

		engine.ending = 0;
		setBoolean(mode, "noContinue", true);
		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0));
	}

	@Test
	void renderGameOverAfterContinueWindow() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		setBoolean(mode, "noContinue", false);
		engine.statc[0] = engine.field.getHeight() + 1 + 600;

		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0),
				"Continue prompt hidden once the countdown expires");
	}

	@Test
	void renderGameOverCursorPositionsAndTraining() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		setBoolean(mode, "noContinue", false);
		engine.statc[0] = engine.field.getHeight() + 5;

		// YES highlighted, normal mode shows the 2-minute penalty
		engine.statc[1] = 0;
		setInt(mode, "trainingType", 0);
		mode.renderGameOver(engine, 0);

		// NO highlighted, training mode hides the penalty
		engine.statc[1] = 1;
		setInt(mode, "trainingType", 1);
		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0));
	}

	@Test
	void renderResultSectionPage2AndBeyond() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		int[] sectiontime = (int[]) findField(mode.getClass(), "sectiontime").get(mode);
		sectiontime[15] = 100;
		sectiontime[16] = -1;
		sectiontime[17] = -2;

		// page 3: second half of the section list
		engine.statc[1] = 2;
		mode.renderResult(engine, 0);

		// out-of-range page draws only the header
		engine.statc[1] = 3;
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static class QuietReceiver extends EventReceiver {
		int nextDisplayType = 0;

		@Override
		public int getNextDisplayType() {
			return nextDisplayType;
		}

		@Override
		public boolean saveProperties(String filename, CustomProperties prop) {
			return true;
		}

		@Override
		public void saveModeConfig(CustomProperties modeConfig) {
			// Do not touch shared config on disk
		}
	}

	private static GameEngine freshEngine(GemManiaMode mode) {
		return freshEngine(mode, new QuietReceiver());
	}

	private static GameEngine freshEngine(GemManiaMode mode, QuietReceiver rec) {
		GameManager manager = new GameManager(rec);
		manager.replayMode = false;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void setInt(GemManiaMode mode, String name, int value) throws Exception {
		findField(mode.getClass(), name).setInt(mode, value);
	}

	private static void setBoolean(GemManiaMode mode, String name, boolean value) throws Exception {
		findField(mode.getClass(), name).setBoolean(mode, value);
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
