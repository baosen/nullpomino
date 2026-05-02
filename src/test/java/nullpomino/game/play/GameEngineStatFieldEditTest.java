package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine#statFieldEdit}'s observable contract: the
 * mode override short-circuit, the cursor wrap behaviour
 * (Left/Right/Up/Down without C wraps in field bounds), the color-
 * picker wrap (Left/Right with C wraps in [GRAY, GEM_PURPLE]), the
 * A/D place/erase actions gated on fldeditFrames > 10, and the B-push
 * exit that returns to fldeditPreviousStat.
 */
class GameEngineStatFieldEditTest {

	@Test
	void modeOverrideShortCircuitsBeforeCursorAndPlacementBranches() {
		BlockingMode mode = new BlockingMode();
		mode.onFieldEditResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.fldeditX = 3;
		engine.fldeditY = 4;
		engine.fldeditFrames = 0;

		engine.statFieldEdit();

		assertTrue(mode.onFieldEditCalled);
		assertEquals(0, engine.fldeditFrames,
				"mode override returning true must skip fldeditFrames++");
		assertEquals(3, engine.fldeditX);
		assertEquals(4, engine.fldeditY);
	}

	@Test
	void normalFrameAdvancesFldeditFramesByOne() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.enterFieldEdit();
		engine.fldeditFrames = 0;

		engine.statFieldEdit();

		assertEquals(1, engine.fldeditFrames);
	}

	@Test
	void leftCursorMoveWrapsAroundAtZero() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.enterFieldEdit();
		engine.fldeditX = 0;

		// LEFT pressed (without C), fresh press (buttonTime == 1).
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		engine.statFieldEdit();

		assertEquals(engine.fieldWidth - 1, engine.fldeditX,
				"LEFT at x=0 wraps to fieldWidth-1");
	}

	@Test
	void rightCursorMoveWrapsAroundAtMax() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.enterFieldEdit();
		engine.fldeditX = engine.fieldWidth - 1;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		engine.statFieldEdit();

		assertEquals(0, engine.fldeditX, "RIGHT at fieldWidth-1 wraps to 0");
	}

	@Test
	void colorChangeWithCButtonWrapsAtPurpleAndAtGray() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.enterFieldEdit();

		// LEFT + hold C -> color decrements; at GRAY wraps to GEM_PURPLE.
		engine.fldeditColor = Block.BLOCK_COLOR_GRAY;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_C] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_C] = 5;
		engine.statFieldEdit();
		assertEquals(Block.BLOCK_COLOR_GEM_PURPLE, engine.fldeditColor,
				"LEFT+C at GRAY wraps to GEM_PURPLE");

		// RIGHT + hold C at GEM_PURPLE -> wraps back to GRAY.
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_C] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_C] = 5;
		engine.statFieldEdit();
		assertEquals(Block.BLOCK_COLOR_GRAY, engine.fldeditColor,
				"RIGHT+C at GEM_PURPLE wraps to GRAY");
	}

	@Test
	void buttonAPlacesBlockAtCursorOnceFrameGateOpens() {
		// fldeditFrames must be > 10 for the placement branch to fire.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.enterFieldEdit();
		engine.fldeditX = 2;
		engine.fldeditY = 3;
		engine.fldeditColor = Block.BLOCK_COLOR_RED;
		engine.fldeditFrames = 11;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 5;
		engine.statFieldEdit();

		assertEquals(Block.BLOCK_COLOR_RED,
				engine.field.getBlockColor(2, 3),
				"BUTTON_A at frame > 10 stamps fldeditColor at the cursor");
	}

	@Test
	void buttonAIsNoOpBeforeFrameGateOpens() {
		// fldeditFrames <= 10 -> no placement.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.enterFieldEdit();
		engine.fldeditX = 2;
		engine.fldeditY = 3;
		engine.fldeditColor = Block.BLOCK_COLOR_RED;
		engine.fldeditFrames = 5;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.statFieldEdit();

		assertEquals(Block.BLOCK_COLOR_NONE,
				engine.field.getBlockColor(2, 3),
				"BUTTON_A at frame <= 10 must not place anything");
	}

	@Test
	void buttonDErasesPlacedBlockOnceFrameGateOpens() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.enterFieldEdit();
		engine.fldeditX = 4;
		engine.fldeditY = 5;
		engine.field.setBlockColor(4, 5, Block.BLOCK_COLOR_BLUE);
		engine.fldeditFrames = 11;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 5;
		engine.statFieldEdit();

		assertEquals(Block.BLOCK_COLOR_NONE,
				engine.field.getBlockColor(4, 5),
				"BUTTON_D at frame > 10 erases the cell at the cursor");
	}

	@Test
	void buttonBPushExitsBackToFldeditPreviousStat() {
		// fldeditFrames > 10 + BUTTON_B push -> stat = fldeditPreviousStat.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.stat = GameEngine.Status.MOVE;
		engine.enterFieldEdit();
		// fldeditPreviousStat is captured by enterFieldEdit; assert it.
		assertEquals(GameEngine.Status.MOVE, engine.fldeditPreviousStat);
		engine.fldeditFrames = 11;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.statFieldEdit();

		assertEquals(GameEngine.Status.MOVE, engine.stat,
				"BUTTON_B push at frame > 10 returns to fldeditPreviousStat");
	}

	@Test
	void buttonBIsNoOpBeforeFrameGateOpens() {
		// Before fldeditFrames > 10, BUTTON_B is suppressed so the user
		// can't accidentally exit the editor on the same frame they
		// entered.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.stat = GameEngine.Status.MOVE;
		engine.enterFieldEdit();
		engine.fldeditFrames = 5;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.statFieldEdit();

		assertEquals(GameEngine.Status.FIELDEDIT, engine.stat,
				"BUTTON_B push before the frame gate must not exit FIELDEDIT");
	}

	private static final class BlockingMode extends AbstractMode {
		boolean onFieldEditCalled;
		boolean onFieldEditResult;

		@Override
		public String getName() { return "stub"; }

		@Override
		public void modeInit(GameManager manager) {}

		@Override
		public void playerInit(GameEngine engine, int playerID) {}

		@Override
		public void renderInput(GameEngine engine, int playerID) {}

		@Override
		public boolean onFieldEdit(GameEngine engine, int playerID) {
			onFieldEditCalled = true;
			return onFieldEditResult;
		}
	}
}
