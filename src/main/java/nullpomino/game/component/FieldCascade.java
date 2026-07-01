// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import nullpomino.game.play.GameEngine;

public class FieldCascade {
	public static boolean doCascadeGravity(Field field, GameEngine.LineGravity type) {
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_LAST_COMMIT, false);
		if (type == GameEngine.LineGravity.CASCADE_SLOW)
			return doCascadeSlow(field);
		else
			return doCascadeGravity(field);
	}

	/**
	 * Main routine for cascade gravity.
	 * @return <code>true</code> if something falls. <code>false</code> if nothing falls.
	 */
	public static boolean doCascadeGravity(Field field) {
		boolean result = false;

		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL, false);

		for(int i = (field.getHeightWithoutHurryupFloor() - 1); i >= (field.hidden_height * -1); i--) {
			for(int j = 0; j < field.width; j++) {
				Block blk = field.getBlock(j, i);

				if((blk != null) && !blk.isEmpty() && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY)) {
					boolean fall = true;
					field.checkBlockLink(j, i);

					for(int k = (field.getHeightWithoutHurryupFloor() - 1); k >= (field.hidden_height * -1); k--) {
						for(int l = 0; l < field.width; l++) {
							Block bTemp = field.getBlock(l, k);

							if( (bTemp != null) && !bTemp.isEmpty() &&
								bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK) && !bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL) )
							{
								Block bBelow = field.getBlock(l, k + 1);

								if( (field.getCoordAttribute(l, k + 1) == Field.COORD_WALL) ||
									((bBelow != null) && !bBelow.isEmpty() && !bBelow.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK))
								  )
								{
									fall = false;
								}
							}
						}
					}

					if(fall) {
						result = true;
						for(int k = (field.getHeightWithoutHurryupFloor() - 1); k >= (field.hidden_height * -1); k--) {
							for(int l = 0; l < field.width; l++) {
								Block bTemp = field.getBlock(l, k);
								Block bBelow = field.getBlock(l, k + 1);

								if( (field.getCoordAttribute(l, k + 1) != Field.COORD_WALL) &&
								    (bTemp != null) && !bTemp.isEmpty() && (bBelow != null) && bBelow.isEmpty() &&
								    bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK) && !bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL) )
								{
									bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK, false);
									bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL, true);
									if (bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_IGNORE_BLOCKLINK))
									{
										bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, false);
										bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, false);
										bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, false);
										bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, false);
									}
									bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_LAST_COMMIT, true);
									field.setBlock(l, k + 1, bTemp);
									field.setBlock(l, k, new Block());
								}
							}
						}
					}
				}
			}
		}

		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK, false);
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL, false);

		return result;
	}

	/**
	 * Routine for cascade gravity which checks from the top down for a slower fall animation.
	 * @return <code>true</code> if something falls. <code>false</code> if nothing falls.
	 */
	public static boolean doCascadeSlow(Field field) {
		boolean result = false;

		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL, false);

		for(int i = (field.hidden_height * -1); i < field.getHeightWithoutHurryupFloor(); i++) {
			for(int j = 0; j < field.width; j++) {
				Block blk = field.getBlock(j, i);

				if((blk != null) && !blk.isEmpty() && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY)) {
					boolean fall = true;
					field.checkBlockLink(j, i);

					for(int k = (field.getHeightWithoutHurryupFloor() - 1); k >= (field.hidden_height * -1); k--) {
						for(int l = 0; l < field.width; l++) {
							Block bTemp = field.getBlock(l, k);

							if( (bTemp != null) && !bTemp.isEmpty() &&
								bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK) && !bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL) )
							{
								Block bBelow = field.getBlock(l, k + 1);

								if( (field.getCoordAttribute(l, k + 1) == Field.COORD_WALL) ||
									((bBelow != null) && !bBelow.isEmpty() && !bBelow.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK)) )
								{
									fall = false;
								}
							}
						}
					}

					if(fall) {
						result = true;
						for(int k = (field.getHeightWithoutHurryupFloor() - 1); k >= (field.hidden_height * -1); k--) {
							for(int l = 0; l < field.width; l++) {
								Block bTemp = field.getBlock(l, k);
								Block bBelow = field.getBlock(l, k + 1);

								if( (field.getCoordAttribute(l, k + 1) != Field.COORD_WALL) &&
								    (bTemp != null) && !bTemp.isEmpty() && (bBelow != null) && bBelow.isEmpty() &&
								    bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK) && !bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL) )
								{
									bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK, false);
									bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL, true);
									if (bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_IGNORE_BLOCKLINK))
									{
										bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, false);
										bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, false);
										bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, false);
										bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, false);
									}
									bTemp.setAttribute(Block.BLOCK_ATTRIBUTE_LAST_COMMIT, true);
									field.setBlock(l, k + 1, bTemp);
									field.setBlock(l, k, new Block());
								}
							}
						}
					}
				}
			}
		}

		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK, false);
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL, false);

		return result;
	}

	public static boolean canCascade(Field field) {
		for(int i = (field.getHeightWithoutHurryupFloor() - 1); i >= (field.hidden_height * -1); i--) {
			for(int j = 0; j < field.width; j++) {
				Block blk = field.getBlock(j, i);

				if((blk != null) && !blk.isEmpty() && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY)) {
					boolean fall = true;
					field.checkBlockLink(j, i);

					for(int k = (field.getHeightWithoutHurryupFloor() - 1); k >= (field.hidden_height * -1); k--) {
						for(int l = 0; l < field.width; l++) {
							Block bTemp = field.getBlock(l, k);

							if( (bTemp != null) && !bTemp.isEmpty() &&
								bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK) && !bTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL) )
							{
								Block bBelow = field.getBlock(l, k + 1);

								if( (field.getCoordAttribute(l, k + 1) == Field.COORD_WALL) ||
									((bBelow != null) && !bBelow.isEmpty() && !bBelow.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK)) )
								{
									fall = false;
								}
							}
						}
					}

					if(fall)
						return true;
				}
			}
		}
		return false;
	}
}
