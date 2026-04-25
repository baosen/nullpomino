// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui;

/**
 * Various effects state
 */
public class EffectObject {
	/** Effect type */
	public int effect;

	/** X-coordinate */
	public int x;

	/** Y-coordinate */
	public int y;

	/** Effect parameters (Block colorSuch as) */
	public int param;

	/** Animation counter */
	public int anim;

	/**
	 * Constructor
	 */
	public EffectObject() {
		effect = 0;
		x = 0;
		y = 0;
		param = 0;
		anim = 0;
	}

	/**
	 * With parametersConstructor
	 * @param effect Effect type
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param param Effect parameters (Block colorEtc.)
	 */
	public EffectObject(int effect, int x, int y, int param) {
		this.effect = effect;
		this.x = x;
		this.y = y;
		this.param = param;
		anim = 0;
	}

	/**
	 * Copy constructor
	 * @param src Copy source
	 */
	public EffectObject(EffectObject src) {
		this.effect = src.effect;
		this.x = src.x;
		this.y = src.y;
		this.param = src.param;
		this.anim = src.anim;
	}
}
