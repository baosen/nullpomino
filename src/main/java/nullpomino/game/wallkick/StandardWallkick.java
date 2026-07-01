// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.wallkick;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;

/**
 * SRS
 */
public class StandardWallkick extends BaseStandardWallkick {
	// Wallkick data
	private static final int WALLKICK_I_L[][][] =
	{
		{{-1, 0},{ 2, 0},{-1,-2},{ 2, 1}},	// 0>>3
		{{ 2, 0},{-1, 0},{ 2,-1},{-1, 2}},	// 1>>0
		{{ 1, 0},{-2, 0},{ 1, 2},{-2,-1}},	// 2>>1
		{{-2, 0},{ 1, 0},{-2, 1},{ 1,-2}},	// 3>>2
	};
	private static final int WALLKICK_I_R[][][] =
	{
		{{-2, 0},{ 1, 0},{-2, 1},{ 1,-2}},	// 0>>1
		{{-1, 0},{ 2, 0},{-1,-2},{ 2, 1}},	// 1>>2
		{{ 2, 0},{-1, 0},{ 2,-1},{-1, 2}},	// 2>>3
		{{ 1, 0},{-2, 0},{ 1, 2},{-2,-1}},	// 3>>0
	};

	// 180-degree rotation wallkick data
	private static final int WALLKICK_NORMAL_180[][][] =
	{
		{{ 1, 0},{ 2, 0},{ 1, 1},{ 2, 1},{-1, 0},{-2, 0},{-1, 1},{-2, 1},{ 0,-1},{ 3, 0},{-3, 0}},	// 0>>2─ ┐
		{{ 0, 1},{ 0, 2},{-1, 1},{-1, 2},{ 0,-1},{ 0,-2},{-1,-1},{-1,-2},{ 1, 0},{ 0, 3},{ 0,-3}},	// 1>>3─ ┼ ┐
		{{-1, 0},{-2, 0},{-1,-1},{-2,-1},{ 1, 0},{ 2, 0},{ 1,-1},{ 2,-1},{ 0, 1},{-3, 0},{ 3, 0}},	// 2>>0─ ┘ │
		{{ 0, 1},{ 0, 2},{ 1, 1},{ 1, 2},{ 0,-1},{ 0,-2},{ 1,-1},{ 1,-2},{-1, 0},{ 0, 3},{ 0,-3}},	// 3>>1─ ─ ┘
	};
	private static final int WALLKICK_I_180[][][] =
	{
		{{-1, 0},{-2, 0},{ 1, 0},{ 2, 0},{ 0, 1}},													// 0>>2─ ┐
		{{ 0, 1},{ 0, 2},{ 0,-1},{ 0,-2},{-1, 0}},													// 1>>3─ ┼ ┐
		{{ 1, 0},{ 2, 0},{-1, 0},{-2, 0},{ 0,-1}},													// 2>>0─ ┘ │
		{{ 0, 1},{ 0, 2},{ 0,-1},{ 0,-2},{ 1, 0}},													// 3>>1─ ─ ┘
	};

	/*
	 * Get kick table
	 */
	@Override
	protected int[][][] getKickTable(int x, int y, int rtDir, int rtOld, int rtNew, boolean allowUpward, Piece piece, Field field, Controller ctrl) {
		return selectKickTable(rtDir, piece.id, WALLKICK_I_L, WALLKICK_I_R, WALLKICK_NORMAL_180, WALLKICK_I_180);
	}
}
