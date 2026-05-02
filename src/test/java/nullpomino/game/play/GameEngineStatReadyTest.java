package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine#statReady}'s observable contract: the mode
 * override short-circuit, the first-frame initialisation that
 * allocates the field and next-piece queue and flips
 * gameActive/gameStarted/isInGame, the SE triggers at readyStart and
 * goStart, and the goEnd transition that sets stat = MOVE and marks
 * readyDone.
 */
class GameEngineStatReadyTest {

	@Test
	void modeOverrideShortCircuitsBeforeFirstFrameSetup() {
		BlockingMode mode = new BlockingMode();
		mode.onReadyResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 0;
		engine.stat = GameEngine.Status.READY;

		engine.statReady();

		assertTrue(mode.onReadyCalled);
		assertEquals(0, engine.statc[0],
				"mode override returning true must skip statc[0]++");
		assertFalse(engine.gameActive,
				"mode override must skip the gameActive flip");
		assertFalse(engine.gameStarted);
	}

	@Test
	void firstFrameAllocatesFieldAndNextPieceQueueAndFlipsInGameFlags() {
		// statc[0]=0, readyDone=false, no field, no next-piece arrays:
		// the first frame fills all of these in.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.field = null;
		engine.nextPieceArrayID = null;
		engine.nextPieceArrayObject = null;
		engine.statc[0] = 0;
		engine.readyDone = false;
		engine.stat = GameEngine.Status.READY;

		engine.statReady();

		assertNotNull(engine.field, "createFieldIfNeeded ran");
		assertNotNull(engine.nextPieceArrayID, "next-piece IDs allocated");
		assertNotNull(engine.nextPieceArrayObject, "next-piece objects allocated");
		assertTrue(engine.gameActive,
				"gameActive flips on the first frame when readyDone=false");
		assertTrue(engine.gameStarted);
		assertTrue(engine.isInGame);
		assertEquals(1, engine.statc[0],
				"non-transition frame advances statc[0] by 1");
	}

	@Test
	void firstFrameWithReadyDoneSkipsFlipsButStillAllocatesField() {
		// readyDone=true means the player has already passed Ready/Go
		// once (e.g. revival from game-over) — gameActive flip is
		// suppressed, but the field/queue allocations still run because
		// they're gated on null state, not readyDone.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.field = null;
		engine.statc[0] = 0;
		engine.readyDone = true;
		engine.gameActive = false;
		engine.stat = GameEngine.Status.READY;

		engine.statReady();

		assertNotNull(engine.field,
				"field allocation runs unconditionally on the first frame");
		assertFalse(engine.gameActive,
				"readyDone=true keeps gameActive at its prior value");
	}

	@Test
	void normalFrameJustAdvancesStatc() {
		// statc[0] != 0 and not at any milestone: just statc[0]++.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.statc[0] = 5;
		engine.readyStart = 0;
		engine.readyEnd = 49;
		engine.goStart = 50;
		engine.goEnd = 100;
		engine.stat = GameEngine.Status.READY;

		engine.statReady();

		assertEquals(6, engine.statc[0]);
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	@Test
	void hittingGoEndTransitionsToMoveAndResetsStatc() {
		// statc[0] == goEnd: transition to MOVE, statc reset, readyDone
		// set.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.statc[0] = 100;
		engine.readyStart = 0;
		engine.readyEnd = 49;
		engine.goStart = 50;
		engine.goEnd = 100;
		engine.readyDone = false;
		engine.stat = GameEngine.Status.READY;

		engine.statReady();

		assertEquals(GameEngine.Status.MOVE, engine.stat,
				"goEnd reached -> stat = MOVE");
		assertEquals(0, engine.statc[0], "resetStatc cleared the counter");
		assertTrue(engine.readyDone,
				"readyDone flips to true so subsequent retries don't reset start time");
	}

	@Test
	void modeOverrideFalseFallsThroughAndAdvancesStatc() {
		BlockingMode mode = new BlockingMode();
		mode.onReadyResult = false;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.statc[0] = 5;
		engine.goEnd = 100;
		engine.stat = GameEngine.Status.READY;

		engine.statReady();

		assertTrue(mode.onReadyCalled);
		assertEquals(6, engine.statc[0],
				"mode returning false lets the engine advance statc");
	}

	private static final class BlockingMode extends AbstractMode {
		boolean onReadyCalled;
		boolean onReadyResult;

		@Override
		public String getName() { return "stub"; }

		@Override
		public void modeInit(GameManager manager) {}

		@Override
		public void playerInit(GameEngine engine, int playerID) {}

		@Override
		public void renderInput(GameEngine engine, int playerID) {}

		@Override
		public boolean onReady(GameEngine engine, int playerID) {
			onReadyCalled = true;
			return onReadyResult;
		}
	}
}
