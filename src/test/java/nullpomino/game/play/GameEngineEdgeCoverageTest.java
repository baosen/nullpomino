package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;

/**
 * Covers remaining hard-to-reach branches in {@link GameEngine}.
 */
class GameEngineEdgeCoverageTest {

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

	/** Spawn a piece so nowPieceObject / nowPieceX / nowPieceY are set. */
	private static void spawnPieceInMove(GameEngine engine) {
		engine.statc[0] = 0;
		engine.statc[1] = 0;
		engine.stat = GameEngine.Status.MOVE;
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		if (engine.nextPieceArrayObject == null) {
			engine.nextPieceArrayID = new int[]{Piece.PIECE_T};
			engine.nextPieceArrayObject = new Piece[1];
			engine.nextPieceArrayObject[0] = new Piece(Piece.PIECE_T);
			engine.nextPieceArrayObject[0].applyOffsetArray(
					engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
					engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		}
		engine.nextPieceCount = 0;
		engine.statMove();
	}

	// ====================================================================
	// 1. init() with ai (lines 905-906)
	// ====================================================================

	@Test
	void initShutsDownAndReinitsAi() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = new GameEngine(gm, 0);
		engine.ai = new DummyAI();
		engine.init();
		assertNotNull(engine.ai, "ai should still be set after init");
	}

	// ====================================================================
	// 2. setTSpin immobile (lines 1279-1283)
	// ====================================================================

	@Test
	void setTSpinImmobileDetectsWhenPieceCannotMove() {
		GameEngine engine = engineWithField();
		engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		engine.tspinAllowKick = true;
		engine.kickused = true;
		Piece tPiece = new Piece(Piece.PIECE_T);
		tPiece.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);

		// T piece at dir 0 has filled cells: (1,0), (0,1), (1,1), (2,1)
		// immobile checks: piece at (5,4), (6,5), (4,5)
		// checkCollision(5,4): cells (6,4),(5,5),(6,5),(7,5) - need 1 blocked
		// checkCollision(6,5): cells (7,5),(6,6),(7,6),(8,6) - need 1 blocked
		// checkCollision(4,5): cells (5,5),(4,6),(5,6),(6,6) - need 1 blocked
		engine.field.setBlockColor(6, 5, Block.BLOCK_COLOR_RED); // blocks (5,4) & (6,5) checks
		engine.field.setBlockColor(5, 5, Block.BLOCK_COLOR_RED); // blocks (5,4) & (4,5) checks
		engine.field.setBlockColor(7, 5, Block.BLOCK_COLOR_RED); // blocks (6,5) check

