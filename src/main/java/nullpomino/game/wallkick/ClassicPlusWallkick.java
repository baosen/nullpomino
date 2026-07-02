// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.wallkick;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * ClassicPlusWallkick - ITypes are alsoWallkickIt is a classic rule that can beWallkick (OldVersionOfCLASSIC3Equivalent)
 */
public class ClassicPlusWallkick extends ClassicWallkick {
	/*
	 * Wallkick
	 */
	public WallkickResult executeWallkick(int x, int y, int rtDir, int rtOld, int rtNew, boolean allowUpward, Piece piece, Field field, Controller ctrl) {
		int check = 0;
		if(piece.big) check = 1;

		// NormalWallkick (IOther)
		WallkickResult result = super.executeWallkick(x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl);
		if(result != null) {
			return result;
		}

		// TEscape
		if((piece.id == Piece.PIECE_T) && (allowUpward) && (rtNew == Piece.DIRECTION_UP)) {
			if(!piece.checkCollision(x, y - 1 - check, rtNew, field)) {
				return new WallkickResult(0, -1 - check, rtNew);
			}
		}

		// IOfWallkick
		if( (piece.id == Piece.PIECE_I) && ((rtNew == Piece.DIRECTION_UP) || (rtNew == Piece.DIRECTION_DOWN)) ) {
			for(int i = check; i <= check * 2; i++) {
				int temp = 0;

				if(!piece.checkCollision(x - 1 - i, y, rtNew, field)) {
					temp = -1 - i;
				} else if(!piece.checkCollision(x + 1 + i, y, rtNew, field)) {
					temp = 1 + i;
				} else if(!piece.checkCollision(x + 2 + i, y, rtNew, field)) {
					temp = 2 + i;
				}

				if(temp != 0) {
					return new WallkickResult(temp, 0, rtNew);
				}
			}
		}

		// IFloor kick (Only if you are in contact with the ground)
		if( (piece.id == Piece.PIECE_I) && (allowUpward) && ((rtNew == Piece.DIRECTION_LEFT) || (rtNew == Piece.DIRECTION_RIGHT)) &&
		    (piece.checkCollision(x, y + 1, field) == true) )
		{
			for(int i = check; i <= check * 2; i++) {
				int temp = 0;

				if(!piece.checkCollision(x, y - 1 - i, rtNew, field)) {
					temp = -1 - i;
				} else if(!piece.checkCollision(x, y - 2 - i, rtNew, field)) {
					temp = -2 - i;
				}

				if(temp != 0) {
					return new WallkickResult(0, temp, rtNew);
				}
			}
		}

		return null;
	}
}
