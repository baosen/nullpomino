package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine#statEndingStart}'s observable contract: the
 * mode override short-circuit, the first-frame setup that turns off
 * the timer and BGM, the line-delay wait, the row-by-row board
 * eraser, the second line-delay window, and the final transition to
 * MOVE (when staffrollEnable) or EXCELLENT.
 */
class GameEngineStatEndingStartTest {

	@Test
	void modeOverrideShortCircuitsBeforeFirstFrameSetup() {
		BlockingMode mode = new BlockingMode();
		mode.onEndingStartResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.timerActive = true;
		engine.owner.bgmStatus.bgm = BGMStatus.BGM_NORMAL1;
		engine.statc[2] = 0;
		engine.stat = GameEngine.Status.ENDINGSTART;

		engine.statEndingStart();

		assertTrue(mode.onEndingStartCalled);
		assertTrue(engine.timerActive,
				"mode override must skip the timer-off + BGM-off block");
		assertEquals(BGMStatus.BGM_NORMAL1, engine.owner.bgmStatus.bgm);
		assertEquals(0, engine.statc[2]);
	}

	@Test
	void firstFrameTurnsOffTimerAndBgmAndAdvancesStatcTwo() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.timerActive = true;
		engine.owner.bgmStatus.bgm = BGMStatus.BGM_NORMAL1;
		engine.statc[2] = 0;
		// statc[0] starts at 0 < getLineDelay (default), so the line-delay
		// branch fires next, advancing statc[0] by 1.
		engine.stat = GameEngine.Status.ENDINGSTART;

		engine.statEndingStart();

		assertFalse(engine.timerActive,
				"first frame turns the timer off");
		assertEquals(BGMStatus.BGM_NOTHING, engine.owner.bgmStatus.bgm,
				"first frame clears BGM so the ending-start SE plays alone");
		assertEquals(1, engine.statc[2],
				"statc[2] flips to 1 so the first-frame block runs once");
		assertEquals(1, engine.statc[0],
				"first call also advances the line-delay counter by 1");
	}

	@Test
	void lineDelayWindowAdvancesStatcZeroUntilTheBoardEraserKicksIn() {
		// statc[0] starts below getLineDelay; each call advances statc[0]
		// by 1 until it reaches getLineDelay, at which point the
		// row-by-row eraser starts.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.statc[2] = 1; // skip first-frame block
		engine.statc[0] = 0;
		engine.statc[1] = 0;
		engine.speed.lineDelay = 3;

		engine.statEndingStart();
		assertEquals(1, engine.statc[0]);

		engine.statEndingStart();
		assertEquals(2, engine.statc[0]);

		engine.statEndingStart();
		// statc[0] now equals getLineDelay (3). The next call switches
		// to the eraser branch instead of advancing statc[0] further.
		assertEquals(3, engine.statc[0]);

		engine.statEndingStart();
		assertEquals(3, engine.statc[0],
				"once statc[0] hits getLineDelay, the eraser branch takes over");
		// statc[1] advances each frame in the eraser branch (one
		// iteration per call).
		assertEquals(1, engine.statc[1]);
	}

	@Test
	void boardEraserClearsTopRowOnEverySixthFrameAndStampsLineFlag() {
		// The eraser activates when statc[0] >= getLineDelay and
		// statc[1] < height*6, painting one row every 6 ticks.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		// Plant a colored block on the top row of the visible field.
		int height = engine.field.getHeight();
		engine.field.setBlockColor(0, height - 1, Block.BLOCK_COLOR_RED);
		engine.statc[2] = 1;
		engine.statc[0] = 0;
		engine.statc[1] = 0;
		engine.speed.lineDelay = 0; // skip the line-delay wait entirely

		// First eraser frame: statc[1]==0 -> y = height - (0/6) = height,
		// out of bounds — but the loop body still runs the line-flag set
		// and block-break on y=height (a no-op except for the flag).
		// statc[1] advances regardless.
		engine.statEndingStart();
		assertEquals(1, engine.statc[1],
				"eraser frame advances statc[1] each call");

		// Walk the eraser forward 5 more frames to land statc[1]=6, the
		// next 'paint a row' iteration.
		for(int i = 0; i < 5; i++) engine.statEndingStart();
		assertEquals(6, engine.statc[1]);

		// One more call: y = height - (6/6) = height - 1, the row we
		// planted. The block clears.
		engine.statEndingStart();
		assertEquals(Block.BLOCK_COLOR_NONE,
				engine.field.getBlockColor(0, height - 1),
				"y = height - statc[1]/6 -> the planted block is now empty");
		assertTrue(engine.field.getLineFlag(height - 1),
				"row gets the line-flag stamped before the block-break");
	}

	@Test
	void postEraserSecondLineDelayWindowAdvancesStatcZeroOnceMore() {
		// Past the eraser (statc[1] >= height*6), statc[0] gets a
		// second pump up to getLineDelay + 2.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.statc[2] = 1;
		// Force the eraser-done state.
		engine.statc[1] = engine.field.getHeight() * 6;
		engine.statc[0] = 0;
		engine.speed.lineDelay = 1;

		engine.statEndingStart();
		assertEquals(1, engine.statc[0]);

		engine.statEndingStart();
		assertEquals(2, engine.statc[0]);
	}

	@Test
	void completionTransitionsToExcellentWhenStaffRollIsDisabled() {
		// All counters past the second line-delay -> ending=2, field
		// reset, statc reset, stat = EXCELLENT.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		// Plant a block so we can confirm field.reset() ran.
		engine.field.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		engine.statc[2] = 1;
		engine.statc[1] = engine.field.getHeight() * 6;
		engine.statc[0] = 5; // already past getLineDelay + 2 with default
		engine.speed.lineDelay = 1; // 1 + 2 = 3, statc[0]=5 is past that
		engine.staffrollEnable = false;
		engine.stat = GameEngine.Status.ENDINGSTART;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statEndingStart();

		assertEquals(2, engine.ending);
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
		assertEquals(0, engine.statc[0], "resetStatc cleared the counter");
		assertEquals(Block.BLOCK_COLOR_NONE,
				engine.field.getBlockColor(0, 0),
				"field.reset() cleared the planted block");
	}

	@Test
	void completionTransitionsToMoveWithStaffRollEnabled() {
		// staffrollEnable=true: ending=2, nowPieceObject cleared, stat
		// becomes MOVE so the staff roll plays before EXCELLENT.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.statc[2] = 1;
		engine.statc[1] = engine.field.getHeight() * 6;
		engine.statc[0] = 5;
		engine.speed.lineDelay = 1;
		engine.staffrollEnable = true;
		engine.stat = GameEngine.Status.ENDINGSTART;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statEndingStart();

		assertEquals(2, engine.ending);
		assertEquals(GameEngine.Status.MOVE, engine.stat);
		assertNull(engine.nowPieceObject,
				"staff roll path clears nowPieceObject so MOVE re-spawns");
	}

	private static final class BlockingMode extends AbstractMode {
		boolean onEndingStartCalled;
		boolean onEndingStartResult;

		@Override
		public String getName() { return "stub"; }

		@Override
		public void modeInit(GameManager manager) {}

		@Override
		public void playerInit(GameEngine engine, int playerID) {}

		@Override
		public void renderInput(GameEngine engine, int playerID) {}

		@Override
		public boolean onEndingStart(GameEngine engine, int playerID) {
			onEndingStartCalled = true;
			return onEndingStartResult;
		}
	}
}
