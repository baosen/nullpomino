// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.subsystem.wallkick;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * WallOnlyWallkick - fieldI will not only kick wallsWallkick(Already been placedBlockI do not kick)
 */
public class WallOnlyWallkick implements Wallkick {
	/*
	 * Wallkick
	 */
	public WallkickResult executeWallkick(int x, int y, int rtDir, int rtOld, int rtNew, boolean allowUpward, Piece piece, Field field, Controller ctrl) {
		int check = 0;
		if(piece.big) check = 1;

		// NormalWallkick (IOther)
		if(piece.id != Piece.PIECE_I) {
			if(checkCollisionKick(piece, x, y, rtNew, field)) {
				int temp = 0;

				if(!piece.checkCollision(x - 1 - check, y, rtNew, field)) temp = -1 - check;
				if(!piece.checkCollision(x + 1 + check, y, rtNew, field)) temp = 1 + check;

				if(temp != 0) {
					return new WallkickResult(temp, 0, rtNew);
				}
			}
		}

		return null;
	}

	/**
	 * WallkickIt is possible to examine whether
	 * @param piece BlockPeace
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return WallkickIf possibletrue
	 */
	private boolean checkCollisionKick(Piece piece, int x, int y, int rt, Field fld) {
		// BigThe only treatment
		if(piece.big == true) return checkCollisionKickBig(piece, x, y, rt, fld);

		for(int i = 0; i < piece.getMaxBlock(); i++) {
			int x2 = x + piece.dataX[rt][i];
			int y2 = y + piece.dataY[rt][i];

			if(fld.getCoordAttribute(x2, y2) == Field.COORD_WALL) {
				return true;
			}
		}

		return false;
	}

	/**
	 * WallkickIt is possible to examine whether (BigFor)
	 * @param piece BlockPeace
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return WallkickIf possibletrue
	 */
	private boolean checkCollisionKickBig(Piece piece, int x, int y, int rt, Field fld) {
		for(int i = 0; i < piece.getMaxBlock(); i++) {
			int x2 = (x + piece.dataX[rt][i] * 2);
			int y2 = (y + piece.dataY[rt][i] * 2);

			// 4BlockMinutes to examine
			for(int k = 0; k < 2; k++)for(int l = 0; l < 2; l++) {
				int x3 = x2 + k;
				int y3 = y2 + l;

				if(fld.getCoordAttribute(x3, y3) == Field.COORD_WALL) {
					return true;
				}
			}
		}

		return false;
	}
}
