package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine#render}'s status-driven dispatch table. The
 * method runs renderFirst / renderLast on both the mode and the
 * receiver, then routes through a per-Status switch that calls
 * exactly one render*X pair (mode then receiver), and finally calls
 * renderInput / renderState / renderHint conditionally on the
 * surrounding flags.
 *
 * <p>NOTHING and INTERRUPTITEM are no-op cases (no per-status render).
 * Every other Status routes through its own pair so a regression in
 * the switch surfaces here.
 */
class GameEngineRenderDispatchTest {

	@Test
	void renderRunsRenderFirstAndRenderLastWrappersOnEveryCall() {
		RecordingMode mode = new RecordingMode();
		RecordingReceiver receiver = new RecordingReceiver();
		GameEngine engine = freshEngine(mode, receiver);
		engine.stat = GameEngine.Status.NOTHING;

		engine.render();

		assertTrue(mode.renderFirstCalled);
		assertTrue(receiver.renderFirstCalled);
		assertTrue(mode.renderLastCalled);
		assertTrue(receiver.renderLastCalled, "receiver.renderLast must run after the per-status pair");
		// NOTHING -> no per-status render fired.
		assertFalse(mode.renderMoveCalled);
		assertFalse(mode.renderResultCalled);
	}

	@Test
	void renderDispatchesToRenderSettingForSettingStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.SETTING;

		engine.render();

		assertTrue(mode.renderSettingCalled);
	}

	@Test
	void renderDispatchesToRenderReadyForReadyStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.READY;

		engine.render();

		assertTrue(mode.renderReadyCalled);
	}

	@Test
	void renderDispatchesToRenderMoveForMoveStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.MOVE;

		engine.render();

		assertTrue(mode.renderMoveCalled);
	}

	@Test
	void renderDispatchesToRenderLockFlashForLockFlashStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.LOCKFLASH;

		engine.render();

		assertTrue(mode.renderLockFlashCalled);
	}

	@Test
	void renderDispatchesToRenderLineClearForLineClearStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.LINECLEAR;

		engine.render();

		assertTrue(mode.renderLineClearCalled);
	}

	@Test
	void renderDispatchesToRenderAreForAreStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.ARE;

		engine.render();

		assertTrue(mode.renderAreCalled);
	}

	@Test
	void renderDispatchesToRenderEndingStartForEndingStartStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.ENDINGSTART;

		engine.render();

		assertTrue(mode.renderEndingStartCalled);
	}

	@Test
	void renderDispatchesToRenderCustomForCustomStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.CUSTOM;

		engine.render();

		assertTrue(mode.renderCustomCalled);
	}

	@Test
	void renderDispatchesToRenderExcellentForExcellentStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.EXCELLENT;

		engine.render();

		assertTrue(mode.renderExcellentCalled);
	}

	@Test
	void renderDispatchesToRenderGameOverForGameOverStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.GAMEOVER;

		engine.render();

		assertTrue(mode.renderGameOverCalled);
	}

	@Test
	void renderDispatchesToRenderResultForResultStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.RESULT;

		engine.render();

		assertTrue(mode.renderResultCalled);
	}

	@Test
	void renderDispatchesToRenderFieldEditForFieldEditStatus() {
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.FIELDEDIT;

		engine.render();

		assertTrue(mode.renderFieldEditCalled);
	}

	@Test
	void renderInterruptItemHasNoPerStatusRender() {
		// INTERRUPTITEM is a no-op in the render switch — only
		// renderFirst / renderLast wrappers fire.
		RecordingMode mode = new RecordingMode();
		GameEngine engine = freshEngine(mode, new RecordingReceiver());
		engine.stat = GameEngine.Status.INTERRUPTITEM;

		engine.render();

		assertTrue(mode.renderFirstCalled);
		assertTrue(mode.renderLastCalled);
		assertFalse(mode.renderMoveCalled);
		assertFalse(mode.renderResultCalled);
	}

	@Test
	void renderInputFiresOnlyWhenOwnerShowInputIsTrue() {
		RecordingMode mode = new RecordingMode();
		RecordingReceiver receiver = new RecordingReceiver();
		GameEngine engine = freshEngine(mode, receiver);
		engine.owner.showInput = false;
		engine.stat = GameEngine.Status.MOVE;

		engine.render();

		assertFalse(mode.renderInputCalled,
				"showInput=false suppresses renderInput");

		engine.owner.showInput = true;
		mode.renderInputCalled = false;
		engine.render();
		assertTrue(mode.renderInputCalled,
				"showInput=true routes through renderInput on the mode");
	}

	private static GameEngine freshEngine(AbstractMode mode, EventReceiver receiver) {
		GameManager gm = new GameManager(receiver);
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	/** AbstractMode subclass that records every render*X invocation. */
	private static final class RecordingMode extends AbstractMode {
		boolean renderFirstCalled, renderLastCalled;
		boolean renderSettingCalled, renderReadyCalled, renderMoveCalled;
		boolean renderLockFlashCalled, renderLineClearCalled, renderAreCalled;
		boolean renderEndingStartCalled, renderCustomCalled, renderExcellentCalled;
		boolean renderGameOverCalled, renderResultCalled, renderFieldEditCalled;
		boolean renderInputCalled;

		@Override public String getName() { return "stub"; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}

		@Override public void renderFirst(GameEngine e, int p) { renderFirstCalled = true; }
		@Override public void renderLast(GameEngine e, int p) { renderLastCalled = true; }
		@Override public void renderSetting(GameEngine e, int p) { renderSettingCalled = true; }
		@Override public void renderReady(GameEngine e, int p) { renderReadyCalled = true; }
		@Override public void renderMove(GameEngine e, int p) { renderMoveCalled = true; }
		@Override public void renderLockFlash(GameEngine e, int p) { renderLockFlashCalled = true; }
		@Override public void renderLineClear(GameEngine e, int p) { renderLineClearCalled = true; }
		@Override public void renderARE(GameEngine e, int p) { renderAreCalled = true; }
		@Override public void renderEndingStart(GameEngine e, int p) { renderEndingStartCalled = true; }
		@Override public void renderCustom(GameEngine e, int p) { renderCustomCalled = true; }
		@Override public void renderExcellent(GameEngine e, int p) { renderExcellentCalled = true; }
		@Override public void renderGameOver(GameEngine e, int p) { renderGameOverCalled = true; }
		@Override public void renderResult(GameEngine e, int p) { renderResultCalled = true; }
		@Override public void renderFieldEdit(GameEngine e, int p) { renderFieldEditCalled = true; }
		@Override public void renderInput(GameEngine e, int p) { renderInputCalled = true; }
	}

	/** Recording stub for the receiver side of the render dispatch. */
	private static final class RecordingReceiver extends EventReceiver {
		boolean renderFirstCalled;
		boolean renderLastCalled;

		@Override
		public void renderFirst(GameEngine engine, int playerID) {
			renderFirstCalled = true;
		}

		@Override
		public void renderLast(GameEngine engine, int playerID) {
			renderLastCalled = true;
		}
	}
}
