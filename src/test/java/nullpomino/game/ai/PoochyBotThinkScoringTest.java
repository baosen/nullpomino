package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers PoochyBot {@link PoochyBot#thinkMain} I-piece valley-bonus scoring:
 * the {@code valley == 3} bonus (1319-1320) and the {@code xMax == 0}
 * left-edge doubling (1323-1324). Both need a vertical I dropped into a column
 * exactly three cells deeper than its neighbours.
 */
class PoochyBotThinkScoringTest {

	private GameEngine engine;
	private PoochyBot ai;

	@BeforeEach
	void setUp() {
		GameManager m = new GameManager(new EventReceiver());
		m.init();
		engine = m.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		ai = new PoochyBot();
	}

	private Piece iPiece() {
		Piece p = new Piece(Piece.PIECE_I);
		p.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_I],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_I]);
		p.direction = Piece.DIRECTION_RIGHT;   // vertical, single column at x+2
		return p;
	}

	@Test
	void valleyDepthThreeMiddleColumn() {
		Field fld = new Field(10, 20, 0, false);
		// Columns 3 and 5 stacked to row 14; column 4 only to row 17 -> 3 deeper.
		for (int y = 14; y < 20; y++) {
			fld.setBlockColor(3, y, Block.BLOCK_COLOR_RED);
			fld.setBlockColor(5, y, Block.BLOCK_COLOR_RED);
		}
		for (int y = 17; y < 20; y++) fld.setBlockColor(4, y, Block.BLOCK_COLOR_RED);
		Piece i = iPiece();
		int x = 2;                              // single column at x+2 = 4 (middle)
		int y = i.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
		int pts = ai.thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, i, 0);
		assertNotEquals(Integer.MIN_VALUE, pts, "valley==3 placement should be scored");
	}

	@Test
	void valleyAtLeftEdgeColumnZeroDoublesBonus() {
		Field fld = new Field(10, 20, 0, false);
		// Column 1 stacked to row 14; column 0 only to row 17 -> column 0 is the
		// 3-deep valley, and xMax == 0 doubles the bonus.
		for (int y = 14; y < 20; y++) fld.setBlockColor(1, y, Block.BLOCK_COLOR_BLUE);
		for (int y = 17; y < 20; y++) fld.setBlockColor(0, y, Block.BLOCK_COLOR_BLUE);
		Piece i = iPiece();
		int x = -2;                             // single column at x+2 = 0 (left edge)
		int y = i.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
		int pts = ai.thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, i, 0);
		assertNotEquals(Integer.MIN_VALUE, pts, "left-edge valley placement should be scored");
	}
}
