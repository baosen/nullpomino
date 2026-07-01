// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

/**
 * Read-only query/analysis helpers extracted from {@link Field}.
 */
public class FieldAnalysis {
	private static final int[][] T_SPIN_CORNERS = {
			{0, 0}, {2, 0}, {0, 2}, {2, 2}
	};

	private static final int[][] BIG_T_SPIN_CORNERS = {
			{1, 1}, {4, 1}, {1, 4}, {4, 4}
	};

	/**
	 * T-SpinIf it was the terrain to betrue
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param big BigWhether
	 * @return T-SpinIf it was the terrain to betrue
	 */
	public static boolean isTSpinSpot(Field field, int x, int y, boolean big) {
		return countFilledTCorners(field, x, y, big) >= 3;
	}

	/**
	 * T-SpinIf it was a hole I cantrue
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param big BigWhether
	 * @return T-SpinIf it was a hole I cantrue
	 */
	public static boolean isTSlot(Field field, int x, int y, boolean big) {
		// I wonder if the central buried
		if(big == true) {
			if(!field.getBlockEmptyF(x + 2, y + 2)) {
				return false;
			}
		} else {
			//□ □ □ ※ ※ ※ □ □ □ □
			//□ □ □ ★ ※ □ □ □ □ □
			//□ □ □ ※ ※ ※ □ □ □ □
			//□ □ □ ○ ※ ○ □ □ □ □

			if(!field.getBlockEmptyF(x + 1, y + 0)) return false;
			if(!field.getBlockEmptyF(x + 1, y + 1)) return false;
			if(!field.getBlockEmptyF(x + 1, y + 2)) return false;

			if(!field.getBlockEmptyF(x + 0, y + 1)) return false;
			if(!field.getBlockEmptyF(x + 2, y + 1)) return false;

			if(!field.getBlockEmptyF(x + 1, y - 1)) return false;
		}

		return countFilledTCorners(field, x, y, big) == 3;
	}

	private static int countFilledTCorners(Field field, int x, int y, boolean big) {
		int count = 0;
		for(int[] corner : big ? BIG_T_SPIN_CORNERS : T_SPIN_CORNERS) {
			if(field.getBlockColor(x + corner[0], y + corner[1]) != Block.BLOCK_COLOR_NONE) {
				count++;
			}
		}
		return count;
	}

	/**
	 * T-SpinI disappearLinescountReturns
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param big BigWhether(Not supported)
	 * @return T-SpinI disappearLinescount(T-SpinFor example, if not the0)
	 */
	public static int getTSlotLineClear(Field field, int x, int y, boolean big) {
		if(!isTSlot(field, x, y, big)) return 0;

		boolean[] lineflag = new boolean[2];
		lineflag[0] = lineflag[1] = true;

		for(int j = 0; j < field.width; j++) {
			for(int i = 0; i < 2; i++) {
				//■ ■ ■ ★ ※ ■ ■ ■ ■ ■
				//□ □ □ ※ ※ ※ □ □ □ □
				//□ □ □ ○ ※ ○ □ □ □ □
				if((j < x) || (j >= x + 3)) {
					if(field.getBlockEmptyF(j, y + 1 + i) == true) {
						lineflag[i] = false;
					}
				}
			}
		}

		int lines = 0;
		for(int i = 0; i < lineflag.length; i++) {
			if(lineflag[i]) lines++;
		}

		return lines;
	}

	/**
	 * T-SpinI disappearLinescountReturns(fieldWhole)
	 * @param big BigWhether(Not supported)
	 * @return T-SpinI disappearLinescount(T-SpinFor example, if not the0)
	 */
	public static int getTSlotLineClearAll(Field field, boolean big) {
		int result = 0;

		for(int j = 0; j < field.width; j++) {
			for(int i = 0; i < field.getHeightWithoutHurryupFloor() - 2; i++) {
				if(field.getLineFlag(i) == false) {
					result += getTSlotLineClear(field, j, i, big);
				}
			}
		}

		return result;
	}

	/**
	 * T-SpinI disappearLinescountReturns(fieldWhole)
	 * @param big BigWhether(Not supported)
	 * @param minimum LowestLinescount(2When youT-Spin DoubleOnly sensitive to the)
	 * @return T-SpinI disappearLinescount(T-SpinOr if it is notminimumI do not meet theLinesEtc.0)
	 */
	public static int getTSlotLineClearAll(Field field, boolean big, int minimum) {
		int result = 0;

		for(int j = 0; j < field.width; j++) {
			for(int i = 0; i < field.getHeightWithoutHurryupFloor() - 2; i++) {
				if(field.getLineFlag(i) == false) {
					int temp = getTSlotLineClear(field, j, i, big);

					if(temp >= minimum)
						result += temp;
				}
			}
		}

		return result;
	}

	/**
	 * fieldWhat in the number ofBlockExamine whether there is
	 * @return fieldAre withinBlockOfcount
	 */
	public static int getHowManyBlocks(Field field) {
		int count = 0;

		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			if(field.getLineFlag(i) == false) {
				for(int j = 0; j < field.width; j++) {
					if(!field.getBlockEmpty(j, i)) {
						count++;
					}
				}
			}
		}

