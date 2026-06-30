package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * Fills the last guard branches in BaseStandardWallkick and GBCWallkick:
 * <ul>
 *   <li>BaseStandardWallkick L45 {@code if((y2 >= 0) || (allowUpward))}: a kick
 *       entry with a negative y-offset reached while {@code allowUpward} is
 *       false, so the {@code allowUpward} sub-condition is evaluated to its
 *       false outcome and the whole guard is false.</li>
 *   <li>GBCWallkick L28: the {@code y2 >= 0} sub-condition taking its true
 *       outcome (the existing tests only ever feed a {-1} y-offset).</li>
 * </ul>
 */
class StandardGbcGuardBranchCoverageTest {

	/**
	 * StandardWallkick with allowUpward=false and a completely filled field: the
	 * NORMAL_R[0] table contains the entry {-1,-1} (y2 = -1). When that entry is
	 * reached, {@code y2 >= 0} is false so {@code allowUpward} is evaluated and is
	 * also false, making the guard false. Every other entry collides, so the
	 * overall result is null.
	 */
	@Test
	void baseStandardNegativeOffsetWithAllowUpwardFalseSkipsGuard() {
		StandardWallkick wallkick = new StandardWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field(10, 20, 2);
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 20; y++) {
				field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		WallkickResult result = wallkick.executeWallkick(
				4, 10, 1, 0, 1, false, piece, field, null);

		assertNull(result);
	}

	/**
	 * GBCWallkick right rotation from direction 2: KICKTABLE_R[2] = {1, 1}, so
	 * y2 = 1 and {@code y2 >= 0} is true (the previously-uncovered true outcome).
	 * The {@code ||} short-circuits and, with the kicked cell free, a result is
	 * produced.
	 */
	@Test
	void gbcPositiveYOffsetTakesY2NonNegativeBranch() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field(10, 20, 2);

		WallkickResult result = wallkick.executeWallkick(
				4, 5, 1, 2, 3, false, piece, field, null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
		assertEquals(1, result.offsetY);
	}
}