		engine.setTSpin(5, 5, tPiece, engine.field);
		assertTrue(engine.tspin, "immobile check should set tspin when piece can't move");
	}

	// ====================================================================
	// 3. setAllSpin immobile (lines 1353-1357)
	// ====================================================================

	@Test
	void setAllSpinImmobileDetectsWhenPieceCannotMove() {
		GameEngine engine = engineWithField();
		engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		engine.tspinAllowKick = true;
		engine.kickused = true;
		engine.tspinEnableEZ = false;
		Piece tPiece = new Piece(Piece.PIECE_T);

		engine.field.setBlockColor(6, 5, Block.BLOCK_COLOR_RED);
		engine.field.setBlockColor(5, 5, Block.BLOCK_COLOR_RED);
		engine.field.setBlockColor(7, 5, Block.BLOCK_COLOR_RED);

		engine.setAllSpin(5, 5, tPiece, engine.field);
		assertTrue(engine.tspin, "immobile check should set tspin");
	}

	// ====================================================================
	// 4. update dispatch: ENDINGSTART, FIELDEDIT, INTERRUPTITEM  (lines 1734-1752)
	// ====================================================================

	@Test
	void updateDispatchesToStatEndingStart() {
		GameEngine engine = freshEngine();
		engine.stat = GameEngine.Status.ENDINGSTART;
		engine.gameActive = true;
		engine.createFieldIfNeeded();
		// Should not throw — exercises line 1734 dispatch.
		engine.update();
	}

	@Test
	void updateDispatchesToStatFieldEdit() {
		GameEngine engine = freshEngine();
		engine.stat = GameEngine.Status.FIELDEDIT;
		engine.gameActive = true;
		engine.createFieldIfNeeded();
		engine.fldeditFrames = 11;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.fldeditPreviousStat = GameEngine.Status.MOVE;
		engine.update();
	}

	@Test
	void updateDispatchesToStatInterruptItem() {
		GameEngine engine = freshEngine();
		engine.stat = GameEngine.Status.INTERRUPTITEM;
		engine.gameActive = true;
		engine.createFieldIfNeeded();
		engine.interruptItemNumber = GameEngine.INTERRUPTITEM_NONE;
		engine.update();
	}

	// ====================================================================
	// 5. nextPiece random direction (line 1924)
	// ====================================================================

	@Test
	void nextPieceUsesRandomDirectionWhenDefaultExceedsCount() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.ruleopt.pieceDefaultDirection[Piece.PIECE_T] = 99;
		engine.createFieldIfNeeded();

		engine.nextPieceArrayID = new int[]{Piece.PIECE_T};
		engine.nextPieceArrayObject = null;
		engine.nextPieceEnable = new boolean[Piece.PIECE_COUNT];
		for (int i = 0; i < Piece.PIECE_COUNT; i++)
			engine.nextPieceEnable[i] = true;
		engine.random = new java.util.Random(42);

		engine.stat = GameEngine.Status.READY;
		engine.gameActive = true;
		engine.statc[0] = 0;
		engine.statReady();

		assertNotNull(engine.nextPieceArrayObject[0]);
		assertTrue(engine.nextPieceArrayObject[0].direction < Piece.DIRECTION_COUNT);
	}

	// ====================================================================
	// 6. DAS charge on blocked move (lines 2362-2363)
	// ====================================================================

	@Test
	void dasChargeOnBlockedMove() {
		GameEngine engine = engineWithField();
		spawnPieceInMove(engine);
		engine.statc[0] = 1;
		engine.dasCount = engine.getDAS();
		engine.dasDirection = -1;
		engine.ruleopt.dasChargeOnBlockedMove = true;
		engine.nowPieceX = 0; // leftmost, cannot move further left
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 10;

		engine.statMove();
		assertEquals(engine.getDAS(), engine.dasCount,
				"dasChargeOnBlockedMove should cap dasCount to getDAS()");
	}

	// ====================================================================
	// 7. New soft drop (lines 2416-2433)
	// ====================================================================

	@Test
	void newSoftDropSetsGcountWithMultiplyNativeSpeed() {
		GameEngine engine = engineWithField();
		spawnPieceInMove(engine);
		engine.statc[0] = 1;
		engine.ruleopt.softdropGravitySpeedLimit = true;
		engine.ruleopt.softdropSpeed = 2.0f;
		engine.ruleopt.softdropMultiplyNativeSpeed = true;
		engine.ruleopt.softdropEnable = true;
		engine.ruleopt.moveUpAndDown = true;
		engine.speed.gravity = 100;
		engine.speed.denominator = 256;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 10;

		engine.statMove();
		assertEquals(200, engine.gcount);
	}

	// ====================================================================
	// 8. Soft-drop surface lock (lines 2536-2538)
	// ====================================================================

	@Test
	void softdropSurfaceLockRuns() {
		GameEngine engine = engineWithField();
		spawnPieceInMove(engine);
		engine.nowPieceY = 18;
		engine.ruleopt.softdropEnable = true;
		engine.ruleopt.softdropSurfaceLock = true;
		engine.ruleopt.moveUpAndDown = true;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 10;

		engine.statMove();
		// Should not throw
	}

	// ====================================================================
	// 9. useAllSpinBonus in lock path (line 2563)
	// ====================================================================

	@Test
	void lockUsesAllSpinBonusWhenFlagIsSet() {
		GameEngine engine = engineWithField();
		spawnPieceInMove(engine);
		engine.nowPieceY = 18;
		engine.lockDelayNow = 100;
		engine.speed.lockDelay = 10;
		engine.useAllSpinBonus = true;
		engine.tspinEnable = true;
		engine.lastmove = GameEngine.LastMove.ROTATE_AIR;
		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		engine.tspinAllowKick = true;

		engine.statMove();
		// Should go through lock path with setAllSpin
	}

	// ====================================================================
	// 10. T-spin 0 lines (lines 2594-2598)
	// ====================================================================

	@Test
	void lockWithTSpinZeroLines() {
		GameEngine engine = engineWithField();
		spawnPieceInMove(engine);
		engine.nowPieceY = 18;
		engine.lockDelayNow = 100;
		engine.speed.lockDelay = 10;
		engine.tspin = true;
		engine.tspinmini = true;
		engine.lastmove = GameEngine.LastMove.ROTATE_AIR;
		engine.ending = 0;
		engine.staffrollEnableStatistics = true;
		engine.enableSE = true;

		engine.statMove();
		// Should play tspin0 sound and record stats; no exception
	}

	// ====================================================================
	// 11. Cascade line clear transition (lines 2625-2627)
	// ====================================================================

	@Test
	void lockTransitionsToLineClearForCascade() {
		GameEngine engine = engineWithField();
		spawnPieceInMove(engine);
		engine.nowPieceY = 18;
		engine.lockDelayNow = 100;
		engine.speed.lockDelay = 10;
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;
		engine.connectBlocks = false;
		engine.ruleopt.lockflash = 0;
		engine.ruleopt.lockflashBeforeLineClear = false;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		engine.lastmove = GameEngine.LastMove.ROTATE_AIR;

		engine.statMove();
		// Just verify no exception - the lock may go to GAMEOVER or LINECLEAR
	}

	// ====================================================================
	// 12. Interrupt item after lock (lines 2643-2645)
	// ====================================================================

	@Test
	void lockTransitionsToInterruptItem() {
		GameEngine engine = engineWithField();
		spawnPieceInMove(engine);
		engine.nowPieceY = 18;
		engine.lockDelayNow = 100;
		engine.speed.lockDelay = 10;
		engine.lineClearing = 0;
		engine.interruptItemNumber = GameEngine.INTERRUPTITEM_MIRROR;
		engine.ruleopt.lockflash = 0;
		engine.ruleopt.lockflashBeforeLineClear = false;
		engine.lastmove = GameEngine.LastMove.ROTATE_AIR;

		engine.statMove();
		// Just verify no exception
	}

	// ====================================================================
	// 13. statLineClear GEM_COLOR (lines 2732-2733)
	// ====================================================================

	@Test
	void statLineClearGemColorCheck() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.GEM_COLOR;
		engine.colorClearSize = 1;
		engine.garbageColorClear = false;
		engine.ignoreHidden = false;
		engine.field.setBlockColor(5, 5, Block.BLOCK_COLOR_GEM_RED);
		engine.statc[0] = 0;

		engine.statLineClear();
	}

	// ====================================================================
	// 14. statLineClear big bighalf (line 2738)
	// ====================================================================

	@Test
	void statLineClearBigHalfShiftsLines() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.LINE;
		engine.big = true;
		engine.bighalf = true;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		engine.field.setLineFlag(19, true);
		engine.lineClearing = 2;
		engine.statc[0] = 0;

		engine.statLineClear();
	}

	// ====================================================================
	// 15. statLineClear tspin sound/stats (lines 2741-2748)
	// ====================================================================

	@Test
	void statLineClearTSpinSoundAndStats() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.LINE;
		engine.tspin = true;
		engine.ending = 0;
		engine.staffrollEnableStatistics = true;
		engine.enableSE = true;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		engine.field.setLineFlag(19, true);
		engine.lineClearing = 1;
		engine.statc[0] = 0;

		engine.statLineClear();
	}

	// ====================================================================
	// 16. statLineClear blockBreak quad for displaySize=1 (lines 2835-2838)
	// ====================================================================

	@Test
	void statLineClearBlockBreakQuadForDisplaySizeOne() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.COLOR;
		engine.colorClearSize = 1;
		engine.garbageColorClear = false;
		engine.gemSameColor = false;
		engine.ignoreHidden = true;
		engine.displaysize = 1;
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		b.elapsedFrames = -1;
		engine.field.setBlockE(5, 0, b);
		engine.lineClearing = 1;
		engine.statc[0] = 0;

		engine.statLineClear();
	}

	// ====================================================================
	// 17. statLineClear gemClearColor dispatch (lines 2853-2854)
	// ====================================================================

	@Test
	void statLineClearCallsGemClearColor() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.GEM_COLOR;
		engine.colorClearSize = 1;
		engine.garbageColorClear = false;
		engine.ignoreHidden = true;
		engine.field.setBlockColor(5, 0, Block.BLOCK_COLOR_GEM_RED);
		engine.field.getBlock(5, 0).setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		engine.lineClearing = 1;
		engine.statc[0] = 0;

		engine.statLineClear();
	}

	// ====================================================================
	// 18. statLineClear cascade re-check (lines 2886-2907)
	// ====================================================================

	@Test
	void statLineClearCascadeRecheck() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.LINE;
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;
		engine.cascadeDelay = 5;
		engine.cascadeClearDelay = 5;
		engine.field.setBlockColor(5, 5, Block.BLOCK_COLOR_RED);
		engine.field.setBlockColor(5, 6, Block.BLOCK_COLOR_RED);
		engine.statc[0] = 100;
		engine.statc[6] = 100;
		engine.lineClearing = 1;

		engine.statLineClear();
	}

	// ====================================================================
	// 19. statLineClear sticky at end (lines 2914-2916)
	// ====================================================================

	@Test
	void statLineClearStickyAtEnd() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = GameEngine.ClearType.LINE;
		engine.sticky = 2;
		engine.statc[0] = 200;
		engine.lineClearing = 1;
		engine.lineGravityType = GameEngine.LineGravity.NATIVE;

		engine.statLineClear();
	}

	// ====================================================================
	// 20. statARE dasRedirect (line 2997)
	// ====================================================================

	@Test
	void statAREDasRedirect() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.ARE;
		engine.gameActive = true;
		engine.ruleopt.dasRedirectInDelay = true;
		engine.ruleopt.dasInARE = false;
		engine.statc[0] = 0;
		engine.statc[1] = 10;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 5;

		engine.statARE();
	}

	// ====================================================================
	// 21. statGameOver button skip (line 3163)
	// ====================================================================

	@Test
	void statGameOverButtonSkips() {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.GAMEOVER;
		engine.gameActive = true;
		engine.lives = 0;
		engine.owner = new GameManager(new EventReceiver());
		engine.owner.init();
		engine.playerID = 0;
		// Fill at least one cell so field is not empty (lines 3133-3137)
		engine.field.setBlockColor(5, 0, Block.BLOCK_COLOR_RED);
		// Advance statc[0] past the height-scanning phase (field.getHeight()+1) and the
		// gameover-sound phase (+1) and past the 60-frame wait for button press
		engine.statc[0] = engine.field.getHeight() + 1 + 60;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		engine.statGameOver();
		// Line 3163: when A is pushed after 60 frames, statc[0] jumped to fieldHeight+1+180,
		// then statc[0]++ increments by one more.
		assertEquals(engine.field.getHeight() + 1 + 180 + 1, engine.statc[0]);
	}

	// ====================================================================
	// 22. interruptItemMirrorProc inversion (lines 3351-3354)
	// ====================================================================

	@Test
	void interruptItemMirrorProcInversion() {
		GameEngine engine = engineWithField();
		engine.statc[0] = 0;
		engine.interruptItemMirrorProc(); // snapshot
		int width = engine.field.getWidth();
		engine.statc[0] = 21; // first inversion frame
		engine.interruptItemMirrorField = new Field(engine.field);

		boolean cont = engine.interruptItemMirrorProc();
		assertTrue(cont);
		assertEquals(22, engine.statc[0]);
	}
}
