// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.util.ArrayList;

public class FieldColorClear {
	/**
	 * Checks for 4x4 square formations and converts blocks to square blocks if needed.
	 * @return Number of square formations (index 0 is gold, index 1 is silver)
	 */
	public static int[] checkForSquares(Field field) {
		int[] squares = {0,0};

		// Check for gold squares
		for (int i = (field.hidden_height * -1); i < (field.getHeightWithoutHurryupFloor() - 3); i++) {
			for (int j = 0; j < (field.width - 3); j++) {
				// rootBlk is the upper-left square
				Block rootBlk = field.getBlock(j, i);
				boolean squareCheck = false;

				/*
				 * id is the color of the top-left square: if it is a monosquare, every block in the
				 * 4x4 area will have this color.
				 */
				int id = Block.BLOCK_COLOR_NONE;
				if (!(rootBlk == null || rootBlk.isEmpty())) {
					id = rootBlk.color;
				}

				// This can't be a square if rootBlk doesn't exist or is part of another square.
				if (!(rootBlk == null || rootBlk.isEmpty() || rootBlk.isGoldSquareBlock() || rootBlk.isSilverSquareBlock())) {
					// A square is innocent until proven guilty.
					squareCheck = true;
					for (int k = 0; k < 4; k++) {
						for (int l = 0; l < 4; l++) {
							// blk is the current block
							Block blk = field.getBlock(j+l, i+k);
							/*
							 * Reasons why the entire area would not be a monosquare: this block does not exist,
							 * it is part of another square, it has been broken by line clears, is a garbage
							 * block, is not the same color as id, or has connections outside the area.
							 */
							if (blk == null || blk.isEmpty() || blk.isGoldSquareBlock() || blk.isSilverSquareBlock() ||
									blk.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN) ||
									blk.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE) || blk.color != id ||
									(l == 0 && blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) ||
									(l == 3 && blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) ||
									(k == 0 && blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) ||
									(k == 3 && blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))){
								squareCheck = false;
								break;
							}
						}
						if (!squareCheck) {
							break;
						}
					}
				}
				// We found a square! Set all the blocks equal to gold blocks.
				if (squareCheck) {
					squares[0]++;
					int[] squareX = new int[] {0, 1, 1, 2};
					int[] squareY = new int[] {0, 3, 3, 6};
					for (int k = 0; k < 4; k++) {
						for (int l = 0; l < 4; l++) {
							Block blk = field.getBlock(j+l, i+k);
							blk.color = Block.BLOCK_COLOR_SQUARE_GOLD_1 + squareX[l] + squareY[k];
							// For stylistic concerns, we attach all blocks in the square together.
							if (k > 0) {
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
							}
							if (k < 3) {
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
							}
							if (l > 0) {
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
							}
							if (l < 3) {
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
							}
						}
					}
				}
			}
		}
		// Check for silver squares
		for (int i = (field.hidden_height * -1); i < (field.getHeightWithoutHurryupFloor() - 3); i++) {
			for (int j = 0; j < (field.width - 3); j++) {
				Block rootBlk = field.getBlock(j, i);
				boolean squareCheck = false;
				// We don't have to check colors because this loop checks for multisquares.
				if (!(rootBlk == null || rootBlk.isEmpty() || rootBlk.isGoldSquareBlock() || rootBlk.isSilverSquareBlock())) {
					// A square is innocent until proven guilty
					squareCheck = true;
					for (int k = 0; k < 4; k++) {
						for (int l = 0; l < 4; l++) {
							Block blk = field.getBlock(j+l, i+k);
							// See above, but without the color checking.
							if (blk == null || blk.isEmpty() || blk.isGoldSquareBlock() || blk.isSilverSquareBlock() ||
									blk.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN) ||
									blk.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE) ||
									(l == 0 && blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) ||
									(l == 3 && blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) ||
									(k == 0 && blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) ||
									(k == 3 && blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))){
								squareCheck = false;
								break;
							}
						}
						if (!squareCheck) {
							break;
						}
					}
				}
				// We found a square! Set all the blocks equal to silver blocks.
				if (squareCheck) {
					squares[1]++;
					int[] squareX = new int[] {0, 1, 1, 2};
					int[] squareY = new int[] {0, 3, 3, 6};
					for (int k = 0; k < 4; k++) {
						for (int l = 0; l < 4; l++) {
							Block blk = field.getBlock(j+l, i+k);
							blk.color = Block.BLOCK_COLOR_SQUARE_SILVER_1 + squareX[l] + squareY[k];
							if (k > 0) {
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
							}
							if (k < 3) {
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
							}
							if (l > 0) {
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
							}
							if (l < 3) {
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
							}
						}
					}
				}
			}
		}

		return squares;
	}

	/**
	 * Checks the lines that are currently being cleared to see how many strips of squares are present in them.
	 * @return +1 for every 1x4 strip of gold (index 0) or silver (index 1)
	 */
	public static int[] getHowManySquareClears(Field field) {
		int[] squares = {0,0};
		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			// Check the lines we are clearing.
			if (field.getLineFlag(i)) {
				for(int j = 0; j < field.width; j++) {
					Block blk = field.getBlock(j, i);

					// Silver blocks are worth 1, gold are worth 2, but not if they are garbage (avalanche)
					if (blk != null && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE)) {
						if (blk.isGoldSquareBlock()) {
							squares[0]++;
						} else if (blk.isSilverSquareBlock()) {
							squares[1]++;
						}
					}
				}
			}
		}
		// We have to divide the amount by 4 because it's based on 1x4 strips, not single blocks.
		squares[0] /= 4;
		squares[1] /= 4;

		return squares;
	}

	/**
	 * Clear line colors of sufficient size.
	 * @param size Minimum length of line for a clear
	 * @param diagonals <code>true</code> to check diagonals, <code>false</code> to check only vertical and horizontal
	 * @param gemSame <code>true</code> to check gem blocks
	 * @return Total number of blocks cleared.
	 */
	public static int clearLineColor (Field field, int size, boolean diagonals, boolean gemSame)
	{
		int total = 0;
		Block b, bAdj;
		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++)
			for(int j = 0; j < field.width; j++)
			{
				b = field.getBlock(j, i);
				if (b == null)
					continue;
				if (b.getAttribute(Block.BLOCK_ATTRIBUTE_ERASE))
				{
					total++;
					if (b.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))
					{
						bAdj = field.getBlock(j, i+1);
						if (bAdj != null)
						{
							bAdj.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, false);
							bAdj.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
						}
					}
					if (b.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP))
					{
						bAdj = field.getBlock(j, i-1);
						if (bAdj != null)
						{
							bAdj.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, false);
							bAdj.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
						}
					}
					if (b.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT))
					{
						bAdj = field.getBlock(j-1, i);
						if (bAdj != null)
						{
							bAdj.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, false);
							bAdj.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
						}
					}
					if (b.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT))
					{
						bAdj = field.getBlock(j+1, i);
						if (bAdj != null)
						{
							bAdj.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, false);
							bAdj.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
						}
					}
					field.setBlockColor(j, i, Block.BLOCK_COLOR_NONE);
				}
			}
		return total;
	}
	/**
	 * Check for line color clears of sufficient size.
	 * @param size Minimum length of line for a clear
	 * @param flag <code>true</code> to set BLOCK_ATTRIBUTE_ERASE to true on blocks to be cleared.
	 * @param diagonals <code>true</code> to check diagonals, <code>false</code> to check only vertical and horizontal
	 * @param gemSame <code>true</code> to check gem blocks
	 * @return Total number of blocks that would be cleared.
	 */
	public static int checkLineColor (Field field, int size, boolean flag, boolean diagonals, boolean gemSame)
	{
		if (size < 1)
			return 0;
		if (flag)
		{
			field.setAllAttribute(Block.BLOCK_ATTRIBUTE_ERASE, false);
			if (field.lineColorsCleared == null)
				field.lineColorsCleared = new ArrayList<Integer>();
			field.gemsCleared = 0;
		}
		int total = 0;
		int x, y, count, blockColor, lineColor;
		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			for(int j = 0; j < field.width; j++) {
				lineColor = field.getBlockColor(j, i, gemSame);
				if (lineColor == Block.BLOCK_COLOR_NONE || lineColor == Block.BLOCK_COLOR_INVALID)
					continue;
				for (int dir = 0; dir < (diagonals ? 3 : 2); dir++) {
					blockColor = lineColor;
					x = j;
					y = i;
					count = 0;
					while (lineColor == blockColor)
					{
						count++;
						if (dir != 1)
							y++;
						if (dir != 0)
							x++;
						blockColor = field.getBlockColor(x, y, gemSame);
					}
					if (count < size)
						continue;
					total += count;
					if (!flag)
						continue;
					if (count == size)
						field.lineColorsCleared.add(lineColor);
					x = j;
					y = i;
					blockColor = lineColor;
					while (lineColor == blockColor)
					{
						Block b = field.getBlock(x, y);
						if (b.hard > 0)
							b.hard--;
						else if (!b.getAttribute(Block.BLOCK_ATTRIBUTE_ERASE))
						{
							if (b.isGemBlock())
								field.gemsCleared++;
							b.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
						}
						if (dir != 1)
							y++;
						if (dir != 0)
							x++;
						blockColor = field.getBlockColor(x, y, gemSame);
					}
				}
			}
		}
		return total;
	}
	/**
	 * Performs all color clears of sufficient size containing at least one gem block.
	 * @param size Minimum size of cluster for a clear
	 * @param garbageClear <code>true</code> to clear garbage blocks adjacent to cleared clusters
	 * @return Total number of blocks cleared.
	 */
	public static int gemClearColor (Field field, int size, boolean garbageClear, boolean ignoreHidden)
	{
		Field temp = new Field(field);
		int total = 0;
		Block b;

		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			for(int j = 0; j < field.width; j++) {
				b = field.getBlock(j, i);
				if (b == null)
					continue;
				if (!b.isGemBlock())
					continue;
				int clear = temp.clearColor(j, i, false, garbageClear, true, ignoreHidden);
				if (clear >= size)
				{
					total += clear;
					clearColor(field, j, i, false, garbageClear, true, ignoreHidden);
				}
			}
		}
		return total;
	}
	public static int gemClearColor (Field field, int size, boolean garbageClear)
	{
		return gemClearColor(field, size, garbageClear, false);
	}
	/**
	 * Performs all color clears of sufficient size.
	 * @param size Minimum size of cluster for a clear
	 * @param garbageClear <code>true</code> to clear garbage blocks adjacent to cleared clusters
	 * @param gemSame <code>true</code> to check gem blocks
	 * @return Total number of blocks cleared.
	 */
	public static int clearColor (Field field, int size, boolean garbageClear, boolean gemSame, boolean ignoreHidden)
	{
		Field temp = new Field(field);
		int total = 0;
		for(int i = ignoreHidden ? 0 : (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			for(int j = 0; j < field.width; j++) {
				int clear = temp.clearColor(j, i, false, garbageClear, gemSame, ignoreHidden);
				if (clear >= size)
				{
					total += clear;
					clearColor(field, j, i, false, garbageClear, gemSame, ignoreHidden);
				}
			}
		}
		return total;
	}
	public static int clearColor (Field field, int size, boolean garbageClear, boolean gemSame)
	{
		return clearColor(field, size, garbageClear, gemSame, false);
	}
	/**
	 * Clears the block at the given position as well as all adjacent blocks of
	 * the same color, and any garbage blocks adjacent to the group if garbageClear is true.
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param flag <code>true</code> to set BLOCK_ATTRIBUTE_ERASE to true on cleared blocks.
	 * @param garbageClear <code>true</code> to clear garbage blocks adjacent to cleared clusters
	 * @param gemSame <code>true</code> to check gem blocks
	 * @return The number of blocks cleared.
	 */
	public static int clearColor (Field field, int x, int y, boolean flag, boolean garbageClear, boolean gemSame,
			boolean ignoreHidden)
	{
		int blockColor = field.getBlockColor(x, y, gemSame);
		if (blockColor == Block.BLOCK_COLOR_NONE || blockColor == Block.BLOCK_COLOR_INVALID)
			return 0;
		Block b = field.getBlock(x, y);
		if (b == null)
			return 0;
		if (b.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE))
			return 0;
		else
			return clearColor(field, x, y, blockColor, flag, garbageClear, gemSame, ignoreHidden);
	}
	/**
	 * Note: This method is private because calling it with a targetColor parameter
	 *       of BLOCK_COLOR_NONE or BLOCK_COLOR_INVALID may cause an infinite loop
	 *       and crash the game. This check is handled by the above public method
	 *       so as to avoid redundant checks.
	 */
	private static int clearColor (Field field, int x, int y, int targetColor, boolean flag, boolean garbageClear,
			boolean gemSame, boolean ignoreHidden)
	{
		if (ignoreHidden && y < 0)
			return 0;
		int blockColor = field.getBlockColor(x, y, gemSame);
		if (blockColor == Block.BLOCK_COLOR_INVALID)
			return 0;
		Block b = field.getBlock(x, y);
		if (flag && b.getAttribute(Block.BLOCK_ATTRIBUTE_ERASE))
			return 0;
		if (garbageClear && b.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE)
				 && !b.getAttribute(Block.BLOCK_ATTRIBUTE_WALL))
		{
			if (flag)
			{
				b.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
				field.garbageCleared++;
			}
			else if (b.hard > 0)
				b.hard--;
			else
				field.setBlockColor(x, y, Block.BLOCK_COLOR_NONE);
		}
		if (blockColor != targetColor)
			return 0;
		if (flag)
			b.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		else if (b.hard > 0)
			b.hard--;
		else
			field.setBlockColor(x, y, Block.BLOCK_COLOR_NONE);
		return 1 + clearColor(field, x+1, y, targetColor, flag, garbageClear, gemSame, ignoreHidden)
				 + clearColor(field, x-1, y, targetColor, flag, garbageClear, gemSame, ignoreHidden)
				 + clearColor(field, x, y+1, targetColor, flag, garbageClear, gemSame, ignoreHidden)
				 + clearColor(field, x, y-1, targetColor, flag, garbageClear, gemSame, ignoreHidden);
	}

	/**
	 * Clears all blocks of the same color
	 * @param targetColor The color to clear
	 * @param flag <code>true</code> to set BLOCK_ATTRIBUTE_ERASE to true on cleared blocks.
	 * @param gemSame <code>true</code> to check gem blocks
	 * @return The number of blocks cleared.
	 */
	public static int allClearColor (Field field, int targetColor, boolean flag, boolean gemSame)
	{
		if (targetColor < 0)
			return 0;
		if (gemSame)
			targetColor = Block.gemToNormalColor(targetColor);
		int total = 0;
		for (int y = (-1 * field.hidden_height); y < field.height; y++)
			for (int x = 0; x < field.width; x++)
				if (field.getBlockColor(x, y, gemSame) == targetColor)
				{
					total++;
					if (flag)
						field.getBlock(x, y).setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
					else
						field.setBlockColor(x, y, Block.BLOCK_COLOR_NONE);
				}
		return total;
	}
}
