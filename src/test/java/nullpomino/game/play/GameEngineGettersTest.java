package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.event.EventReceiver;

/**
 * Pins the pure-function getters and resets on GameEngine. Modes call
 * these every frame for ARE/lock-delay budgets, button-mapping, and
 * piece-spawn coords; the override-vs-rule-option fallback ladder needs
 * to keep working as RuleOptions evolves.
 */
class GameEngineGettersTest {

	private static GameEngine newEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	@Test
	void resetStatcZeroesEveryStatusCounter() {
		GameEngine eng = newEngine();
		for (int i = 0; i < eng.statc.length; i++) eng.statc[i] = 42;

		eng.resetStatc();

		for (int i = 0; i < eng.statc.length; i++) assertEquals(0, eng.statc[i]);
	}

	@Test
	void getNextIDReturnsPieceNoneWhenSequenceArrayIsNull() {
		GameEngine eng = newEngine();

		assertNull(eng.nextPieceArrayID);
		assertEquals(Piece.PIECE_NONE, eng.getNextID(0));
		assertEquals(Piece.PIECE_NONE, eng.getNextID(99));
	}

	@Test
	void getNextIDWrapsIndexAcrossSequenceLength() {
		GameEngine eng = newEngine();
		eng.nextPieceArrayID = new int[] {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L};

		assertEquals(Piece.PIECE_I, eng.getNextID(0));
		assertEquals(Piece.PIECE_T, eng.getNextID(1));
		// Index 5 wraps: 5 - 3 = 2 → PIECE_L
		assertEquals(Piece.PIECE_L, eng.getNextID(5));
	}

	@Test
	void getNextObjectReturnsNullWhenObjectArrayIsNull() {
		GameEngine eng = newEngine();

		assertNull(eng.nextPieceArrayObject);
		assertNull(eng.getNextObject(0));
	}

	@Test
	void getNextObjectCopyReturnsFreshCopyOfStoredPiece() {
		GameEngine eng = newEngine();
		Piece original = new Piece(Piece.PIECE_T);
		eng.nextPieceArrayObject = new Piece[] {original};

		Piece copy = eng.getNextObjectCopy(0);

		assertNotNull(copy);
		assertNotSame(original, copy, "must return a defensive copy");
		assertEquals(original.id, copy.id);
	}

	@Test
	void getNextObjectCopyReturnsNullWhenSourceIsNull() {
		GameEngine eng = newEngine();

		assertNull(eng.getNextObjectCopy(0));
	}

	@Test
	void getAreReturnsRawSpeedValueWhenRuleBoundsAreUnconstrained() {
		GameEngine eng = newEngine();

		assertEquals(eng.speed.are, eng.getARE());
		assertEquals(eng.speed.areLine, eng.getARELine());
		assertEquals(eng.speed.lineDelay, eng.getLineDelay());
		assertEquals(eng.speed.lockDelay, eng.getLockDelay());
		assertEquals(eng.speed.das, eng.getDAS());
	}

	@Test
	void getAreClampsToMinWhenSpeedFallsBelowRuleMin() {
		GameEngine eng = newEngine();
		eng.speed.are = 5;
		eng.ruleopt.minARE = 10;

		assertEquals(10, eng.getARE());
	}

	@Test
	void getAreClampsToMaxWhenSpeedExceedsRuleMax() {
		GameEngine eng = newEngine();
		eng.speed.are = 50;
		eng.ruleopt.maxARE = 30;

		assertEquals(30, eng.getARE());
	}

	@Test
	void getLineDelayClampsToConfiguredBounds() {
		GameEngine eng = newEngine();
		eng.speed.lineDelay = 5;
		eng.ruleopt.minLineDelay = 10;
		assertEquals(10, eng.getLineDelay());

		eng.speed.lineDelay = 100;
		eng.ruleopt.maxLineDelay = 60;
		assertEquals(60, eng.getLineDelay());
	}

