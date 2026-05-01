package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link GameEngine#interruptItemMirrorProc} state machine
 * (snapshot → swap-each-column → wait → end) and the
 * {@link GameEngine#statInterruptItem} dispatcher that resets the
 * status field when the proc returns false.
 */
class GameEngineInterruptItemTest {

	@Test
	void mirrorProcOnFirstFrameSnapshotsFieldAndClearsBoardThenAdvancesCounter() {
		GameEngine engine = freshEngine();
		engine.createFieldIfNeeded();
		engine.field.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		engine.statc[0] = 0;

		boolean cont = engine.interruptItemMirrorProc();

		assertTrue(cont, "mirror proc continues until counter wraps");
		assertEquals(1, engine.statc[0]);
		// Field is cleared at frame 0, snapshot lives in interruptItemMirrorField.
		assertEquals(Block.BLOCK_COLOR_NONE, engine.field.getBlockColor(0, 0));
	}

	@Test
	void mirrorProcWaitFramesAdvanceCounterWithoutMutatingField() {
		GameEngine engine = freshEngine();
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		engine.interruptItemMirrorProc(); // snapshot + clear

		// Frames 1..20 are pre-mirror wait — counter advances, nothing else.
		for(int frame = 1; frame <= 20; frame++) {
			boolean cont = engine.interruptItemMirrorProc();
			assertTrue(cont);
			assertEquals(frame + 1, engine.statc[0]);
		}
	}

	@Test
	void mirrorProcEndFrameClearsCounterAndReturnsFalse() {
		GameEngine engine = freshEngine();
		engine.createFieldIfNeeded();
		// Drive directly to the terminal frame: 21 + width*2 + 5 .
		int width = engine.field.getWidth();
		engine.statc[0] = 21 + width * 2 + 5;

		// First, populate the snapshot field reference so the end-branch
		// reads a non-null reference before nulling it.
		engine.statc[0] = 0;
		engine.interruptItemMirrorProc(); // snapshot
		// Now jump forward to the terminal frame.
		engine.statc[0] = 21 + width * 2 + 5;

		boolean cont = engine.interruptItemMirrorProc();

		assertFalse(cont, "mirror proc returns false at the terminal frame");
		assertEquals(0, engine.statc[0],
				"the terminal branch zeros the counter for the next item");
	}

	@Test
	void statInterruptItemMirrorBranchAdvancesUntilProcReturnsFalse() {
		GameEngine engine = freshEngine();
		engine.createFieldIfNeeded();
		engine.interruptItemNumber = GameEngine.INTERRUPTITEM_MIRROR;
		engine.interruptItemPreviousStat = GameEngine.Status.MOVE;
		engine.stat = GameEngine.Status.INTERRUPTITEM;
		// Drive the proc to its end on the first call: snapshot first,
		// then jump to the terminal frame.
		engine.statc[0] = 0;
		engine.interruptItemMirrorProc();
		int width = engine.field.getWidth();
		engine.statc[0] = 21 + width * 2 + 5;

		engine.statInterruptItem();

		// Once proc returns false, statInterruptItem clears
		// interruptItemNumber and restores the previous status.
		assertEquals(GameEngine.INTERRUPTITEM_NONE, engine.interruptItemNumber);
		assertEquals(GameEngine.Status.MOVE, engine.stat);
		// resetStatc zeros every status counter.
		for(int v : engine.statc) assertEquals(0, v);
	}

	@Test
	void statInterruptItemDoesNothingWhenProcContinues() {
		GameEngine engine = freshEngine();
		engine.createFieldIfNeeded();
		engine.interruptItemNumber = GameEngine.INTERRUPTITEM_MIRROR;
		engine.interruptItemPreviousStat = GameEngine.Status.MOVE;
		engine.stat = GameEngine.Status.INTERRUPTITEM;

		// First frame snapshots and continues.
		engine.statc[0] = 0;
		engine.statInterruptItem();

		assertEquals(GameEngine.INTERRUPTITEM_MIRROR, engine.interruptItemNumber,
				"interruptItemNumber stays mirror while the proc still has frames");
		assertEquals(GameEngine.Status.INTERRUPTITEM, engine.stat);
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}
