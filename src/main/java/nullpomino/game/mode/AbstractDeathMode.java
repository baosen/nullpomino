// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

/**
 * Shared 20G "death"-tier speed timing for SPEED MANIA and PHANTOM MANIA.
 *
 * <p>Both modes run the identical six-section ARE / ARE-line / line-delay /
 * lock-delay / DAS curve and the same BGM fadeout/change points; only the
 * surrounding mode logic differs. Hoisting the tables here removes the
 * duplicated copy without changing behavior. (SPEED MANIA 2 uses a different,
 * fourteen-section curve and is intentionally NOT parented here.)
 */
public abstract class AbstractDeathMode extends AbstractManiaMode {

	/** ARE table */
	protected static final int[] tableARE       = {15, 11, 11,  5,  4,  3};

	/** ARE after line clear table */
	protected static final int[] tableARELine   = {11,  5,  5,  4,  4,  3};

	/** Line clear time table */
	protected static final int[] tableLineDelay = {12,  6,  6,  7,  5,  4};

	/** Fixation time table */
	protected static final int[] tableLockDelay = {31, 27, 23, 19, 16, 16};

	/** DAS table */
	protected static final int[] tableDAS       = {11, 11, 10,  9,  7,  7};

	/** BGM fadeout levels */
	protected static final int[] tableBGMFadeout = {280, 480, -1};

	/** BGM change levels */
	protected static final int[] tableBGMChange  = {300, 500, -1};
}
