package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertNull;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;

import org.junit.jupiter.api.Test;

/**
 * Closes the switch-default branch gaps in
 * {@link AvalancheClassicWallkick} (L23) and {@link PhysicianWallkick}
 * (L23): a rotation direction outside UP/RIGHT/DOWN/LEFT falls through to
 * {@code return null}.
 *
 * <p>With a real Piece the guard {@code piece.checkCollision(x, y, rtNew,
 * field)} would index its 4-direction data arrays out of bounds for such a
 * value, so the guard is stubbed to report a collision without indexing.
 */
class WallkickSwitchDefaultBranchGapTest {

	/** Reports a collision for any direction without touching piece data. */
	private static final class AlwaysCollidingPiece extends Piece {
		AlwaysCollidingPiece() {
			super(Piece.PIECE_O);
		}

		@Override
		public boolean checkCollision(int x, int y, int rt, Field fld) {
			return true;
		}
	}

	@Test
	void avalancheClassicUnknownDirectionFallsThroughToNull() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		assertNull(wallkick.executeWallkick(4, 4, 1, Piece.DIRECTION_UP, Piece.DIRECTION_RANDOM,
				true, new AlwaysCollidingPiece(), new Field(), null));
	}

	@Test
	void physicianUnknownDirectionFallsThroughToNull() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		assertNull(wallkick.executeWallkick(4, 4, 1, Piece.DIRECTION_UP, Piece.DIRECTION_RANDOM,
				true, new AlwaysCollidingPiece(), new Field(), null));
	}
}
