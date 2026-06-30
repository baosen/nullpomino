package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * Covers the remaining branches in ClassicWallkick and ClassicPlusWallkick:
 * <ul>
 *   <li>the {@code getCoordAttribute(...) != COORD_VANISH} guard taking its
 *       false outcome (a piece block sitting in the vanish zone), in both the
 *       normal and the big-piece collision-kick checks; and</li>
 *   <li>the ClassicPlus I-floor-kick loop exhausting (every lift blocked, so
 *       the {@code i <= check * 2} condition is evaluated false).</li>
 * </ul>
 *
 * The vanish zone is reached with a field whose hidden height is 0: any cell at
 * {@code y < 0} (with an in-range x and no ceiling) reports COORD_VANISH.
 */
class ClassicVanishBranchCoverageTest {

	// =============== ClassicWallkick ===============

	/**
	 * ClassicWallkick.checkCollisionKick (L66): a non-big T piece spawned with
	 * blocks in the vanish zone makes {@code getCoordAttribute == COORD_VANISH},
	 * so the {@code != COORD_VANISH} sub-condition takes its false branch.
	 */
	@Test
	void classicNonBigVanishZoneSkipsCollisionCell() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field(10, 20, 0);

		// T-UP checks blocks at (x, y+1) and (x+2, y+1). With y = -2 those land at
		// y = -1 (vanish, hidden height 0), x = 4 and 6 (in range, not a wall).
		WallkickResult result = wallkick.executeWallkick(
				4, -2, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);

		assertEquals(Field.COORD_VANISH, field.getCoordAttribute(4, -1));
		// No block colour in the vanish cells, so no kick is produced.
		assertNull(result);
	}

	/**
	 * ClassicWallkick.checkCollisionKickBig (L104): same vanish-zone false branch
	 * but exercised through the big-piece collision check.
	 */
	@Test
	void classicBigVanishZoneSkipsCollisionCell() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		piece.big = true;
		Field field = new Field(20, 30, 0);

		WallkickResult result = wallkick.executeWallkick(
				6, -4, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);

		assertEquals(Field.COORD_VANISH, field.getCoordAttribute(6, -1));
		assertNull(result);
	}

	// =============== ClassicPlusWallkick ===============

	/**
	 * ClassicPlusWallkick.checkCollisionKick (L111): non-big vanish-zone false branch.
	 */
	@Test
	void classicPlusNonBigVanishZoneSkipsCollisionCell() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field(10, 20, 0);

		// allowUpward=false keeps the T-escape branch from firing; checkCollisionKick
		// (the leftmost operand of the kick-eligibility ||) still runs and meets the
		// vanish cells, so the result is null.
		WallkickResult result = wallkick.executeWallkick(
				4, -2, 1, 0, Piece.DIRECTION_UP, false, piece, field, null);

		assertEquals(Field.COORD_VANISH, field.getCoordAttribute(4, -1));
		assertNull(result);
	}

	/**
	 * ClassicPlusWallkick.checkCollisionKickBig (L149): big vanish-zone false branch.
	 */
	@Test
	void classicPlusBigVanishZoneSkipsCollisionCell() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		piece.big = true;
		Field field = new Field(20, 30, 0);

		WallkickResult result = wallkick.executeWallkick(
				6, -4, 1, 0, Piece.DIRECTION_UP, false, piece, field, null);

		assertEquals(Field.COORD_VANISH, field.getCoordAttribute(6, -1));
		assertNull(result);
	}

	/**
	 * ClassicPlusWallkick I-floor-kick (L66): the I piece is in contact with the
	 * floor (so the floor-kick block is entered) but every upward lift is blocked,
	 * so {@code temp} stays 0, no early return fires and the
	 * {@code for(i = check; i <= check * 2; i++)} condition is evaluated false to
	 * exit the loop.
	 */
	@Test
	void classicPlusIFloorKickLoopExhaustsWhenAllLiftsBlocked() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I);
		Field field = new Field(10, 20, 0);

		// Fill the whole field, then carve a 4-cell vertical slot at column 5
		// (rows 13..16) so the I piece (rotated LEFT: a vertical bar at x+1)
		// fits there but is blocked below (floor contact) and above (no lift).
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 20; y++) {
				field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}
		for (int y = 13; y <= 16; y++) {
			field.setBlock(5, y, new Block());
		}

		// Floor contact precondition for the I-floor-kick block.
		assertTrue(piece.checkCollision(4, 14, field));

		WallkickResult result = wallkick.executeWallkick(
				4, 13, 1, Piece.DIRECTION_UP, Piece.DIRECTION_LEFT, true, piece, field, null);

		// Both lifts blocked, so the loop exhausts and no kick is returned.
		assertNull(result);
	}
}
