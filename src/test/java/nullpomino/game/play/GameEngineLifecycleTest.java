package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;

/**
 * Pins the behavioural lifecycle helpers on GameEngine: input direction
 * decoding, move/rotate-count caps, field bootstrap, field-edit entry,
 * and gameEnded. These are called every frame from update() / mode hooks
 * and need to keep their shape across refactors of the input handler
 * and the field-create path.
 */
class GameEngineLifecycleTest {

	private static GameEngine newEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	@Test
	void getMoveDirectionReturnsZeroWhenNeitherSideIsHeld() {
		GameEngine eng = newEngine();
		assertEquals(0, eng.getMoveDirection());
	}

	@Test
	void getMoveDirectionReturnsMinusOneForLeftAndOneForRight() {
		GameEngine eng = newEngine();

		eng.ctrl.buttonTime[Controller.BUTTON_LEFT] = 5;
		assertEquals(-1, eng.getMoveDirection());

		eng.ctrl.buttonTime[Controller.BUTTON_LEFT] = 0;
		eng.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 5;
		assertEquals(1, eng.getMoveDirection());
	}

	@Test
	void getMoveDirectionPicksMostRecentlyPressedSideWhenBothHeld() {
		GameEngine eng = newEngine();
		eng.ruleopt.moveLeftAndRightAllow = true;

		// LEFT held longer (older press), RIGHT just tapped (newer)
		eng.ctrl.buttonTime[Controller.BUTTON_LEFT] = 10;
		eng.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 3;
		eng.ruleopt.moveLeftAndRightUsePreviousInput = false;
		assertEquals(1, eng.getMoveDirection(),
				"with usePreviousInput=false, the more recent press wins");

		eng.ruleopt.moveLeftAndRightUsePreviousInput = true;
		assertEquals(-1, eng.getMoveDirection(),
				"with usePreviousInput=true, the longer-held direction wins");
	}

	@Test
	void padRepeatIncrementsDasCountWhileHoldingDirection() {
		GameEngine eng = newEngine();
		eng.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 5;
		eng.dasCount = 3;

		eng.padRepeat();

		assertEquals(4, eng.dasCount);
		assertEquals(1, eng.dasDirection);
	}

	@Test
	void padRepeatResetsDasCountWhenNeutralAndNoCharge() {
		GameEngine eng = newEngine();
		eng.dasCount = 8;
		eng.ruleopt.dasStoreChargeOnNeutral = false;

		eng.padRepeat();

		assertEquals(0, eng.dasCount,
				"neutral with dasStoreChargeOnNeutral=false drops the charge");
		assertEquals(0, eng.dasDirection);
	}

	@Test
	void padRepeatPreservesDasCountWhenStoreChargeOnNeutralIsOn() {
		GameEngine eng = newEngine();
		eng.dasCount = 8;
		eng.ruleopt.dasStoreChargeOnNeutral = true;

		eng.padRepeat();

		assertEquals(8, eng.dasCount);
	}

	@Test
	void dasRedirectMirrorsCurrentMoveDirectionWithoutTouchingDasCount() {
		GameEngine eng = newEngine();
		eng.dasCount = 11;
		eng.ctrl.buttonTime[Controller.BUTTON_LEFT] = 2;

		eng.dasRedirect();

		assertEquals(-1, eng.dasDirection);
		assertEquals(11, eng.dasCount, "dasRedirect must not touch dasCount");
	}

	@Test
	void isMoveCountExceedRespectsShareCountFlagAndLimit() {
		GameEngine eng = newEngine();
		eng.ruleopt.lockresetLimitMove = 5;
		eng.ruleopt.lockresetLimitShareCount = true;

		eng.extendedMoveCount = 3;
		eng.extendedRotateCount = 1;
		assertFalse(eng.isMoveCountExceed());

		eng.extendedMoveCount = 4;
		eng.extendedRotateCount = 1;
		assertTrue(eng.isMoveCountExceed(),
				"shared-count: move + rotate >= limit triggers");

		eng.ruleopt.lockresetLimitShareCount = false;
		eng.extendedMoveCount = 6;
		assertTrue(eng.isMoveCountExceed());
		eng.extendedMoveCount = 4;
		assertFalse(eng.isMoveCountExceed());
	}

	@Test
	void isRotateCountExceedRespectsShareCountFlagAndDistinctLimit() {
		GameEngine eng = newEngine();
		eng.ruleopt.lockresetLimitShareCount = false;
		eng.ruleopt.lockresetLimitRotate = 4;
		eng.extendedRotateCount = 5;

		assertTrue(eng.isRotateCountExceed());

		eng.extendedRotateCount = 3;
		assertFalse(eng.isRotateCountExceed());
	}

