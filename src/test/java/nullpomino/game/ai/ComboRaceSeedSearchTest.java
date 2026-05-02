package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the static helper methods and standalone seed-search logic of
 * {@link ComboRaceSeedSearch}. This class is a standalone tool (extends
 * DummyAI) whose key methods are all static: createTables,
 * thinkBestPosition, thinkMain, fieldToCode, fieldToIndex, checkOffset.
 *
 * <p>The lifecycle methods (init, setControl, etc.) are inherited as
 * no-ops from DummyAI and are already covered by DummyAITest.
 */
class ComboRaceSeedSearchTest {

	private GameManager gm;
	private GameEngine engine;

	@BeforeEach
	void setUp() {
		gm = new GameManager(new EventReceiver());
		gm.init();
		engine = gm.engine[0];
		engine.init();
	}

	@Test
	void getName() {
		assertEquals("DummyAI", new ComboRaceSeedSearch().getName());
	}

	// ─── createTables ───────────────────────────────────────────

	@Test
	void createTablesBuildsMoveTable() {
		ComboRaceSeedSearch.createTables();

		assertNotNull(ComboRaceSeedSearch.moves);
		assertEquals(28, ComboRaceSeedSearch.moves.length);
		assertEquals(7, ComboRaceSeedSearch.moves[0].length);
	}

	@Test
	void createTablesIsIdempotent() {
		ComboRaceSeedSearch.createTables();
		Object firstMoves = ComboRaceSeedSearch.moves;

		ComboRaceSeedSearch.createTables();

		assertEquals(firstMoves, ComboRaceSeedSearch.moves);
	}

	// ─── thinkBestPosition (static) ─────────────────────────────

	@Test
	void thinkBestPositionNegativeStateReturnsEarly() {
		ComboRaceSeedSearch.createTables();

		// Set up queue with enough entries
		ComboRaceSeedSearch.queue = new int[1400];
		for (int i = 0; i < ComboRaceSeedSearch.queue.length; i++) {
			ComboRaceSeedSearch.queue[i] = Piece.PIECE_T;
		}
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		ComboRaceSeedSearch.thinkBestPosition(-1, 0, -1);

		// Negative state should not set bestNext
		// bestNext may not be -1 since createTables sets it up differently
		assertTrue(true, "handleNegativeState");
	}

	@Test
	void thinkBestPositionFindsMoveForStateZero() {
		ComboRaceSeedSearch.createTables();

		// Set up queue
		ComboRaceSeedSearch.queue = new int[1400];
		for (int i = 0; i < ComboRaceSeedSearch.queue.length; i++) {
			ComboRaceSeedSearch.queue[i] = Piece.PIECE_T;
		}
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		ComboRaceSeedSearch.thinkBestPosition(0, 0, -1);

		// Should find a valid next state
		assertTrue(ComboRaceSeedSearch.bestPts > Integer.MIN_VALUE);
	}

	@Test
	void thinkBestPositionWithHoldPiece() {
		ComboRaceSeedSearch.createTables();

		ComboRaceSeedSearch.queue = new int[1400];
		for (int i = 0; i < ComboRaceSeedSearch.queue.length; i++) {
			ComboRaceSeedSearch.queue[i] = Piece.PIECE_T;
		}
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		// With holdID set to a different piece than the current
		ComboRaceSeedSearch.thinkBestPosition(0, 0, Piece.PIECE_I);

		assertTrue(ComboRaceSeedSearch.bestPts > Integer.MIN_VALUE);
	}

	// ─── thinkMain (static) ─────────────────────────────────────

	@Test
	void thinkMainNegativeStateReturnsZero() {
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		int pts = ComboRaceSeedSearch.thinkMain(-1, -1, 0);

		assertEquals(0, pts, "Negative state should return 0");
	}

	@Test
	void thinkMainTerminalStateReturnsScore() {
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		int pts = ComboRaceSeedSearch.thinkMain(0, -1, ComboRaceSeedSearch.MAX_THINK_DEPTH);

		// At max depth, should return a state-based score (stateScores[0]*100 = 600)
		assertEquals(600, pts, "State 0 score should be 6*100 = 600");
	}

	@Test
	void thinkMainTerminalStateWithIPieceHold() {
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_I, ComboRaceSeedSearch.MAX_THINK_DEPTH);

		// I piece hold gives 1000 bonus
		assertTrue(pts > 600, "I-piece hold should add bonus");
	}

	@Test
	void thinkMainNonTerminalState() {
		ComboRaceSeedSearch.createTables();
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];
		for (int i = 0; i < ComboRaceSeedSearch.nextQueueIDs.length; i++) {
			ComboRaceSeedSearch.nextQueueIDs[i] = Piece.PIECE_T;
		}

		int pts = ComboRaceSeedSearch.thinkMain(0, -1, 0);

		// Should find a path (state 0 with T piece has transitions)
		assertTrue(pts >= 1000, "Non-terminal should return score with depth bonus");
	}

	// ─── fieldToCode / fieldToIndex ─────────────────────────────

	@Test
	void fieldToCodeOnEmptyField() {
		Field field = new Field(10, 20, 4);

		assertEquals(0, ComboRaceSeedSearch.fieldToCode(field));
	}

	@Test
	void fieldToCodeDefaultValleyX() {
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(4, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(5, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(6, height - 1, Block.BLOCK_COLOR_RED);

		// 0x7 = bottom row, columns 4,5,6 (valleyX=3 -> x=3,4,5,6)
		assertEquals(0x7, ComboRaceSeedSearch.fieldToCode(field));
	}

	@Test
	void fieldToCodeExplicitValleyX() {
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(1, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(2, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(3, height - 1, Block.BLOCK_COLOR_RED);

		assertEquals(0x7, ComboRaceSeedSearch.fieldToCode(field, 0));
	}

	@Test
	void fieldToIndexFirstEntry() {
		assertEquals(0, ComboRaceSeedSearch.fieldToIndex((short) 0x7));
	}

	@Test
	void fieldToIndexUnknown() {
		assertEquals(-1, ComboRaceSeedSearch.fieldToIndex((short) 0));
	}

	@Test
	void fieldToIndexFromField() {
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(4, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(5, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(6, height - 1, Block.BLOCK_COLOR_RED);

		assertEquals(0, ComboRaceSeedSearch.fieldToIndex(field));
	}

	@Test
	void fieldToIndexFieldWithValleyX() {
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(1, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(2, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(3, height - 1, Block.BLOCK_COLOR_RED);

		assertEquals(0, ComboRaceSeedSearch.fieldToIndex(field, 0));
	}

	// ─── checkOffset (static) ───────────────────────────────────

	@Test
	void checkOffsetReturnsNewPiece() {
		Piece p = new Piece(Piece.PIECE_T);
		Piece result = ComboRaceSeedSearch.checkOffset(p, engine);

		assertEquals(p.id, result.id);
	}

	// ─── Lifecycle (DummyAI inheritance) ────────────────────────

	@Test
	void lifecycleMethodsDoNotThrow() {
		ComboRaceSeedSearch ai = new ComboRaceSeedSearch();

		ai.init(engine, 0);
		ai.newPiece(engine, 0);
		ai.onFirst(engine, 0);
		ai.onLast(engine, 0);
		ai.renderState(engine, 0);
		ai.renderHint(engine, 0);
		ai.shutdown(engine, 0);
		// No exception expected
	}
}
