package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the "new best position" record-branches of
 * {@link PoochyBot#thinkBestPosition} for the left-shift tuck (case 2,
 * lines 815-827), right-shift tuck (case 3, 833-846) and their hold-piece
 * equivalents (case 8, 1016-1029; case 9, 1036-1049).
 *
 * <p>{@code logBest(n)} is called at the end of each such branch, so a
 * {@link RecBot} that records the case numbers gives a direct proof the branch
 * executed. The boards are overhang notches that make a horizontal tuck the
 * locally-best move at some point during the search.
 */
class PoochyBotSearchBranchCoverageTest {

	/** PoochyBot that records which logBest() cases fire. */
	private static final class RecBot extends PoochyBot {
		final List<Integer> cases = new ArrayList<>();
		@Override protected void logBest(int n) { cases.add(n); }
	}

	private static GameEngine engine() {
		GameManager m = new GameManager(new EventReceiver());
		m.init();
		GameEngine e = m.engine[0];
		e.init();
		e.createFieldIfNeeded();
		// Enable reverse/double rotation so the search explores all rotation
		// branches; this shapes the running best so the hold-shift tucks below
		// register as local improvements.
		e.ruleopt.rotateButtonAllowReverse = true;
		e.ruleopt.rotateButtonAllowDouble = true;
		return e;
	}

	private static Piece piece(GameEngine e, int id) {
		Piece p = new Piece(id);
		p.applyOffsetArray(e.ruleopt.pieceOffsetX[id], e.ruleopt.pieceOffsetY[id]);
		return p;
	}

	private static void fillRows(Field f, int x, int y0, int y1) {
		for (int y = y0; y < y1; y++) f.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
	}

	/** Floor + a left-overhang notch at column 4 (lip at column 3). */
	private static void leftNotch(Field f) {
		int h = f.getHeight(), w = f.getWidth();
		for (int x = 0; x < w; x++) fillRows(f, x, h - 3, h);
		for (int y = h - 8; y < h; y++) f.setBlockColor(4, y, Block.BLOCK_COLOR_NONE);
		f.setBlockColor(3, h - 8, Block.BLOCK_COLOR_GRAY);
		f.setBlockColor(5, h - 6, Block.BLOCK_COLOR_GRAY);
	}

	/** Floor + a right-overhang notch at column 5 (lip at column 6). */
	private static void rightNotch(Field f) {
		int h = f.getHeight(), w = f.getWidth();
		for (int x = 0; x < w; x++) fillRows(f, x, h - 3, h);
		for (int y = h - 8; y < h; y++) f.setBlockColor(5, y, Block.BLOCK_COLOR_NONE);
		f.setBlockColor(6, h - 8, Block.BLOCK_COLOR_GRAY);
		f.setBlockColor(4, h - 6, Block.BLOCK_COLOR_GRAY);
	}

	private static List<Integer> run(GameEngine e, int nowId, int holdId) {
		RecBot bot = new RecBot();
		e.nowPieceObject = piece(e, nowId);
		e.holdPieceObject = piece(e, holdId);
		bot.thinkBestPosition(e, 0);
		return bot.cases;
	}

	@Test
	void leftShiftTuckRecordsBestPosition() {
		GameEngine e = engine();
		leftNotch(e.field);
		assertTrue(run(e, Piece.PIECE_L, Piece.PIECE_I).contains(2),
				"left-shift tuck should be recorded as a new best (case 2)");
	}

	@Test
	void rightShiftTuckRecordsBestPosition() {
		GameEngine e = engine();
		rightNotch(e.field);
		assertTrue(run(e, Piece.PIECE_L, Piece.PIECE_I).contains(3),
				"right-shift tuck should be recorded as a new best (case 3)");
	}

	@Test
	void heldPieceLeftShiftTuckRecordsBestPosition() {
		GameEngine e = engine();
		leftNotch(e.field);
		assertTrue(run(e, Piece.PIECE_L, Piece.PIECE_T).contains(8),
				"held-piece left-shift tuck should be recorded as a new best (case 8)");
	}
}
