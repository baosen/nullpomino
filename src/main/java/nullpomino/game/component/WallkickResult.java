// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.io.Serializable;

/**
 * WallkickThe resulting class
 */
public class WallkickResult implements Serializable {
	/** Serial version ID */
	private static final long serialVersionUID = -7985029240622355609L;

	/** X-coordinateCorrection amount */
	public int offsetX;

	/** Y-coordinateCorrection amount */
	public int offsetY;

	/** rotationPiece afterDirection */
	public int direction;

	/**
	 * Constructor
	 */
	public WallkickResult() {
		reset();
	}

	/**
	 * With parametersConstructor
	 * @param offsetX X-coordinateCorrection amount
	 * @param offsetY Y-coordinateCorrection amount
	 * @param direction rotationOf Tetoramino afterDirection
	 */
	public WallkickResult(int offsetX, int offsetY, int direction) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.direction = direction;
	}

	/**
	 * Copy constructor
	 * @param w Copy source
	 */
	public WallkickResult(WallkickResult w) {
		copy(w);
	}

	/**
	 * Reset to defaults
	 */
	public void reset() {
		offsetX = 0;
		offsetY = 0;
		direction = 0;
	}

	/**
	 * AnotherWallkickResultCopied from the
	 * @param w Copy source
	 */
	public void copy(WallkickResult w) {
		this.offsetX = w.offsetX;
		this.offsetY = w.offsetY;
		this.direction = w.direction;
	}

	/**
	 * TopDirectionToWallkickDetermine whether
	 * @return TopDirectionToWallkickWhen (offsetY < 0To), in which case thetrue
	 */
	public boolean isUpward() {
		return (offsetY < 0);
	}
}
