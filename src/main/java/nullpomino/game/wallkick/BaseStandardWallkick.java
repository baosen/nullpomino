package nullpomino.game.wallkick;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * Base class for all Standard (SRS) wallkicks.
 *
 * <p>The four SRS variants ({@link StandardWallkick}, {@link StandardSymmetricWallkick},
 * {@link StandardMild180Wallkick}, {@link StandardSymmetricMild180Wallkick}) share the
 * same normal/I2/I3/L3 kick tables and the same piece-and-direction dispatch. Only the
 * I-piece tables (symmetric vs. asymmetric) and the 180-degree tables (full vs. mild)
 * differ between them. Those shared tables and the dispatch live here; each subclass
 * supplies only the tables it changes and forwards to {@link #selectKickTable}.
 */
public class BaseStandardWallkick implements Wallkick {
	// Wallkick data shared by every Standard variant
	private static final int WALLKICK_NORMAL_L[][][] =
	{
		{{ 1, 0},{ 1,-1},{ 0, 2},{ 1, 2}},	// 0>>3
		{{ 1, 0},{ 1, 1},{ 0,-2},{ 1,-2}},	// 1>>0
		{{-1, 0},{-1,-1},{ 0, 2},{-1, 2}},	// 2>>1
		{{-1, 0},{-1, 1},{ 0,-2},{-1,-2}},	// 3>>2
	};
	private static final int WALLKICK_NORMAL_R[][][] =
	{
		{{-1, 0},{-1,-1},{ 0, 2},{-1, 2}},	// 0>>1
		{{ 1, 0},{ 1, 1},{ 0,-2},{ 1,-2}},	// 1>>2
		{{ 1, 0},{ 1,-1},{ 0, 2},{ 1, 2}},	// 2>>3
		{{-1, 0},{-1, 1},{ 0,-2},{-1,-2}},	// 3>>0
	};
	private static final int WALLKICK_I2_L[][][] =
	{
		{{ 1, 0},{ 0,-1},{ 1,-2}},			// 0>>3
		{{ 0, 1},{ 1, 0},{ 1, 1}},			// 1>>0
		{{-1, 0},{ 0, 1},{-1, 0}},			// 2>>1
		{{ 0,-1},{-1, 0},{-1, 1}},			// 3>>2
	};
	private static final int WALLKICK_I2_R[][][] =
	{
		{{ 0,-1},{-1, 0},{-1,-1}},			// 0>>1
		{{ 1, 0},{ 0,-1},{ 1, 0}},			// 1>>2
		{{ 0, 1},{ 1, 0},{ 1,-1}},			// 2>>3
		{{-1, 0},{ 0, 1},{-1, 2}},			// 3>>0
	};
	private static final int WALLKICK_I3_L[][][] =
	{
		{{ 1, 0},{-1, 0},{ 0, 0},{ 0, 0}},	// 0>>3
		{{-1, 0},{ 1, 0},{ 0,-1},{ 0, 1}},	// 1>>0
		{{-1, 0},{ 1, 0},{ 0, 2},{ 0,-2}},	// 2>>1
		{{ 1, 0},{-1, 0},{ 0,-1},{ 0, 1}},	// 3>>2
	};
	private static final int WALLKICK_I3_R[][][] =
	{
		{{ 1, 0},{-1, 0},{ 0, 1},{ 0,-1}},	// 0>>1
		{{ 1, 0},{-1, 0},{ 0,-2},{ 0, 2}},	// 1>>2
		{{-1, 0},{ 1, 0},{ 0, 1},{ 0,-1}},	// 2>>3
		{{-1, 0},{ 1, 0},{ 0, 0},{ 0, 0}},	// 3>>0
	};
	private static final int WALLKICK_L3_L[][][] =
	{
		{{ 0,-1},{ 0, 1}},					// 0>>3
		{{ 1, 0},{-1, 0}},					// 1>>0
		{{ 0, 1},{ 0,-1}},					// 2>>1
		{{-1, 0},{ 1, 0}},					// 3>>2
	};
	private static final int WALLKICK_L3_R[][][] =
	{
		{{-1, 0},{ 1, 0}},					// 0>>1
		{{ 0,-1},{ 0, 1}},					// 1>>2
		{{ 1, 0},{-1, 0}},					// 2>>3
		{{ 0, 1},{ 0,-1}},					// 3>>0
	};

	/**
	 * Get wallkick table. Used from executeWallkick.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rtDir Rotation button used (-1: left rotation, 1: right rotation, 2: 180-degree rotation)
	 * @param rtOld Direction before rotation
	 * @param rtNew Direction after rotation
	 * @param allowUpward If true, upward wallkicks are allowed.
	 * @param piece Current piece
	 * @param field Current field
	 * @param ctrl Button input status (it may be null, when controlled by an AI)
	 * @return Wallkick Table. You may return null if you don't want to execute a kick.
	 */
	protected int[][][] getKickTable(int x, int y, int rtDir, int rtOld, int rtNew, boolean allowUpward, Piece piece, Field field, Controller ctrl) {
		return null;
	}

	/**
	 * Select the SRS kick table for the given rotation, using the shared normal/I2/I3/L3
	 * tables and the variant-specific I-piece and 180-degree tables passed by the caller.
	 * @param rtDir Rotation button used (-1: left, 1: right, 2: 180-degree)
	 * @param pieceId Current piece id
	 * @param iKickL I-piece table for left rotation
	 * @param iKickR I-piece table for right rotation
	 * @param normal180 180-degree table for non-I pieces
	 * @param i180 180-degree table for the I piece
	 * @return the selected kick table, or null if rtDir is not a recognised rotation
	 */
	protected final int[][][] selectKickTable(int rtDir, int pieceId,
			int[][][] iKickL, int[][][] iKickR, int[][][] normal180, int[][][] i180) {
		if(rtDir == 2) {
			// 180-degree rotation
			switch(pieceId) {
			case Piece.PIECE_I:
				return i180;
			default:
				return normal180;
			}
		} else if(rtDir == -1) {
			// Left rotation
			switch(pieceId) {
			case Piece.PIECE_I:
				return iKickL;
			case Piece.PIECE_I2:
				return WALLKICK_I2_L;
			case Piece.PIECE_I3:
				return WALLKICK_I3_L;
			case Piece.PIECE_L3:
				return WALLKICK_L3_L;
			default:
				return WALLKICK_NORMAL_L;
			}
		} else if(rtDir == 1) {
			// Right rotation
			switch(pieceId) {
			case Piece.PIECE_I:
				return iKickR;
			case Piece.PIECE_I2:
				return WALLKICK_I2_R;
			case Piece.PIECE_I3:
				return WALLKICK_I3_R;
			case Piece.PIECE_L3:
				return WALLKICK_L3_R;
			default:
				return WALLKICK_NORMAL_R;
			}
		}

		return null;
	}

	/*
	 * Wallkick
	 */
	public WallkickResult executeWallkick(int x, int y, int rtDir, int rtOld, int rtNew, boolean allowUpward, Piece piece, Field field, Controller ctrl) {
		int[][][] kicktable = getKickTable(x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl);

		if(kicktable != null) {
			for(int i = 0; i < kicktable[rtOld].length; i++) {
				int x2 = kicktable[rtOld][i][0];
				int y2 = kicktable[rtOld][i][1];

				if(piece.big == true) {
					x2 *= 2;
					y2 *= 2;
				}

				if((y2 >= 0) || (allowUpward)) {
					if(piece.checkCollision(x + x2, y + y2, rtNew, field) == false) {
						return new WallkickResult(x2, y2, rtNew);
					}
				}
			}
		}

		return null;
	}
}
