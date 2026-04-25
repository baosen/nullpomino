// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.subsystem.wallkick;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * DTET Wallkick - An extension of the Classic Wallkick system for DRS by Zircean
 */
public class DTETWallkick implements Wallkick {
	/** Wallkick table */
	private static final int[][] WALLKICK = new int[][] {{-1,0},{1,0},{0,1},{-1,1},{1,1}};

	/*
	 * Wallkick main method
	 */
	public WallkickResult executeWallkick(int x, int y, int rtDir, int rtOld, int rtNew, boolean allowUpward, Piece piece, Field field, Controller ctrl) {
		int x2, y2;

		for (int i = 0; i < WALLKICK.length; i++)
		{
			if (rtDir < 0 || rtDir == 2)
			{
				x2 = WALLKICK[i][0];
			}
			else
			{
				x2 = -WALLKICK[i][0];
			}
			y2 = WALLKICK[i][1];

			if (piece.big)
			{
				x2 *= 2; y2 *= 2;
			}

			if(piece.checkCollision(x + x2, y + y2, rtNew, field) == false) {
				return new WallkickResult(x2, y2, rtNew);
			}
		}
		return null;
	}
}
