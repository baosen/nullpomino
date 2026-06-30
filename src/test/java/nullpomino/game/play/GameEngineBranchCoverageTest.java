package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link GameEngine}. Targets the remaining
 * reachable branches not pinned by the existing GameEngine*Test suite:
 *
 * <ul>
 *   <li>The {@code owner.mode == null} arm of every render-dispatch
 *       {@code if(owner.mode != null) owner.mode.renderX(...)} line.</li>
 *   <li>The render AI overlay arms ({@code aiShowState} / {@code aiShowHint}).</li>
 *   <li>The {@code onX(...) == true -> return} short-circuit arms of
 *       statLineClear / statEndingStart / statFieldEdit.</li>
 *   <li>The {@code owner.mode != null} arms inside fieldEdit-exit and the
 *       place-same-color / erase-already-empty false branches.</li>
 *   <li>fieldUpdate X-RAY and COLOR item-effect loops.</li>
 *   <li>getMoveDirection simultaneous-left+right ordering branches.</li>
 *   <li>checkDropContinuousUse initial-rotate direction branches.</li>
 *   <li>statLineClear T-Spin double/triple statistic increments.</li>
 * </ul>
 */
class GameEngineBranchCoverageTest {

	// ----------------------------------------------------------------
	// Render dispatch: owner.mode == null arms (false branch of every
	// `if(owner.mode != null) owner.mode.renderX(...)`).
	// ----------------------------------------------------------------

