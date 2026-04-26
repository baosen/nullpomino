// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.io.Serializable;

/**
 * BackgroundImage state
 */
public class BackgroundStatus implements Serializable {
	/** Serial version ID */
	private static final long serialVersionUID = 2159669210087818385L;

	/** Background number */
	public int bg;

	/** Background fade flag */
	public boolean fadesw;

	/** Background fade state (false for fadeout, true for fade-in) */
	public boolean fadestat;

	/** Background fade usage counter */
	public int fadecount;

	/** Background after fade */
	public int fadebg;

	/**
	 * Default constructor
	 */
	public BackgroundStatus() {
		reset();
	}

	/**
	 * Copy constructor
	 * @param b Copy source
	 */
	public BackgroundStatus(BackgroundStatus b) {
		copy(b);
	}

	/**
	 * Reset to defaults
	 */
	public void reset() {
		bg = 0;
		fadesw = false;
		fadestat = false;
		fadecount = 0;
		fadebg = 0;
	}

	/**
	 * Copy from a different BackgroundStatus
	 * @param b Copy source
	 */
	public void copy(BackgroundStatus b) {
		bg = b.bg;
		fadesw = b.fadesw;
		fadestat = b.fadestat;
		fadecount = b.fadecount;
		fadebg = b.fadebg;
	}

	/**
	 * Update background fade state
	 */
	public void fadeUpdate() {
		if(!fadesw) return;

		if(fadecount < 100) {
			fadecount += 10;
			return;
		}

		if(!fadestat) {
			bg = fadebg;
			fadestat = true;
			fadecount = 0;
			return;
		}

		fadesw = false;
		fadestat = false;
		fadecount = 0;
	}
}