	@Test
	void isMoveCountExceedReturnsFalseWhenLimitIsNegative() {
		GameEngine eng = newEngine();
		eng.ruleopt.lockresetLimitMove = -1;
		eng.extendedMoveCount = 9999;

		assertFalse(eng.isMoveCountExceed(),
				"negative limit means uncapped");
	}

	@Test
	void createFieldIfNeededFillsDefaultsFromRuleOptionsAndAllocatesField() {
		GameEngine eng = newEngine();
		assertEquals(-1, eng.fieldWidth, "init() leaves dimensions sentinel-negative");

		eng.createFieldIfNeeded();

		assertEquals(eng.ruleopt.fieldWidth, eng.fieldWidth);
		assertEquals(eng.ruleopt.fieldHeight, eng.fieldHeight);
		assertEquals(eng.ruleopt.fieldHiddenHeight, eng.fieldHiddenHeight);
		assertNotNull(eng.field);
		assertEquals(eng.fieldWidth, eng.field.getWidth());
		assertEquals(eng.fieldHeight, eng.field.getHeight());
	}

	@Test
	void createFieldIfNeededDoesNotReplaceExistingField() {
		GameEngine eng = newEngine();
		Field original = new Field(8, 16, 2, false);
		eng.field = original;

		eng.createFieldIfNeeded();

		assertEquals(original, eng.field, "existing field must not be replaced");
	}

	@Test
	void enterFieldEditSwitchesStatusAndSeedsEditorState() {
		GameEngine eng = newEngine();
		eng.stat = GameEngine.Status.MOVE;

		eng.enterFieldEdit();

		assertEquals(GameEngine.Status.FIELDEDIT, eng.stat);
		assertEquals(GameEngine.Status.MOVE, eng.fldeditPreviousStat);
		assertEquals(0, eng.fldeditX);
		assertEquals(0, eng.fldeditY);
		assertEquals(Block.BLOCK_COLOR_GRAY, eng.fldeditColor);
		assertEquals(0, eng.fldeditFrames);
		assertFalse(eng.owner.menuOnly);
		assertNotNull(eng.field, "enterFieldEdit must materialise the field");
	}

	@Test
	void gameEndedCapturesEndTimeAndDeactivatesGame() {
		GameEngine eng = newEngine();
		eng.startTime = System.nanoTime();
		eng.replayTimer = 60;
		eng.gameActive = true;
		eng.timerActive = true;
		eng.isInGame = true;

		eng.gameEnded();

		assertTrue(eng.endTime > 0, "endTime must be set when called the first time");
		assertFalse(eng.gameActive);
		assertFalse(eng.timerActive);
		assertFalse(eng.isInGame);
	}

	@Test
	void gameEndedIsIdempotentOnRepeatCalls() {
		GameEngine eng = newEngine();
		eng.startTime = System.nanoTime();
		eng.replayTimer = 60;

		eng.gameEnded();
		long firstEnd = eng.endTime;

		eng.gameEnded();
		assertEquals(firstEnd, eng.endTime,
				"second call must not overwrite endTime");
	}

	@Test
	void getSpawnPosXReturnsCenteredColumnForStandardIPiece() {
		GameEngine eng = newEngine();
		eng.field = new Field(10, 20, 3, false);
		Piece piece = new Piece(Piece.PIECE_I);

		int x = eng.getSpawnPosX(eng.field, piece);

		// 10-wide field, I piece's getWidth() returns 3 (max-min delta) →
		// -1 + (10 - 3 + 1)/2 = 3 (with default piece-spawn offsets at 0)
		assertEquals(3, x);
	}

	@Test
	void getSpawnPosYUsesAboveFieldEntryWhenCeilingIsOff() {
		GameEngine eng = newEngine();
		eng.ruleopt.pieceEnterAboveField = true;
		eng.ruleopt.fieldCeiling = false;
		Piece piece = new Piece(Piece.PIECE_I);

		int y = eng.getSpawnPosY(piece);

		assertEquals(-1 - piece.getMaximumBlockY(), y);
	}

	@Test
	void getSpawnPosYUsesInFieldEntryWhenCeilingForcesIt() {
		GameEngine eng = newEngine();
		eng.ruleopt.pieceEnterAboveField = false;
		eng.ruleopt.fieldCeiling = true;
		Piece piece = new Piece(Piece.PIECE_I);

		int y = eng.getSpawnPosY(piece);

		assertEquals(-piece.getMinimumBlockY(), y);
	}
}
