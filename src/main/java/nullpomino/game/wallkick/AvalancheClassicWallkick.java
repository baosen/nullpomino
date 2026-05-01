// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.wallkick;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * AvalancheWallkick
 */
public class AvalancheClassicWallkick implements Wallkick {
	/*
	 * Wallkick
	 */
	public WallkickResult executeWallkick(int x, int y, int rtDir, int rtOld, int rtNew, boolean allowUpward, Piece piece, Field field, Controller ctrl) {
		int check = 1;
		if(piece.big) check = 2;

		if(!piece.checkCollision(x, y, rtNew, field))
			return null;
		switch(rtNew)
		{
			case Piece.DIRECTION_UP:
				if(!piece.checkCollision(x - check, y, rtNew, field))
					return new WallkickResult(-1*check, 0, rtNew);
			break;
			case Piece.DIRECTION_RIGHT:
				if(!piece.checkCollision(x, y - check, rtNew, field))
					return new WallkickResult(0, -1*check, rtNew);
			break;
			case Piece.DIRECTION_DOWN:
				if(!piece.checkCollision(x + check, y, rtNew, field))
					return new WallkickResult(check, 0, rtNew);
			break;
			case Piece.DIRECTION_LEFT:
				if(!piece.checkCollision(x, y + check, rtNew, field))
					return new WallkickResult(0, check, rtNew);
			break;
		}
		return null;
	}
}
