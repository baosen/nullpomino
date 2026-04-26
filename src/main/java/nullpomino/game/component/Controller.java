// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.io.Serializable;
import java.util.Arrays;

/**
 *  button inputClass to manage the state
 */
public class Controller implements Serializable {
	/** Serial version ID */
	private static final long serialVersionUID = -4855072501928533723L;

	/** ↑ (Hard drop) button */
	public static final int BUTTON_UP = 0;

	/** ↓ (Soft drop) button */
	public static final int BUTTON_DOWN = 1;

	/** ← (Left movement) button */
	public static final int BUTTON_LEFT = 2;

	/** → (Right movement) button */
	public static final int BUTTON_RIGHT = 3;

	/** A (Regular rotation) button */
	public static final int BUTTON_A = 4;

	/** B (Reverse rotation)  button */
	public static final int BUTTON_B = 5;

	/** C (Regular rotation) button */
	public static final int BUTTON_C = 6;

	/** D (Hold) button */
	public static final int BUTTON_D = 7;

	/** E (180-degree rotation) button */
	public static final int BUTTON_E = 8;

	/** F (Use item, staff roll fast-forward, etc.) button */
	public static final int BUTTON_F = 9;

	/** Number of buttons */
	public static final int BUTTON_COUNT = 10;

	/** Constant-bit operationcount */
	public static final int BUTTON_BIT_UP = 1,
							BUTTON_BIT_DOWN = 2,
							BUTTON_BIT_LEFT = 4,
							BUTTON_BIT_RIGHT = 8,
							BUTTON_BIT_A = 16,
							BUTTON_BIT_B = 32,
							BUTTON_BIT_C = 64,
							BUTTON_BIT_D = 128,
							BUTTON_BIT_E = 256,
							BUTTON_BIT_F = 512;

	private static final int[] BUTTON_BITS = {
		BUTTON_BIT_UP,
		BUTTON_BIT_DOWN,
		BUTTON_BIT_LEFT,
		BUTTON_BIT_RIGHT,
		BUTTON_BIT_A,
		BUTTON_BIT_B,
		BUTTON_BIT_C,
		BUTTON_BIT_D,
		BUTTON_BIT_E,
		BUTTON_BIT_F
	};

	/** ButtonIf you hold down thetrue */
	public boolean[] buttonPress;

	/** ButtonI have to leave the press time */
	public int[] buttonTime;

	/**
	 * Constructor
	 */
	public Controller() {
		reset();
	}

	/**
	 * Copy constructor
	 * @param c Copy source
	 */
	public Controller(Controller c) {
		copy(c);
	}

	/**
	 * Back to the initial state
	 */
	public void reset() {
		buttonPress = new boolean[BUTTON_COUNT];
		buttonTime = new int[BUTTON_COUNT];
	}

	/**
	 * OtherController stateCopy
	 * @param c Copy source
	 */
	public void copy(Controller c) {
		buttonPress = Arrays.copyOf(c.buttonPress, BUTTON_COUNT);
		buttonTime = Arrays.copyOf(c.buttonTime, BUTTON_COUNT);
	}

	/**
	 *  buttonThe state is not pressed all the
	 */
	public void clearButtonState() {
		Arrays.fill(buttonPress, false);
	}

	/**
	 *  buttonA1 frame Determine whether the state I was only pressed
	 * @param btn Button number
	 * @return  buttonA1 frame If you hold down onlytrue
	 */
	public boolean isPush(int btn) {
		return (buttonTime[btn] == 1);
	}

	/**
	 *  buttonDetermine whether the state is pressed
	 * @param btn Button number
	 * @return  buttonState if you press thetrue
	 */
	public boolean isPress(int btn) {
		return (buttonTime[btn] >= 1);
	}

	/**
	 * Menu Determines whether the cursor is moved in
	 * @param key Button number
	 * @return If the cursor movestrue
	 */
	public boolean isMenuRepeatKey(int key) {
		return isMenuRepeatKey(key, true);
	}

	/**
	 * Menu Determines whether the cursor is moved in
	 * @param key Button number
	 * @param enableCButton C buttonAllow for high-speed movement
	 * @return If the cursor movestrue
	 */
	public boolean isMenuRepeatKey(int key, boolean enableCButton) {
		return (buttonTime[key] == 1) ||
				((buttonTime[key] >= 25) && (buttonTime[key] % 3 == 0)) ||
				((buttonTime[key] >= 1) && isPress(BUTTON_C) && enableCButton);
	}

	/**
	 *  button inputBit state flagReturns
	 * @return  button inputBit of state flag
	 */
	public int getButtonBit() {
		int input = 0;

		for(int i = 0; i < BUTTON_COUNT; i++) {
			if(buttonPress[i]) input |= BUTTON_BITS[i];
		}

		return input;
	}

	/**
	 *  button inputBit state flagSet based on
	 * @param input  button inputBit of state flag
	 */
	public void setButtonBit(int input) {
		clearButtonState();

		for(int i = 0; i < BUTTON_COUNT; i++) {
			buttonPress[i] = (input & BUTTON_BITS[i]) != 0;
		}
	}

	/**
	 *  button input timeUpdate
	 */
	public void updateButtonTime() {
		for(int i = 0; i < BUTTON_COUNT; i++) {
			buttonTime[i] = buttonPress[i] ? buttonTime[i] + 1 : 0;
		}
	}

}
