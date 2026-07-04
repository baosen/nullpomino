// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.util.Random;

import nullpomino.game.play.GameEngine;
import nullpomino.util.JdkRandom;

public class FieldGarbage {
	public static void garbageDrop(Field field, GameEngine engine, int drop, boolean big) {
		field.garbageDrop(engine, drop, big, 0, 0, -1, Block.BLOCK_COLOR_GRAY);
	}
	public static void garbageDrop(Field field, GameEngine engine, int drop, boolean big, int hard) {
		field.garbageDrop(engine, drop, big, hard, 0, -1, Block.BLOCK_COLOR_GRAY);
	}
	public static void garbageDrop(Field field, GameEngine engine, int drop, boolean big, int hard, int countdown) {
		field.garbageDrop(engine, drop, big, hard, countdown, -1, Block.BLOCK_COLOR_GRAY);
	}
	public static void garbageDrop(Field field, GameEngine engine, int drop, boolean big, int hard, int countdown, int avoidColumn) {
		field.garbageDrop(engine, drop, big, hard, countdown, avoidColumn, Block.BLOCK_COLOR_GRAY);
	}
	public static void garbageDrop(Field field, GameEngine engine, int drop, boolean big, int hard, int countdown, int avoidColumn, int color) {
		int y = -1 * field.hidden_height;
		int actualWidth = field.width;
		if (big)
			actualWidth >>= 1;
		int bigMove = big ? 2 : 1;
		while (drop >= actualWidth)
		{
			drop -= actualWidth;
			for (int x = 0; x < actualWidth; x+=bigMove)
				field.garbageDropPlace(x, y, big, hard, color, countdown);
			y+=bigMove;
		}
		if (drop == 0)
			return;
		boolean[] placeBlock = new boolean[actualWidth];
		int j;
		if (drop > (actualWidth>>1))
		{
			for (int x = 0; x < actualWidth; x++)
				placeBlock[x] = true;
			int start = actualWidth;
			if (avoidColumn >= 0 && avoidColumn < actualWidth)
			{
				start--;
				placeBlock[avoidColumn] = false;
			}
			for (int i = start; i > drop; i--)
			{
				do {
					j = engine.random.nextInt(actualWidth);
				} while (!placeBlock[j]);
				placeBlock[j] = false;
			}
		}
		else
		{
			for (int x = 0; x < actualWidth; x++)
				placeBlock[x] = false;
			for (int i = 0; i < drop; i++)
			{
				do {
					j = engine.random.nextInt(actualWidth);
				} while (placeBlock[j] && j != avoidColumn);
				placeBlock[j] = true;
			}
		}

		for (int x = 0; x < actualWidth; x++)
			if (placeBlock[x])
				field.garbageDropPlace(x*bigMove, y, big, hard, color, countdown);
	}