	@Test
	void getLockDelayClampsToConfiguredBounds() {
		GameEngine eng = newEngine();
		eng.speed.lockDelay = 5;
		eng.ruleopt.minLockDelay = 10;
		assertEquals(10, eng.getLockDelay());
	}

	@Test
	void getDasClampsToOwOverrideMinFirstThenRuleMin() {
		GameEngine eng = newEngine();
		eng.speed.das = 5;
		// owMinDAS=-1 means override disabled → falls through to ruleopt
		eng.owMinDAS = -1;
		eng.ruleopt.minDAS = 8;
		assertEquals(8, eng.getDAS());

		// owMinDAS overrides ruleopt
		eng.owMinDAS = 12;
		assertEquals(12, eng.getDAS());
	}

	@Test
	void getUpAndGetDownRespectOwReverseUpDown() {
		GameEngine eng = newEngine();

		eng.owReverseUpDown = false;
		assertEquals(Controller.BUTTON_UP, eng.getUp());
		assertEquals(Controller.BUTTON_DOWN, eng.getDown());

		eng.owReverseUpDown = true;
		assertEquals(Controller.BUTTON_DOWN, eng.getUp());
		assertEquals(Controller.BUTTON_UP, eng.getDown());
	}

	@Test
	void getDasDelayUsesOwOverrideOverRuleOption() {
		GameEngine eng = newEngine();

		eng.owDasDelay = -1;
		eng.ruleopt.dasDelay = 7;
		assertEquals(7, eng.getDASDelay(),
				"owDasDelay disabled (-1) falls through to ruleopt.dasDelay");

		eng.owDasDelay = 9;
		assertEquals(9, eng.getDASDelay(),
				"owDasDelay enabled overrides ruleopt.dasDelay");
	}

	@Test
	void getSkinUsesOwOverrideOverRuleOption() {
		GameEngine eng = newEngine();

		eng.owSkin = -1;
		eng.ruleopt.skin = 3;
		assertEquals(3, eng.getSkin(),
				"owSkin disabled (-1) falls through to ruleopt.skin");

		eng.owSkin = 5;
		assertEquals(5, eng.getSkin());
	}

	@Test
	void isRotateButtonDefaultRightFollowsOwOverrideThenRuleOption() {
		GameEngine eng = newEngine();

		eng.owRotateButtonDefaultRight = -1;
		eng.ruleopt.rotateButtonDefaultRight = true;
		assertTrue(eng.isRotateButtonDefaultRight());

		eng.owRotateButtonDefaultRight = 0;
		assertFalse(eng.isRotateButtonDefaultRight());
		eng.owRotateButtonDefaultRight = 1;
		assertTrue(eng.isRotateButtonDefaultRight());
	}

	@Test
	void isDiagonalMoveEnabledFollowsOwOverrideThenRuleOption() {
		GameEngine eng = newEngine();

		eng.owMoveDiagonal = -1;
		eng.ruleopt.moveDiagonal = true;
		assertTrue(eng.isDiagonalMoveEnabled());

		eng.owMoveDiagonal = 0;
		assertFalse(eng.isDiagonalMoveEnabled());
		eng.owMoveDiagonal = 1;
		assertTrue(eng.isDiagonalMoveEnabled());
	}

	@Test
	void getRotateDirectionRotatesRelativeToCurrentPieceDirection() {
		GameEngine eng = newEngine();
		eng.nowPieceObject = new Piece(Piece.PIECE_T);
		eng.nowPieceObject.direction = 1;

		// move=1 → direction 2
		assertEquals(2, eng.getRotateDirection(1));
		// move=-1 → direction 0
		assertEquals(0, eng.getRotateDirection(-1));
		// move=2 (180) → direction 3
		assertEquals(3, eng.getRotateDirection(2));
	}

