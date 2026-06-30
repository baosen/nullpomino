package nullpomino.game.ai;

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
 * Targets {@link PoochyBot#mostMovableX} finesse branches the game simulation
 * doesn't reach: the T-piece (direction != UP) lower-bottom kick handling
 * (1690-1711) and the vertical-I left-edge height returns (1738-1745). High
 * gravity bypasses the low-gravity early-out so the kick loop runs.
 */
class PoochyBotMostMovableXTest {

	private GameManager manager;
	private GameEngine engine;
	private PoochyBot ai;

	@BeforeEach
	void setUp() {
		manager = new GameManager(new EventReceiver());
		manager.init();
		engine = manager.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.speed.gravity = 1;        // gravity >= denominator -> skip low-gravity return
		engine.speed.denominator = 1;
		engine.ruleopt.rotateMaxUpwardWallkick = -1;  // floorKickOK enabled
		ai = new PoochyBot();
	}

	private Piece piece(int id) {
		Piece p = new Piece(id);
		p.applyOffsetArray(engine.ruleopt.pieceOffsetX[id], engine.ruleopt.pieceOffsetY[id]);
		return p;
	}

	private void floor(Field f, int yTop) {
		for (int y = yTop; y < f.getHeight(); y++)
			for (int x = 0; x < f.getWidth(); x++)
				f.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
	}

	private void clearCol(Field f, int x, int y0, int y1) {
		for (int y = y0; y < y1; y++) f.setBlockColor(x, y, Block.BLOCK_COLOR_NONE);
	}

	@Test
	void tPieceDownStemHoleKicks() {
		// A flat floor with a single deep 1-wide hole: a DOWN-oriented T's centre
		// stem falls into the hole (testY2 > testY), triggering the kick branches.
		for (int dir : new int[]{1, -1}) {
			for (int holeCol = 1; holeCol <= 8; holeCol++) {
				Field f = new Field(10, 20, 3, false);
				floor(f, 16);
				clearCol(f, holeCol, 13, 20);            // deep 1-wide hole
				Piece t = piece(Piece.PIECE_T);
				t.direction = Piece.DIRECTION_DOWN;       // != UP -> special branch
				int startX = holeCol - 1;
				int r = ai.mostMovableX(startX, 2, dir, engine, f, t, Piece.DIRECTION_UP);
				assertTrue(r >= -2, "T mostMovableX returned " + r);
			}
		}
	}

	@Test
	void tPieceDownNarrowSlotVariants() {
		// Walls flanking the hole at different gaps to drive kickLeft / no-kick paths.
		int[][] configs = {{4, 6}, {3, 5}, {2, 7}, {5, 7}};
		for (int[] c : configs) {
			for (int dir : new int[]{1, -1}) {
				Field f = new Field(10, 20, 3, false);
				floor(f, 18);
				for (int y = 10; y < 20; y++) {
					f.setBlockColor(c[0], y, Block.BLOCK_COLOR_RED);
					f.setBlockColor(c[1], y, Block.BLOCK_COLOR_RED);
				}
				Piece t = piece(Piece.PIECE_T);
				t.direction = Piece.DIRECTION_LEFT;
				int r = ai.mostMovableX((c[0] + c[1]) / 2, 2, dir, engine, f, t, Piece.DIRECTION_UP);
				assertTrue(r >= -2, "T slot mostMovableX returned " + r);
			}
		}
	}

	@Test
	void verticalIPieceLeftEdge() {
		// A vertical I driven left off the edge with various left-column heights,
		// to exercise the testX<0 height comparisons (1738-1745).
		int[][] heights = {{3, 8, 8}, {8, 3, 3}, {2, 9, 2}, {9, 2, 9}};
		for (int[] h : heights) {
			Field f = new Field(10, 20, 3, false);
			floor(f, 19);
			for (int col = 0; col < 3; col++)
				for (int y = 20 - h[col]; y < 20; y++)
					f.setBlockColor(col, y, Block.BLOCK_COLOR_BLUE);
			Piece iPiece = piece(Piece.PIECE_I);
			iPiece.direction = Piece.DIRECTION_RIGHT;   // vertical (rt&1)==1
			int r = ai.mostMovableX(2, 2, -1, engine, f, iPiece, Piece.DIRECTION_RIGHT);
			assertTrue(r >= -2, "vertical I mostMovableX returned " + r);
		}
	}

	@Test
	void verticalIPieceLeftEdgeReturnZero() {
		// Vertical I stopped against a tall left wall (block ends at col 1, testX<0)
		// with col1 taller than col2/col3 so height1 < hbY(2) && height1 < hbY(3)+2
		// -> the return-0 arm (1742-1743).
		Field f = new Field(10, 20, 3, false);
		for (int y = 5; y < 20; y++) f.setBlockColor(0, y, Block.BLOCK_COLOR_GRAY); // left wall
		for (int x = 1; x < 10; x++) {
			int top = (x == 1) ? 17 : 19;   // col1 taller than the rest
			for (int y = top; y < 20; y++) f.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
		}
		Piece iPiece = piece(Piece.PIECE_I);
		iPiece.direction = Piece.DIRECTION_RIGHT;
		int r = ai.mostMovableX(3, 2, -1, engine, f, iPiece, Piece.DIRECTION_RIGHT);
		assertTrue(r >= -2, "vertical I return-zero mostMovableX returned " + r);
	}

	@Test
	void bigIPieceFloorKick() {
		// Big I piece needing a floor kick during the move loop (1722-1731, 1725).
		Field f = new Field(10, 20, 3, false);
		floor(f, 14);
		for (int y = 8; y < 20; y++) f.setBlockColor(5, y, Block.BLOCK_COLOR_GREEN);
		Piece iPiece = piece(Piece.PIECE_I);
		iPiece.big = true;
		iPiece.direction = Piece.DIRECTION_RIGHT;
		int r = ai.mostMovableX(2, 2, 1, engine, f, iPiece, Piece.DIRECTION_UP);
		assertTrue(r >= -2, "big I mostMovableX returned " + r);
	}
}