		return count;
	}

	/**
	 * How many of the left-BlockI examine whether the side-by-side
	 * @return Are arranged from leftBlockThe totalcount
	 */
	public static int getHowManyBlocksFromLeft(Field field) {
		int count = 0;

		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			if(field.getLineFlag(i) == false) {
				for(int j = 0; j < field.width; j++) {
					if(!field.getBlockEmpty(j, i)) {
						count++;
					} else {
						break;
					}
				}
			}
		}

		return count;
	}

	/**
	 * How many of the rightBlockI examine whether the side-by-side
	 * @return Side-by-side from the rightBlockThe totalcount
	 */
	public static int getHowManyBlocksFromRight(Field field) {
		int count = 0;

		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			if(field.getLineFlag(i) == false) {
				for(int j = field.width - 1; j > 0; j--) {
					if(!field.getBlockEmpty(j, i)) {
						count++;
					} else {
						break;
					}
				}
			}
		}

		return count;
	}

	/**
	 * At the topBlockOfY-coordinateGet the
	 * @return At the topBlockOfY-coordinate
	 */
	public static int getHighestBlockY(Field field) {
		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			if(field.getLineFlag(i) == false) {
				for(int j = 0; j < field.width; j++) {
					if(!field.getBlockEmpty(j, i)) return i;
				}
			}
		}

		return field.height;
	}

	/**
	 * At the topBlockOfY-coordinateGet the (X-coordinateWe can specifyVersion)
	 * @param x X-coordinate
	 * @return At the topBlockOfY-coordinate
	 */
	public static int getHighestBlockY(Field field, int x) {
		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			if(field.getLineFlag(i) == false) {
				if(!field.getBlockEmpty(x, i)) return i;
			}
		}

		return field.height;
	}

	/**
	 * I will see if there is a gap under the specified coordinates
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return If there is a gap under the specified coordinatestrue
	 */
	public static boolean isHoleBelow(Field field, int x, int y) {
		if(!field.getBlockEmpty(x, y) && field.getBlockEmpty(x, y + 1)) return true;
		return false;
	}

	/**
	 * fieldThe gap in thecountExamine the
	 * @return fieldThe gap in thecount
	 */
	public static int getHowManyHoles(Field field) {
		int hole = 0;
		boolean samehole = false;

		for(int j = 0; j < field.width; j++) {
			samehole = false;

			for(int i = getHighestBlockY(field); i < field.getHeightWithoutHurryupFloor(); i++) {
				if(field.getLineFlag(i) == false) {
					if(isHoleBelow(field, j, i)) {
						samehole = true;
					} else if(samehole && field.getBlockEmpty(j, i)) {
						hole++;
					} else {
						samehole = false;
					}
				}
			}
		}

		return hole;
	}

	/**
	 * Many pieces on top of the gapBlockI find out that you have stacked
	 * @return Are stackedBlockOfcount
	 */
	public static int getHowManyLidAboveHoles(Field field) {
		int blocks = 0;

		for(int j = 0; j < field.width; j++) {
			int count = 0;

			for(int i = getHighestBlockY(field); i < field.getHeightWithoutHurryupFloor() - 1; i++) {
				if(field.getLineFlag(i) == false) {
					if(isHoleBelow(field, j, i)) {
						count++;
						blocks += count;
						count = 0;
					} else if(!field.getBlockEmpty(j, i)) {
						count++;
					}
				}
			}
		}

		return blocks;
	}

	/**
	 * Every valley (■ ■ which returns the total depth of the) terrain that is (The return value is large enough to have many deep valleys)
	 * @return I shall be the sum of the depth of the valley all
	 */
	public static int getTotalValleyDepth(Field field) {
		int depth = 0;

		for(int j = 0; j < field.width; j++) {
			int d = getValleyDepth(field, j);
			if(d >= 2) depth += d;
		}

		return depth;
	}

	/**
	 * IValley type is required (Depth3Or more)countReturns
	 * @return IValley type is requiredcount
	 */
	public static int getTotalValleyNeedIPiece(Field field) {
		int count = 0;

		for(int j = 0; j < field.width; j++) {
			if(getValleyDepth(field, j) >= 3) count++;
		}

		return count;
	}

	/**
	 * Valley (■ ■ examine the depth of the) terrain that is
	 * @param x ExamineX-coordinate
	 * @return The depth of the valley (If you do not have0)
	 */
	public static int getValleyDepth(Field field, int x) {
		int depth = 0;

		int highest = getHighestBlockY(field, x - 1);
		highest = Math.min(highest, getHighestBlockY(field, x));
		highest = Math.min(highest, getHighestBlockY(field, x + 1));

		for(int i = highest; i < field.getHeightWithoutHurryupFloor(); i++) {
			if(field.getLineFlag(i) == false) {
				if( (!field.getBlockEmptyF(x - 1, i) || (x <= 0)) && field.getBlockEmptyF(x, i) && (!field.getBlockEmptyF(x + 1, i) || (x >= field.width - 1)) )
					depth++;
			}
		}

		return depth;
	}
}
