package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine#update}'s status-driven dispatch table. The
 * method runs onFirst / onLast wrappers on both the mode and the
 * receiver, then routes through a per-Status switch that calls
 * exactly one stat*X handler. NOTHING is the only case with no per-
 * status work; lagStop suppresses the entire switch. The replay /
 * input / timer bookkeeping at the head and tail also runs on every
 * call.
 */
class GameEngineUpdateDispatchTest {

	@Test
	void updateRunsOnFirstAndOnLastWrappersOnEveryCall() {
		RecordingMode mode = new RecordingMode();
		RecordingReceiver receiver = new RecordingReceiver();
		GameEngine engine = freshEngine(mode, receiver);
		engine.stat = GameEngine.Status.NOTHING;

		engine.update();

		assertTrue(mode.onFirstCalled);
		assertTrue(receiver.onFirstCalled);
		assertTrue(mode.onLastCalled);
		assertTrue(receiver.onLastCalled);
	}

	@Test
	void updateNothingStatusRunsNoStatHandler() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.NOTHING;

		engine.update();

		assertFalse(mode.onSettingCalled);
		assertFalse(mode.onReadyCalled);
		assertFalse(mode.onMoveCalled);
		assertFalse(mode.onLockFlashCalled);
		assertFalse(mode.onLineClearCalled);
		assertFalse(mode.onAREcalled);
		assertFalse(mode.onEndingStartCalled);
		assertFalse(mode.onCustomCalled);
		assertFalse(mode.onExcellentCalled);
		assertFalse(mode.onGameOverCalled);
		assertFalse(mode.onResultCalled);
		assertFalse(mode.onFieldEditCalled);
	}

	@Test
	void updateLagStopFreezesTheStatSwitchEntirely() {
		// lagStop=true makes update skip the per-status switch.
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.READY;
		engine.lagStop = true;

		engine.update();

		assertFalse(mode.onReadyCalled,
				"lagStop suppresses the per-status hook");
		assertTrue(mode.onFirstCalled,
				"onFirst still runs even with lagStop");
		assertTrue(mode.onLastCalled,
				"onLast still runs even with lagStop");
	}

	@Test
	void updateSettingStatusRoutesThroughOnSetting() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.SETTING;

		engine.update();

		assertTrue(mode.onSettingCalled);
	}

	@Test
	void updateReadyStatusRoutesThroughOnReady() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.READY;

		engine.update();

		assertTrue(mode.onReadyCalled);
	}

	@Test
	void updateLockFlashStatusRoutesThroughOnLockFlash() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.ruleopt.lockflash = 100;
		engine.statc[0] = 0;
		engine.stat = GameEngine.Status.LOCKFLASH;

		engine.update();

		assertTrue(mode.onLockFlashCalled);
	}

	@Test
	void updateAreStatusRoutesThroughOnARE() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.statc[0] = 0;
		engine.statc[1] = 100;
		engine.stat = GameEngine.Status.ARE;

		engine.update();

		assertTrue(mode.onAREcalled);
	}

	@Test
	void updateCustomStatusRoutesThroughOnCustom() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.CUSTOM;

		engine.update();

		assertTrue(mode.onCustomCalled);
	}

	@Test
	void updateGameOverStatusRoutesThroughOnGameOver() {
		// onGameOver returns true so the engine bails before the
		// dead-end body that touches the field.
		RecordingMode mode = new RecordingMode();
		mode.onGameOverResult = true;
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.GAMEOVER;

		engine.update();

		assertTrue(mode.onGameOverCalled);
	}

	@Test
	void updateResultStatusRoutesThroughOnResult() {
		// onResult returning true skips the engine's deactivation
		// block.
		RecordingMode mode = new RecordingMode();
		mode.onResultResult = true;
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.RESULT;

		engine.update();

		assertTrue(mode.onResultCalled);
	}

	@Test
	void updateAdvancesReplayTimerWhenGameIsActive() {
		// gameActive=true with non-replay mode: replayData.setInputData
		// fires and replayTimer increments.
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.gameActive = true;
		engine.replayTimer = 0;
		engine.stat = GameEngine.Status.NOTHING;

		engine.update();

		assertEquals(1, engine.replayTimer,
				"gameActive frame must tick replayTimer");
	}

	@Test
	void updateAdvancesStatisticsTimeWhenGameAndTimerActive() {
		// gameActive=true and timerActive=true -> statistics.time++.
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.time = 100;
		engine.stat = GameEngine.Status.NOTHING;

		engine.update();

		assertEquals(101, engine.statistics.time);
	}

	@Test
	void updateDoesNotAdvanceStatisticsTimeWhenGameInactive() {
		// gameActive=false -> statistics.time unchanged.
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.gameActive = false;
		engine.timerActive = true;
		engine.statistics.time = 50;
		engine.stat = GameEngine.Status.NOTHING;

		engine.update();

		assertEquals(50, engine.statistics.time);
	}

	@Test
	void updateDoesNotAdvanceStatisticsTimeWhenTimerInactive() {
		// gameActive=true, timerActive=false -> statistics.time
		// unchanged. Pin the AND gate at the tail.
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.gameActive = true;
		engine.timerActive = false;
		engine.statistics.time = 50;
		engine.stat = GameEngine.Status.NOTHING;

		engine.update();

		assertEquals(50, engine.statistics.time);
	}

	private static GameEngine freshEngine(AbstractMode mode, EventReceiver receiver) {
		GameManager gm = new GameManager(receiver);
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	/** AbstractMode subclass that records every onX invocation. */
	private static final class RecordingMode extends AbstractMode {
		boolean onFirstCalled, onLastCalled;
		boolean onSettingCalled, onReadyCalled, onMoveCalled;
		boolean onLockFlashCalled, onLineClearCalled, onAREcalled;
		boolean onEndingStartCalled, onCustomCalled, onExcellentCalled;
		boolean onGameOverCalled, onResultCalled, onFieldEditCalled;
		boolean onGameOverResult, onResultResult;

		@Override public String getName() { return "stub"; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}

		@Override public void onFirst(GameEngine e, int p) { onFirstCalled = true; }
		@Override public void onLast(GameEngine e, int p) { onLastCalled = true; }
		@Override public boolean onSetting(GameEngine e, int p) { onSettingCalled = true; return true; }
		@Override public boolean onReady(GameEngine e, int p) { onReadyCalled = true; return true; }
		@Override public boolean onMove(GameEngine e, int p) { onMoveCalled = true; return true; }
		@Override public boolean onLockFlash(GameEngine e, int p) { onLockFlashCalled = true; return true; }
		@Override public boolean onLineClear(GameEngine e, int p) { onLineClearCalled = true; return true; }
		@Override public boolean onARE(GameEngine e, int p) { onAREcalled = true; return true; }
		@Override public boolean onEndingStart(GameEngine e, int p) { onEndingStartCalled = true; return true; }
		@Override public boolean onCustom(GameEngine e, int p) { onCustomCalled = true; return true; }
		@Override public boolean onExcellent(GameEngine e, int p) { onExcellentCalled = true; return true; }
		@Override public boolean onGameOver(GameEngine e, int p) { onGameOverCalled = true; return onGameOverResult; }
		@Override public boolean onResult(GameEngine e, int p) { onResultCalled = true; return onResultResult; }
		@Override public boolean onFieldEdit(GameEngine e, int p) { onFieldEditCalled = true; return true; }
	}

	/** Recording stub for the receiver side of the update dispatch. */
	private static final class RecordingReceiver extends EventReceiver {
		boolean onFirstCalled;
		boolean onLastCalled;

		@Override
		public void onFirst(GameEngine engine, int playerID) {
			onFirstCalled = true;
		}

		@Override
		public void onLast(GameEngine engine, int playerID) {
			onLastCalled = true;
		}
	}
}