	@Test
	void getRotateDirectionWrapsAroundFromDirectionThree() {
		GameEngine eng = newEngine();
		eng.nowPieceObject = new Piece(Piece.PIECE_T);
		eng.nowPieceObject.direction = 3;

		assertEquals(0, eng.getRotateDirection(1), "right rotation past 3 wraps to 0");
		assertEquals(2, eng.getRotateDirection(-1));
		// 180 from direction 3: 3 + 2 = 5 → 5 - 4 = 1
		assertEquals(1, eng.getRotateDirection(2));
	}

	@Test
	void getRotateDirectionWithoutPieceUsesMoveAsAbsoluteDirection() {
		GameEngine eng = newEngine();
		eng.nowPieceObject = null;

		// Without a piece, rt = 0 + move
		assertEquals(1, eng.getRotateDirection(1));
		// move=-1 → -1 → wrap to 3
		assertEquals(3, eng.getRotateDirection(-1));
	}

	@Test
	void resetFieldVisibleNoOpsWhenFieldIsNull() {
		GameEngine eng = newEngine();
		assertNull(eng.field);

		// Should not throw
		eng.resetFieldVisible();
	}

	@Test
	void resetFieldVisibleRestoresAlphaDarknessAndAttributesOnEveryColoredBlock() {
		GameEngine eng = newEngine();
		eng.field = new Field(10, 20, 3, false);
		eng.field.setBlockColor(2, 5, Block.BLOCK_COLOR_RED);
		Block b = eng.field.getBlock(2, 5);
		b.alpha = 0.3f;
		b.darkness = 0.7f;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, false);

		eng.resetFieldVisible();

		assertEquals(1f, b.alpha);
		assertEquals(0f, b.darkness);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE));
	}

	@Test
	void playSEDelegatesToReceiverWhenEnabled() {
		// Recording receiver lets us observe the SE name passed through.
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		gm.init();
		gm.engine[0].init();
		GameEngine eng = gm.engine[0];

		eng.enableSE = true;
		eng.playSE("lock");

		assertEquals("lock", receiver.lastSE);

		eng.enableSE = false;
		eng.playSE("hold");
		assertEquals("lock", receiver.lastSE,
				"playSE must not fire when enableSE is false");
	}

	@Test
	void isHoldOkReturnsFalseWhenHoldDisabledByRule() {
		GameEngine eng = newEngine();
		eng.ruleopt.holdEnable = false;

		assertFalse(eng.isHoldOK());
	}

	@Test
	void isHoldOkReturnsFalseWhenHoldUsageExceedsLimit() {
		GameEngine eng = newEngine();
		eng.ruleopt.holdEnable = true;
		eng.ruleopt.holdLimit = 1;
		eng.holdUsedCount = 2;

		assertFalse(eng.isHoldOK());
	}

	@Test
	void isHoldOkAllowsHoldWithDefaultRuleOptionsAndCleanState() {
		GameEngine eng = newEngine();
		eng.ruleopt.holdEnable = true;
		eng.ruleopt.holdLimit = -1;
		eng.holdDisable = false;
		eng.initialHoldContinuousUse = false;

		assertTrue(eng.isHoldOK());
	}

	@Test
	void shutdownNullsOutCoreReferences() {
		// Build engine in isolation — shutdown breaks the GameManager owner link.
		GameEngine eng = new GameEngine(new GameManager(new EventReceiver()), 0,
				new RuleOptions(), null, null);
		eng.statc = new int[GameEngine.MAX_STATC];

		eng.shutdown();

		assertNull(eng.owner);
		assertNull(eng.ruleopt);
		assertNull(eng.field);
		assertNull(eng.ctrl);
		assertNull(eng.statistics);
		assertNull(eng.speed);
		assertNull(eng.random);
		assertNull(eng.replayData);
	}

	private static final class RecordingReceiver extends EventReceiver {
		String lastSE;

		@Override
		public void playSE(String name) {
			this.lastSE = name;
		}
	}
}
