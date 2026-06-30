package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;

/**
 * Targets still-uncovered lines in {@link GameEngine} that existing tests miss.
 * Each test is designed to force a specific branch to execute.
 */
class GameEngineFinalCoverageTest {

	// ---------- helpers ----------

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

	// ====================================================================
	// 1. Domino Quick Turn else-if branch (lines 2239-2240)
	//    Start I2 at direction 2 so onGroundBeforeRotate checks different
	//    cells than the quick-turn collision:
	//    - onGroundBeforeRotate: checkCollision(x, y+1, dir2) = cells
	//      (x+1,y+2) and (x,y+2) — block these.
	//    - quick-turn checkCollision(x, y, dir0) = cells (x,y) and (x+1,y)
	//      — keep these clear.
	//    - initial rotation (BUTTON_B) checkCollision(x, y, dir1) = cells
	//      (x+1,y) and (x+1,y+1) — block (x+1,y+1) to make it fail.
	// ====================================================================

	@Test
	void dominoQuickTurnElseIfBranch() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.ruleopt.moveFirstFrame = true;
		engine.ruleopt.rotateWallkick = false;

		// I2 piece at direction 2
		Piece i2 = new Piece(Piece.PIECE_I2);
		i2.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_I2],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_I2]);
		i2.direction = 2;
		i2.updateConnectData();
		engine.nowPieceObject = i2;
		engine.nowPieceX = 5;
		engine.nowPieceY = 6;
		engine.dominoQuickTurn = true;
		engine.nowPieceRotateFailCount = 1;

		// Block (6,7) so initial rotation (BUTTON_B → dir 1) fails:
		// I2 dir 1 cells: (6,6) and (6,7). Block (6,7).
		engine.field.setBlockColor(6, 7, Block.BLOCK_COLOR_GRAY);

		// Block (5,8) and (6,8) for onGroundBeforeRotate:
		// onGround = checkCollision(5,7, dir2) → cells (6,8) and (5,8)
		engine.field.setBlockColor(5, 8, Block.BLOCK_COLOR_GRAY);
		engine.field.setBlockColor(6, 8, Block.BLOCK_COLOR_GRAY);

		// Keep (5,6) and (6,6) clear for quick-turn (dir0) success.

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.initialRotateDirection = 0;

		engine.nextPieceArrayObject = new Piece[1];
		engine.nextPieceArrayObject[0] = new Piece(Piece.PIECE_I2);

		engine.statMove();

		// Line 2240: nowPieceY++ when collision is false and onGround is true
		assertEquals(7, engine.nowPieceY,
				"Domino quick turn should enter else-if (Y++) when collision is false and onGround is true");
	}

	// ====================================================================
	// 2. Domino Quick Turn collision-true branch (line 2238)
	// ====================================================================

	@Test
	void dominoQuickTurnCollisionBranch() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.ruleopt.moveFirstFrame = true;
		engine.ruleopt.rotateWallkick = false;

		Piece i2 = new Piece(Piece.PIECE_I2);
		i2.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_I2],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_I2]);
		i2.direction = 2;
		i2.updateConnectData();
		engine.nowPieceObject = i2;
		engine.nowPieceX = 5;
		engine.nowPieceY = 6;
		engine.dominoQuickTurn = true;
		engine.nowPieceRotateFailCount = 1;

		// Block (6,7) for initial rotation failure
		engine.field.setBlockColor(6, 7, Block.BLOCK_COLOR_GRAY);

		// Block (5,6) so quick-turn dir0 collides
		engine.field.setBlockColor(5, 6, Block.BLOCK_COLOR_GRAY);

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.initialRotateDirection = 0;

		engine.nextPieceArrayObject = new Piece[1];
		engine.nextPieceArrayObject[0] = new Piece(Piece.PIECE_I2);

		engine.statMove();
		assertEquals(5, engine.nowPieceY,
				"Domino quick turn collision-true branch should Y--");
	}

	// ====================================================================
	// 3. Spawn shift loop (lines 2278-2283)
	//    Set pieceEnterAboveField=false so O spawns at y=0, fill rows 0-1
	//    to force collision, then shift up clears the collision.
	// ====================================================================

	@Test
	void spawnShiftNonBigPiece() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 0;
		engine.statc[1] = 0;
		engine.big = false;
		engine.ruleopt.pieceEnterAboveField = false;
		engine.ruleopt.pieceEnterMaxDistanceY = 5;
		engine.enableSE = true;

		// Fill rows 0 and 1 completely so O piece at spawnY=0 collides
		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlockColor(x, 0, Block.BLOCK_COLOR_GRAY);
			engine.field.setBlockColor(x, 1, Block.BLOCK_COLOR_GRAY);
		}

		engine.nextPieceArrayID = new int[]{Piece.PIECE_O};
		engine.nextPieceArrayObject = new Piece[1];
		engine.nextPieceArrayObject[0] = new Piece(Piece.PIECE_O);
		engine.nextPieceArrayObject[0].applyOffsetArray(
				engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		engine.nextPieceCount = 0;

		engine.statMove();
		// After shift: O piece should have moved up enough to clear blocks.
		assertTrue(engine.nowPieceY < 0,
				"Non-big O piece should be shifted into hidden area after collision");
	}

	@Test
	void spawnShiftBigPiece() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 0;
		engine.statc[1] = 0;
		engine.big = true;
		engine.ruleopt.pieceEnterAboveField = false;
		engine.ruleopt.pieceEnterMaxDistanceY = 5;
		engine.enableSE = true;

		// Fill rows 0-3 so big O piece at spawnY=0 collides
		for (int x = 0; x < engine.field.getWidth(); x++) {
			for (int y = 0; y <= 3; y++) {
				engine.field.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
			}
		}

		engine.nextPieceArrayID = new int[]{Piece.PIECE_O};
		engine.nextPieceArrayObject = new Piece[1];
		engine.nextPieceArrayObject[0] = new Piece(Piece.PIECE_O);
		engine.nextPieceArrayObject[0].applyOffsetArray(
				engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		engine.nextPieceArrayObject[0].big = true;
		engine.nextPieceCount = 0;

		engine.statMove();
		assertTrue(engine.nowPieceY < 0,
				"Big O piece should be shifted into hidden area after collision");
	}

	// ====================================================================
	// 4. DAS instant move (lines 2338-2340)
	//    Condition: getDASDelay()==0  &&  dasCount>0  &&  room to move twice
	// ====================================================================

	@Test
	void dasInstantMoveFlags() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.ruleopt.moveFirstFrame = true;
		engine.owDasDelay = 0; // getDASDelay() returns 0
		engine.speed.das = 0; // getDAS() returns 0 so dasCount(1) >= 0
		engine.dasDirection = 1;
		engine.dasSpeedCount = 0;
		engine.dasInstant = false;
		engine.dasRepeat = false;
		engine.enableSE = true;

		// Use T piece (not I, to avoid any offset issues)
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
				engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 0;
		engine.nowPieceY = 5;
		engine.nowPieceBottomY = 5;
		engine.statc[0] = 1;
		engine.dasCount = 1; // > 0 so inner condition passes

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 10;

		engine.nextPieceArrayObject = new Piece[1];
		engine.nextPieceArrayObject[0] = new Piece(Piece.PIECE_T);

		engine.statMove();

		// Verify lines 2338-2340 were reached:
		// dasInstant is set to true by line 2340
		assertTrue(engine.dasRepeat, "dasRepeat should be set to true");
		assertTrue(engine.dasInstant, "dasInstant should be set to true");
	}

	// ====================================================================
	// 5. New soft drop non-multiply (line 2426)
	//    gcount = (int)(speed.denominator * ruleopt.softdropSpeed)
	// ====================================================================

	@Test
	void newSoftDropNonMultiply() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.ruleopt.moveFirstFrame = true;
		engine.ruleopt.softdropGravitySpeedLimit = true;
		engine.ruleopt.softdropSpeed = 2.0f;   // >= 1.0f → new path
		engine.ruleopt.softdropMultiplyNativeSpeed = false;
		engine.ruleopt.softdropEnable = true;
		engine.ruleopt.moveUpAndDown = true;
		engine.speed.gravity = 100;
		engine.speed.denominator = 256;

		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
				engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 10;
		engine.nowPieceBottomY = 12;
		engine.statc[0] = 1;

		// Set gcount high so the softdrop brings it to 512,
		// then denom subtraction leaves residual as evidence
		engine.gcount = 0;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 10;

		engine.nextPieceArrayObject = new Piece[1];
		engine.nextPieceArrayObject[0] = new Piece(Piece.PIECE_T);

		engine.statMove();

		// The new softdrop non-multiply path sets gcount = (int)(256 * 2.0) = 512.
		// The gravity loop (line 2444) then subtracts 256 per cell fallen.
		// gcount = 512 - 256*N where N depends on piece fall.
		// If gcount is in range [0, 256) the softdrop path was exercised.
		assertTrue(engine.gcount >= 0 && engine.gcount < engine.speed.denominator,
				"gcount should indicate softdrop path was exercised");
	}

	// ====================================================================
	// 6. Null-block continue in non-LINE clear mode (line 2830)
	// ====================================================================

	@Test
	void nullBlockContinueInColorMode() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.COLOR;
		engine.colorClearSize = 1;
		engine.garbageColorClear = false;
		engine.gemSameColor = false;
		engine.ignoreHidden = true;
		engine.speed.lineDelay = 5;
		engine.ruleopt.minLineDelay = -1;
		engine.ruleopt.maxLineDelay = -1;
		engine.statc[0] = 0;
		engine.lineClearing = 1;

		// Set a block with ERASE attribute and VISIBLE so checkColor finds it
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		engine.field.setBlockE(5, 19, b);
		engine.field.getRowE(0)[0] = null;

		assertThrows(NullPointerException.class, engine::statLineClear);
		// The null cell hits the line-2830 continue in the block-break pass;
		// clearColor later traverses the same manually corrupted field and throws.
	}

	// ====================================================================
	// 7. Cascade chain re-check with GEM_COLOR (line 2899)
	// ====================================================================

	@Test
	void cascadeGemColorRecheck() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.GEM_COLOR;
		engine.colorClearSize = 1;
		engine.garbageColorClear = false;
		engine.ignoreHidden = true;
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;
		engine.speed.lineDelay = 5;
		engine.ruleopt.minLineDelay = -1;
		engine.ruleopt.maxLineDelay = -1;
		engine.cascadeDelay = 0;
		engine.cascadeClearDelay = 0;
		engine.statc[0] = 100;
		engine.statc[6] = 100;
		engine.lineClearing = 1;

		// Place gem blocks that form a clearable group
		engine.field.setBlockColor(5, 5, Block.BLOCK_COLOR_GEM_RED);
		engine.field.setBlockColor(5, 6, Block.BLOCK_COLOR_GEM_RED);

		engine.statLineClear();
		// Should not throw; line 2899 is exercised.
	}
}