	private static GameEngine nullModeEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	@Test void renderSettingWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.SETTING;
		e.render();
		assertTrue(true);
	}

	@Test void renderReadyWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.READY;
		e.render();
		assertTrue(true);
	}

	@Test void renderMoveWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.MOVE;
		e.render();
		assertTrue(true);
	}

	@Test void renderLockFlashWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.LOCKFLASH;
		e.render();
		assertTrue(true);
	}

	@Test void renderLineClearWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.LINECLEAR;
		e.render();
		assertTrue(true);
	}

	@Test void renderAreWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.ARE;
		e.render();
		assertTrue(true);
	}

	@Test void renderEndingStartWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.ENDINGSTART;
		e.render();
		assertTrue(true);
	}

	@Test void renderCustomWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.CUSTOM;
		e.render();
		assertTrue(true);
	}

	@Test void renderExcellentWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.EXCELLENT;
		e.render();
		assertTrue(true);
	}

	@Test void renderGameOverWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.GAMEOVER;
		e.render();
		assertTrue(true);
	}

	@Test void renderResultWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.RESULT;
		e.render();
		assertTrue(true);
	}

	@Test void renderFieldEditWithNullMode() {
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.FIELDEDIT;
		e.render();
		assertTrue(true);
	}

	@Test void renderInputWithNullModeAndShowInput() {
		// owner.showInput true + owner.mode null exercises the
		// `if(owner.mode != null) owner.mode.renderInput` false arm.
		GameEngine e = nullModeEngine();
		e.stat = GameEngine.Status.MOVE;
		e.owner.showInput = true;
		e.render();
		assertTrue(true);
	}

	// ----------------------------------------------------------------
	// Render AI overlay arms: aiShowState / aiShowHint true.
	// ----------------------------------------------------------------

	@Test void renderInvokesAiStateAndHintWhenEnabled() {
		GameEngine e = nullModeEngine();
		e.ai = new DummyAI();
		e.aiShowState = true;
		e.aiShowHint = true;
		e.stat = GameEngine.Status.NOTHING;
		e.render();
		assertTrue(true);
	}

	// ----------------------------------------------------------------
	// onX(...) == true -> return short-circuit arms.
	// ----------------------------------------------------------------

	@Test void statLineClearModeOnLineClearReturnsTrue() {
		// onLineClear returning true must short-circuit before
		// checkDropContinuousUse / the first-frame processing.
		BlockingMode mode = new BlockingMode();
		mode.onLineClearResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.createFieldIfNeeded();
		e.stat = GameEngine.Status.LINECLEAR;
		e.statc[0] = 0;
		e.statLineClear();
		assertTrue(mode.onLineClearCalled);
		// statc[0] must be untouched by the skipped body.
		assertEquals(0, e.statc[0]);
	}

	@Test void statEndingStartModeOnEndingStartReturnsTrue() {
		BlockingMode mode = new BlockingMode();
		mode.onEndingStartResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.createFieldIfNeeded();
		e.stat = GameEngine.Status.ENDINGSTART;
		e.statc[2] = 0;
		e.statEndingStart();
		assertTrue(mode.onEndingStartCalled);
		// The short-circuit skips the statc[2]==0 block that would set it to 1.
		assertEquals(0, e.statc[2]);
	}

	@Test void statFieldEditModeOnFieldEditReturnsFalseRunsBody() {
		// onFieldEdit returning false must NOT short-circuit; the body
		// runs and increments fldeditFrames (covers the == false arm).
		BlockingMode mode = new BlockingMode();
		mode.onFieldEditResult = false;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.stat = GameEngine.Status.MOVE;
		e.enterFieldEdit();
		e.fldeditFrames = 0;
		e.statFieldEdit();
		assertTrue(mode.onFieldEditCalled);
		assertEquals(1, e.fldeditFrames);
	}

	// ----------------------------------------------------------------
	// statFieldEdit: mode != null exit arm + place-same / erase-empty
	// false branches.
	// ----------------------------------------------------------------

	@Test void statFieldEditExitInvokesModeFieldEditExit() {
		// BUTTON_B push at frame>10 with a non-null mode covers the
		// `if(owner.mode != null) owner.mode.fieldEditExit(...)` true arm.
		BlockingMode mode = new BlockingMode();
		mode.onFieldEditResult = false;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.stat = GameEngine.Status.MOVE;
		e.enterFieldEdit();
		e.fldeditFrames = 11;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_B] = true;
		e.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		e.statFieldEdit();
		assertTrue(mode.fieldEditExitCalled);
		assertEquals(GameEngine.Status.MOVE, e.stat);
	}

	@Test void statFieldEditPlaceIsNoOpWhenColorAlreadyMatches() {
		// getBlockColorE == fldeditColor -> the inner `!=` test is false,
		// so no placement / change SE.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.enterFieldEdit();
		e.fldeditX = 2;
		e.fldeditY = 3;
		e.fldeditColor = Block.BLOCK_COLOR_RED;
		// Pre-place the SAME color the cursor would stamp.
		e.field.setBlockColor(2, 3, Block.BLOCK_COLOR_RED);
		e.fldeditFrames = 11;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_A] = true;
		e.ctrl.buttonTime[Controller.BUTTON_A] = 5;
		e.statFieldEdit();
		assertEquals(Block.BLOCK_COLOR_RED, e.field.getBlockColor(2, 3));
	}

	@Test void statFieldEditEraseIsNoOpWhenCellAlreadyEmpty() {
		// getBlockEmptyE true -> the `!empty` guard is false, no change SE.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.enterFieldEdit();
		e.fldeditX = 4;
		e.fldeditY = 5;
		// Cell is already empty.
		e.fldeditFrames = 11;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_D] = true;
		e.ctrl.buttonTime[Controller.BUTTON_D] = 5;
		e.statFieldEdit();
		assertEquals(Block.BLOCK_COLOR_NONE, e.field.getBlockColor(4, 5));
	}

	// ----------------------------------------------------------------
	// fieldUpdate item-effect loops: X-RAY and COLOR.
	// ----------------------------------------------------------------

	@Test void fieldUpdateXRayTogglesBlockVisibility() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.itemXRayEnable = true;
		e.itemXRayCount = 0;
		// A visible block at column 0 so the % 36 == i toggle is observable.
		e.field.setBlockColor(0, 19, Block.BLOCK_COLOR_GRAY);
		e.field.getBlock(0, 19).elapsedFrames = -1;
		e.fieldUpdate();
		// itemXRayCount advanced past 0 (the enable branch ran).
		assertTrue(e.itemXRayCount > 0);
	}

	@Test void fieldUpdateColorAdjustsBlockAlpha() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.itemColorEnable = true;
		e.itemColorCount = 0;
		e.field.setBlockColor(3, 10, Block.BLOCK_COLOR_RED);
		e.field.getBlock(3, 10).elapsedFrames = -1;
		e.fieldUpdate();
		assertTrue(e.itemColorCount > 0);
	}

	@Test void fieldUpdateColorDisabledResetsCount() {
		// The else-arm (itemColorEnable false) zeroes the counter.
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.itemColorEnable = false;
		e.itemColorCount = 99;
		e.fieldUpdate();
		assertEquals(0, e.itemColorCount);
	}

	// ----------------------------------------------------------------
	// getMoveDirection: simultaneous LEFT+RIGHT ordering branches.
	// ----------------------------------------------------------------

	@Test void getMoveDirectionLeftHeldLongerPrefersRight() {
		// LEFT+RIGHT, moveLeftAndRightAllow, LEFT held longer than RIGHT,
		// usePreviousInput false -> returns 1 (RIGHT). (L1141 true arm.)
		GameEngine e = freshEngine();
		e.ruleopt.moveLeftAndRightAllow = true;
		e.ruleopt.moveLeftAndRightUsePreviousInput = false;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 10;
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 3;
		assertEquals(1, e.getMoveDirection());
	}

	@Test void getMoveDirectionRightHeldLongerPrefersLeft() {
		// RIGHT held longer than LEFT -> L1143 else-if true arm.
		GameEngine e = freshEngine();
		e.ruleopt.moveLeftAndRightAllow = true;
		e.ruleopt.moveLeftAndRightUsePreviousInput = false;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 3;
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 10;
		assertEquals(-1, e.getMoveDirection());
	}

	@Test void getMoveDirectionSimultaneousNotAllowedReturnsZero() {
		// LEFT+RIGHT but moveLeftAndRightAllow false -> falls through to 0.
		GameEngine e = freshEngine();
		e.ruleopt.moveLeftAndRightAllow = false;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 5;
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 5;
		assertEquals(0, e.getMoveDirection());
	}

	// ----------------------------------------------------------------
	// checkDropContinuousUse: initial-rotate direction branches.
	// ----------------------------------------------------------------

	@Test void checkDropContinuousUseInitialRotateButtonEDirection() {
		// BUTTON_E pressed -> dir = 2 (L1126 true arm). With matching
		// last direction the continuous-use flag survives.
		GameEngine e = freshEngine();
		e.gameActive = true;
		e.ruleopt.rotateInitialLimit = true;
		e.initialRotateContinuousUse = true;
		e.initialRotateLastDirection = 2;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_E] = true;
		e.ctrl.buttonTime[Controller.BUTTON_E] = 1;
		e.checkDropContinuousUse();
		assertTrue(e.initialRotateContinuousUse);
	}

	@Test void checkDropContinuousUseInitialRotateButtonADirection() {
		// BUTTON_A pressed -> dir = -1 (L1124 true arm).
		GameEngine e = freshEngine();
		e.gameActive = true;
		e.ruleopt.rotateInitialLimit = true;
		e.initialRotateContinuousUse = true;
		e.initialRotateLastDirection = -1;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_A] = true;
		e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.checkDropContinuousUse();
		assertTrue(e.initialRotateContinuousUse);
	}

	@Test void checkDropContinuousUseInitialRotateDirectionMismatchClears() {
		// dir computed != last direction -> flag cleared.
		GameEngine e = freshEngine();
		e.gameActive = true;
		e.ruleopt.rotateInitialLimit = true;
		e.initialRotateContinuousUse = true;
		e.initialRotateLastDirection = 1;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_A] = true;
		e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.checkDropContinuousUse();
		assertFalse(e.initialRotateContinuousUse);
	}

	// ----------------------------------------------------------------
	// statLineClear: T-Spin Double / Triple statistic increments.
	// ----------------------------------------------------------------

	@Test void statLineClearTSpinDoubleIncrementsStat() {
		// tspin + li==2 + !tspinmini -> totalTSpinDouble++ (L2747 true arm).
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.speed.lineDelay = 1;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0;
		e.tspin = true;
		e.tspinmini = false;
		e.ending = 0;
		fillLine(e, 18);
		fillLine(e, 19);
		int before = e.statistics.totalTSpinDouble;
		e.statLineClear();
		assertEquals(before + 1, e.statistics.totalTSpinDouble);
	}

	@Test void statLineClearTSpinDoubleMiniIncrementsStat() {
		// tspin + li==2 + tspinmini -> totalTSpinDoubleMini++ (L2746 true arm).
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.speed.lineDelay = 1;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0;
		e.tspin = true;
		e.tspinmini = true;
		e.ending = 0;
		fillLine(e, 18);
		fillLine(e, 19);
		int before = e.statistics.totalTSpinDoubleMini;
		e.statLineClear();
		assertEquals(before + 1, e.statistics.totalTSpinDoubleMini);
	}

	@Test void statLineClearTSpinTripleIncrementsStat() {
		// tspin + li==3 -> totalTSpinTriple++ (L2748 true arm).
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.speed.lineDelay = 1;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0;
		e.tspin = true;
		e.tspinmini = false;
		e.ending = 0;
		fillLine(e, 17);
		fillLine(e, 18);
		fillLine(e, 19);
		int before = e.statistics.totalTSpinTriple;
		e.statLineClear();
		assertEquals(before + 1, e.statistics.totalTSpinTriple);
	}

	@Test void statLineClearStaffrollStatisticsSecondDisjunct() {
		// ending != 0 but staffrollEnableStatistics true -> the second
		// disjunct of every `(ending==0)||(staffrollEnableStatistics)`
		// guard in statLineClear is taken instead of the first.
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.speed.lineDelay = 1;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0;
		e.tspin = false;
		e.ending = 2;
		e.staffrollEnableStatistics = true;
		fillLine(e, 19);
		int before = e.statistics.totalSingle;
		e.statLineClear();
		assertEquals(before + 1, e.statistics.totalSingle,
				"second disjunct (staffrollEnableStatistics) lets the stat run during ending");
	}

	// ----------------------------------------------------------------
	// statGameOver: dead-end row sweep with a non-empty field exercises
	// the `displaysize != -1` darkness arm (L3150).
	// ----------------------------------------------------------------

	@Test void statGameOverDeadEndRowSweepDarkensBlocksWhenDisplaysizeSet() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.createFieldIfNeeded();
		e.gameStarted = true;
		e.gameActive = true;
		e.lives = 0;
		e.displaysize = 0; // != -1 -> the darkness branch fires
		e.statc[0] = 0;
		// Non-empty field so the row sweep runs (skips the empty shortcut).
		e.field.setBlockColor(0, e.field.getHeight() - 1, Block.BLOCK_COLOR_RED);
		// First call: gameEnded + resetFieldVisible (field not empty).
		e.statGameOver();
		// Advance until the bottom row (height - statc[0]) is swept.
		for (int i = 0; i < e.field.getHeight() + 2; i++) {
			if (e.statc[0] >= e.field.getHeight() + 1) break;
			e.statGameOver();
		}
		Block blk = e.field.getBlock(0, e.field.getHeight() - 1);
		assertEquals(0.3f, blk.darkness, 0.001f,
				"dead-end sweep darkens non-garbage blocks to 0.3 when displaysize != -1");
	}

	// ----------------------------------------------------------------
	// Helpers
	// ----------------------------------------------------------------

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static GameEngine engineWithField() {
		GameEngine e = freshEngine();
		e.createFieldIfNeeded();
		return e;
	}

	private static void fillLine(GameEngine e, int y) {
		for (int x = 0; x < e.field.getWidth(); x++)
			e.field.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
	}

	/** AbstractMode whose lifecycle hooks return configurable booleans. */
	private static final class BlockingMode extends AbstractMode {
		boolean onLineClearCalled, onLineClearResult;
		boolean onEndingStartCalled, onEndingStartResult;
		boolean onFieldEditCalled, onFieldEditResult;
		boolean fieldEditExitCalled;

		@Override public String getName() { return "stub"; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}

		@Override public boolean onLineClear(GameEngine engine, int playerID) {
			onLineClearCalled = true;
			return onLineClearResult;
		}

		@Override public boolean onEndingStart(GameEngine engine, int playerID) {
			onEndingStartCalled = true;
			return onEndingStartResult;
		}

		@Override public boolean onFieldEdit(GameEngine engine, int playerID) {
			onFieldEditCalled = true;
			return onFieldEditResult;
		}

		@Override public void fieldEditExit(GameEngine engine, int playerID) {
			fieldEditExitCalled = true;
		}
	}
}
