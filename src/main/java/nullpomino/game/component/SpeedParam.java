// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.io.Serializable;

/**
 * BlockWait and emergence rate of fall of the piece timeSuch as data
 */
public class SpeedParam implements Serializable {
	/** Serial version ID */
	private static final long serialVersionUID = -955934100998757270L;

	/** Fall velocity */
	public int gravity;

	/** Denominator of the rate of fall (gravity==denominatorIf1GBecome) */
	public int denominator;

	/** Wait appearance time */
	public int are;

	/** Line clearAfter waiting for the emergence of time */
	public int areLine;

	/** Line clear time */
	public int lineDelay;

	/** Fixation time */
	public int lockDelay;

	/** Lateral motion time */
	public int das;

	/**
	 * Constructor
	 */
	public SpeedParam() {
		reset();
	}

	/**
	 * Copy constructor
	 * @param s Copy source
	 */
	public SpeedParam(SpeedParam s) {
		copy(s);
	}

	/**
	 * Reset to defaults
	 */
	public void reset() {
		gravity = 4;
		denominator = 256;
		are = 24;
		areLine = 24;
		lineDelay = 40;
		lockDelay = 30;
		das = 14;
	}

	/**
	 * AnotherSpeedParamCopied from the
	 * @param s Copy source
	 */
	public void copy(SpeedParam s) {
		gravity = s.gravity;
		denominator = s.denominator;
		are = s.are;
		areLine = s.areLine;
		lineDelay = s.lineDelay;
		lockDelay = s.lockDelay;
		das = s.das;
	}
}
