// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.io.Serializable;

/**
 * Class that manages the state of music playback
 */
public class BGMStatus implements Serializable {
	/** Serial version ID */
	private static final long serialVersionUID = -1003092972570497408L;

	/** Constant musiccount */
	public static final int BGM_NOTHING = -1,
							BGM_NORMAL1 = 0,
							BGM_NORMAL2 = 1,
							BGM_NORMAL3 = 2,
							BGM_NORMAL4 = 3,
							BGM_NORMAL5 = 4,
							BGM_NORMAL6 = 5,
							BGM_PUZZLE1 = 6,
							BGM_PUZZLE2 = 7,
							BGM_PUZZLE3 = 8,
							BGM_PUZZLE4 = 9,
							BGM_ENDING1 = 10,
							BGM_ENDING2 = 11,
							BGM_SPECIAL1 = 12,
							BGM_SPECIAL2 = 13,
							BGM_SPECIAL3 = 14,
							BGM_SPECIAL4 = 15;

	/** MusicalMaximumcount */
	public static final int BGM_COUNT = 16;

	/** Current BGM number */
	public int bgm;

	/** Volume (1f=100%, 0.5f=50%) */
	public float volume;

	/** BGM fadeoutSwitch */
	public boolean fadesw;

	/**
	 * Constructor
	 */
	public BGMStatus() {
		reset();
	}

	/**
	 * Copy constructor
	 * @param b Copy source
	 */
	public BGMStatus(BGMStatus b) {
		copy(b);
	}

	/**
	 * Reset to defaults
	 */
	public void reset() {
		bgm = BGM_NOTHING;
		volume = 1f;
		fadesw = false;
	}

	/**
	 * OtherBGMStatusCopied from the
	 * @param b Copy source
	 */
	public void copy(BGMStatus b) {
		bgm = b.bgm;
		volume = b.volume;
		fadesw = b.fadesw;
	}

	/**
	 * BGM fadeUpdate of state and volume
	 */
	public void fadeUpdate() {
		if(fadesw == true) {
			if(volume > 0f) {
				volume -= 0.005f;
			} else if(volume < 0f) {
				volume = 0f;
			}
		} else {
			if(volume < 1f) {
				volume = 1f;
			}
		}
	}
}
