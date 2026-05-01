// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.wallkick;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * Wallkick system interface
 */
public interface Wallkick {
	/**
	 * Execute a wallkick
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rtDir Rotation button used (-1: left rotation, 1: right rotation, 2: 180-degree rotation)
	 * @param rtOld Direction before rotation
	 * @param rtNew Direction after rotation
	 * @param allowUpward If true, upward wallkicks are allowed.
	 * @param piece Current piece
	 * @param field Current field
	 * @param ctrl Button input status (it may be null, when controlled by an AI)
	 * @return WallkickResult object, or null if you don't want a kick
	 */
	public WallkickResult executeWallkick(int x, int y, int rtDir, int rtOld, int rtNew, boolean allowUpward, Piece piece, Field field, Controller ctrl);
}