	public static boolean garbageDropPlace (Field field, int x, int y, boolean big, int hard)
	{
		return field.garbageDropPlace(x, y, big, hard, Block.BLOCK_COLOR_GRAY, 0);
	}
	public static boolean garbageDropPlace (Field field, int x, int y, boolean big, int hard, int color)
	{
		return field.garbageDropPlace(x, y, big, hard, color, 0);
	}
	public static boolean garbageDropPlace (Field field, int x, int y, boolean big, int hard, int color, int countdown)
	{
		Block b = field.getBlock(x, y);
		if (b == null)
			return false;
		if (big)
		{
			field.garbageDropPlace(x+1, y, false, hard);
			field.garbageDropPlace(x, y+1, false, hard);
			field.garbageDropPlace(x+1, y+1, false, hard);
		}
		if (field.getBlockEmptyF(x, y))
		{
			field.setBlockColor(x, y, color);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY, false);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, false);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, false);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, false);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, false);
			b.hard = hard;
			b.secondaryColor = 0;
			b.countdown = countdown;
			return true;
		}
		return false;
	}

	public static void addRandomHoverBlocks(Field field, GameEngine engine, int count, int[] colors, int minY,
			boolean avoidLines)
	{
		field.addRandomHoverBlocks(engine, count, colors, minY, avoidLines, false);
	}

	public static void addRandomHoverBlocks(Field field, GameEngine engine, int count, int[] colors, int minY,
			boolean avoidLines, boolean flashMode)
	{
		Random posRand = new JdkRandom(engine.random.nextLong());
		Random colorRand = new JdkRandom(engine.random.nextLong());
		int placeHeight = field.height-minY;
		int placeSize = placeHeight * field.width;
		boolean[][] placeBlock = new boolean[field.width][placeHeight];
		int[] colorCounts = new int[colors.length];
		for (int i = 0; i < colorCounts.length; i++)
			colorCounts[i] = 0;

		int blockColor;
		if (count < (placeSize >> 1))
		{
			int colorShift = colorRand.nextInt(colors.length);
			int x, y;
			for (y = 0; y < placeHeight; y++)
				for (x = 0; x < field.width; x++)
					placeBlock[x][y] = false;
			for (int i = 0; i < count; i++)
			{
				x = posRand.nextInt(field.width);
				y = posRand.nextInt(placeHeight);
				if (!field.getBlockEmpty(x, y+minY))
					i--;
				else
				{
					blockColor = ((i+colorShift)%colors.length);
					colorCounts[blockColor]++;
					field.addHoverBlock(x, y+minY, colors[blockColor]);
					placeBlock[x][y] = true;
				}
			}
		}
		else
		{
			int x, y;
			for (y = 0; y < placeHeight; y++)
				for (x = 0; x < field.width; x++)
					placeBlock[x][y] = true;
			for (int i = placeSize; i > count; i--)
			{
				x = posRand.nextInt(field.width);
				y = posRand.nextInt(placeHeight);
				if (placeBlock[x][y])
					placeBlock[x][y] = false;
				else
					i++;
			}
			for (y = 0; y < placeHeight; y++)
				for (x = 0; x < field.width; x++)
					if (placeBlock[x][y])
					{
						blockColor = colorRand.nextInt(colors.length);
						colorCounts[blockColor]++;
						field.addHoverBlock(x, y+minY, colors[blockColor]);
					}
		}
		if (!avoidLines || colors.length == 1)
			return;
		int colorUp, colorLeft, cIndex;
		for (int y = minY; y < field.height; y++)
			for (int x = 0; x < field.width; x++)
				if (placeBlock[x][y-minY])
				{
					colorUp = field.getBlockColor(x, y-2);
					colorLeft = field.getBlockColor(x-2, y);
					blockColor = field.getBlockColor(x, y);
					if (blockColor != colorUp && blockColor != colorLeft)
						continue;

					cIndex = -1;
					for (int i = 0; i < colorCounts.length; i++)
						if (colors[i] == blockColor)
						{
							cIndex = i;
							break;
						}

					if (colors.length == 2)
					{
						if ((colors[0] == colorUp && colors[1] != colorLeft) ||
								(colors[0] == colorLeft && colors[1] != colorUp))
						{
							colorCounts[1]++;
							colorCounts[cIndex]--;
							field.setBlockColor(x, y, colors[1]);
						}
						else if ((colors[1] == colorUp && colors[0] != colorLeft) ||
								(colors[1] == colorLeft && colors[0] != colorUp))
						{
							colorCounts[0]++;
							colorCounts[cIndex]--;
							field.setBlockColor(x, y, colors[0]);
						}
					}
					else
					{
						int newColor;
						do {
							newColor = colorRand.nextInt(colors.length);
						} while (colors[newColor] == colorUp || colors[newColor] == colorLeft);
						colorCounts[cIndex]--;
						colorCounts[newColor]++;
						field.setBlockColor(x, y, colors[newColor]);
					}
				}
		boolean[] canSwitch = new boolean[colors.length];
		int minCount = count/colors.length;
		int maxCount = (count+colors.length-1)/colors.length;
		boolean done = true;
		for (int i = 0; i < colorCounts.length; i++)
			if (colorCounts[i] > maxCount)
			{
				done = false;
				break;
			}
		int colorSide, bestSwitch, bestSwitchCount;
		int excess = 0;
		boolean fill = false;
		while (!done)
		{
			done = true;
			for (int y = minY; y < field.height; y++)
				for (int x = 0; x < field.width; x++)
				{
					blockColor = field.getBlockColor(x, y);
					fill = blockColor == Block.BLOCK_COLOR_NONE;
					cIndex = -1;
					if (!fill)
					{
						if (!placeBlock[x][y-minY])
							continue;
						for (int i = 0; i < colorCounts.length; i++)
							if (colors[i] == blockColor)
							{
								cIndex = i;
								break;
							}
						if (cIndex == -1)
							continue;
						if (colorCounts[cIndex] <= maxCount)
							continue;
					}
					for (int i = 0; i < colorCounts.length; i++)
						canSwitch[i] = colorCounts[i] < maxCount;

					colorSide = field.getBlockColor(x, y-2);
					for (int i = 0; i < colors.length; i++)
						if (colors[i] == colorSide)
						{
							canSwitch[i] = false;
							break;
						}
					colorSide = field.getBlockColor(x, y+2);
					for (int i = 0; i < colors.length; i++)
						if (colors[i] == colorSide)
						{
							canSwitch[i] = false;
							break;
						}
					colorSide = field.getBlockColor(x-2, y);
					for (int i = 0; i < colors.length; i++)
						if (colors[i] == colorSide)
						{
							canSwitch[i] = false;
							break;
						}
					colorSide = field.getBlockColor(x+2, y);
					for (int i = 0; i < colors.length; i++)
						if (colors[i] == colorSide)
						{
							canSwitch[i] = false;
							break;
						}
					bestSwitch = -1;
					bestSwitchCount = Integer.MAX_VALUE;
					for (int i = 0; i < colorCounts.length; i++)
						if (canSwitch[i] && colorCounts[i] < bestSwitchCount)
						{
							bestSwitch = i;
							bestSwitchCount = colorCounts[i];
						}
					if (bestSwitch != -1)
					{
						if (fill)
						{
							excess++;
							field.addHoverBlock(x, y, colors[bestSwitch]);
							placeBlock[x][y-minY] = true;
						}
						else
						{
							colorCounts[cIndex]--;
							field.setBlockColor(x, y, colors[bestSwitch]);
						}
						colorCounts[bestSwitch]++;
						done = false;
					}
				}
			while (excess > 0)
			{
				int x = posRand.nextInt(field.width);
				int y = posRand.nextInt(placeHeight)+minY;
				if (!placeBlock[x][y-minY])
					continue;
				blockColor = field.getBlockColor(x, y);
				for (int i = 0; i < colors.length; i++)
					if (colors[i] == blockColor)
					{
						if (colorCounts[i] > minCount)
						{
							field.setBlockColor(x, y, Block.BLOCK_COLOR_NONE);
							colorCounts[i]--;
							excess--;
							placeBlock[x][y-minY] = false;
						}
						break;
					}
			}
			boolean balanced = true;
			for (int i = 0; i < colorCounts.length; i++)
				if (colorCounts[i] > maxCount)
				{
					balanced = false;
					break;
				}
			if (balanced)
				done = true;
		}
		if (!flashMode)
			return;
		done = true;
		boolean[] gemNeeded = new boolean[colors.length];
		for (int i = 0; i < colors.length; i++)
		{
			if (colors[i] >= 2 && colors[i] <= 8 && colorCounts[i] > 0)
			{
				gemNeeded[i] = true;
				done = false;
			}
			else
				gemNeeded[i] = false;
		}
		while (!done)
		{
			int x = posRand.nextInt(field.width);
			int y = posRand.nextInt(placeHeight)+minY;
			if (!placeBlock[x][y-minY])
				continue;
			blockColor = field.getBlockColor(x, y);
			for (int i = 0; i < colors.length; i++)
				if (colors[i] == blockColor)
				{
					if (gemNeeded[i])
					{
						field.setBlockColor(x, y, blockColor+7);
						gemNeeded[i] = false;
					}
					break;
				}
			done = true;
			for (int i = 0; i < colors.length; i++)
				if (gemNeeded[i])
					done = false;
		}
	}
	public static boolean addHoverBlock(Field field, int x, int y, int color)
	{
		Block b = field.getBlock(x, y);
		if (b == null)
			return false;
		b.color = color;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY, true);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, false);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, false);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, false);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, false);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, false);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, false);
		return true;
	}

	public static void shuffleColors(Field field, int[] blockColors, int numColors, Random rand) {
		blockColors = blockColors.clone();
		int maxX = Math.min(blockColors.length, numColors);
		int temp, j;
		int i = maxX;
		while (i > 1)
		{
			j = rand.nextInt(i);
			i--;
			if (j != i)
			{
				temp = blockColors[i];
				blockColors[i] = blockColors[j];
				blockColors[j] = temp;
			}
		}
		for (int x = 0; x < field.width; x++)
			for (int y = 0; y < field.height; y++)
			{
				temp = field.getBlockColor(x, y)-1;
				if (numColors == 3 && temp >= 3)
					temp--;
				if (temp >= 0 && temp < maxX)
					field.setBlockColor(x, y, blockColors[temp]);
			}
	}
}
