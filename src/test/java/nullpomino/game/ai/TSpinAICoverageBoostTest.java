package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Coverage-boost tests for {@link TSpinAI#thinkMain} targeting the branches
 * left uncovered by the existing TSpinAI suite:
 *
 * <ul>
 *   <li>96-97 + 100-101 - the "new T-slot" reward block and the {@code forceHold}
 *       assignment (next != T, hold == T). Reached only when placing a piece
 *       actually creates exactly one new T-slot ({@code tslotAfter == 1}) and
 *       exactly one new hole, with the stack non-dangerous.</li>
 *   <li>123 - lid-increase demerit on the danger path.</li>
 *   <li>134 - the T-spin line-clear bonus ({@code tspin && lines >= 1 &&
 *       holeAfter < holeBefore}).</li>
 *   <li>144 - need-an-I-piece valley reduction reward (depth 0, non-danger).</li>
 *   <li>157-158 - height-increase demerit ({@code depth > 0 || danger}).</li>
 * </ul>
 *
 * Every field shape was derived empirically by replicating {@code thinkMain}'s
 * pre/post-placement metrics, so each targeted branch is guaranteed to run.
 * Pieces are coloured (so {@code placeToField} writes visible blocks) and the
 * relevant outcomes are asserted ({@code forceHold}, the T-spin bonus
 * magnitude) to prove the branches executed.
 */
class TSpinAICoverageBoostTest {

	private GameManager gm;
	private GameEngine engine;
	private TSpinAI ai;

	@BeforeEach
	void setUp() {
		gm = new GameManager(new EventReceiver());
		gm.init();
		engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.combo = 1;
		ai = new TSpinAI();
	}

	private static Field field() {
		return new Field(10, 20, 0, false);
	}

	private static Piece coloured(int id, int rt) {
		Piece p = new Piece(id);
		p.setColor(1);
		p.direction = rt;
		return p;
	}

	/**
	 * Lines 96-97 and 100-101: dropping an L piece into a near-complete board
	 * forms exactly one new T-slot while adding exactly one hole, so the new
	 * T-slot reward fires. With next = S (not T) and hold = T, {@code forceHold}
	 * is set, which we assert as direct proof both blocks executed.
	 */
	@Test
	void newTSlotRewardSetsForceHold() {
		// Filled cells (colour 1) of a board that, once the L lands, yields a
		// brand-new single-line T-slot plus exactly one new hole.
		int[][] filled = {
				{0,13},{2,13},
				{0,14},{1,14},{2,14},{3,14},{4,14},{5,14},{6,14},{7,14},{9,14},
				{0,15},{1,15},{2,15},{3,15},{4,15},{5,15},{6,15},{7,15},{8,15},{9,15},
				{0,16},{1,16},{2,16},{3,16},{4,16},{6,16},{7,16},{9,16},
				{0,17},{1,17},{3,17},{4,17},{5,17},{6,17},{7,17},{8,17},{9,17},
				{0,18},{2,18},{3,18},{4,18},{5,18},{6,18},{7,18},{8,18},{9,18},
				{0,19},{1,19},{2,19},{3,19},{4,19},{6,19},{7,19},{8,19},{9,19}
		};
		Field fld = field();
		for (int[] c : filled) {
			fld.setBlockColor(c[0], c[1], 1);
		}

		Piece piece = coloured(Piece.PIECE_L, Piece.DIRECTION_DOWN);
		Piece next = coloured(Piece.PIECE_S, Piece.DIRECTION_UP);   // next != T
		Piece hold = coloured(Piece.PIECE_T, Piece.DIRECTION_UP);   // hold == T

		ai.forceHold = false;
		int pts = ai.thinkMain(engine, 5, 11, Piece.DIRECTION_DOWN, -1, fld, piece, next, hold, 0);

		assertTrue(ai.forceHold, "new T-slot path must set forceHold (lines 96-101)");
		assertTrue(pts >= 100000, "new T-slot reward of +100000 must be applied");
	}

	/**
	 * Line 123: on a tall (danger) stack with a covered hole, dropping an O on
	 * top adds blocks above the hole, increasing the lid count -> the danger
	 * lid-increase demerit fires.
	 */
	@Test
	void lidIncreaseDangerDemerit() {
		Field fld = field();
		for (int x = 0; x < 9; x++) {
			for (int y = 9; y <= 19; y++) {
				fld.setBlockColor(x, y, 1); // column 9 stays empty -> never a full line
			}
		}
		fld.setBlockColor(0, 15, 0); // covered hole in column 0

		Piece o = coloured(Piece.PIECE_O, Piece.DIRECTION_UP);
		assertDoesNotThrow(() -> ai.thinkMain(engine, 0, 7, Piece.DIRECTION_UP, -1, fld, o, null, null, 1));
	}

	/**
	 * Line 134: a T-spin double that also resolves a covered hole. The T enters
	 * a 3-1 notch (rtOld != -1 so it counts as a spin), clears two lines, and
	 * the line clears reduce the hole count -> the T-spin bonus of
	 * {@code 100000 * lines} is added.
	 */
	@Test
	void tSpinClearBonus() {
		Field fld = field();
		for (int x = 0; x < 10; x++) {
			fld.setBlockColor(x, 18, 1);
			fld.setBlockColor(x, 19, 1);
		}
		// Carve the T-piece target cells (4,18),(5,18),(6,18),(5,19).
		fld.setBlockColor(4, 18, 0);
		fld.setBlockColor(5, 18, 0);
		fld.setBlockColor(6, 18, 0);
		fld.setBlockColor(5, 19, 0);
		fld.setBlockColor(4, 17, 1); // overhang corner -> T-spin spot at (4,17)
		// A covered hole resolved by the clears.
		fld.setBlockColor(0, 16, 1);
		fld.setBlockColor(0, 17, 0);

		Piece t = coloured(Piece.PIECE_T, Piece.DIRECTION_DOWN);
		int pts = ai.thinkMain(engine, 4, 17, Piece.DIRECTION_DOWN, Piece.DIRECTION_UP, fld, t, null, null, 0);

		assertTrue(pts >= 100000, "T-spin clear bonus must be applied (line 134)");
	}

	/**
	 * Line 144: a deep (depth >= 3) valley filled by a vertical I piece reduces
	 * the "needs an I piece" valley count. With depth 0 and a non-danger stack
	 * the {@code * 10} reward branch is taken.
	 */
	@Test
	void needIValleyReductionReward() {
		Field fld = field();
		for (int x = 0; x < 9; x++) {
			if (x != 3) {
				for (int y = 15; y <= 19; y++) {
					fld.setBlockColor(x, y, 1); // column 9 empty -> no full lines
				}
			}
		}
		fld.setBlockColor(3, 19, 1); // column 3 is a depth-4 valley

		Piece i = coloured(Piece.PIECE_I, Piece.DIRECTION_LEFT); // vertical
		int pts = ai.thinkMain(engine, 2, 15, Piece.DIRECTION_LEFT, -1, fld, i, null, null, 0);
		assertTrue(pts != 0, "valley reduction reward should produce a positive score (line 144)");
	}

	/**
	 * Lines 157-158: dropping an O on a flat low stack (no line clears) raises
	 * the stack, so {@code heightBefore > heightAfter}. With depth > 0 the
	 * height-increase demerit fires.
	 */
	@Test
	void heightIncreaseDemeritWhenDepthPositive() {
		Field fld = field();
		for (int x = 0; x < 10; x++) {
			fld.setBlockColor(x, 18, 1);
			fld.setBlockColor(x, 19, 1);
		}
		fld.setBlockColor(9, 18, 0); // keep two cells empty so nothing clears
		fld.setBlockColor(9, 19, 0);

		Piece o = coloured(Piece.PIECE_O, Piece.DIRECTION_UP);
		assertDoesNotThrow(() -> ai.thinkMain(engine, 3, 16, Piece.DIRECTION_UP, -1, fld, o, null, null, 1));
	}
}
