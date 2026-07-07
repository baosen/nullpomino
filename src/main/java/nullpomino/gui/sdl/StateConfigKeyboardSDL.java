// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.util.GeneralUtil;

/**
 * Keyboard config screen state
 */
public class StateConfigKeyboardSDL extends BaseStateSDL {
	/** Number of frames you have to wait */
	public static final int KEYACCEPTFRAME = 15;

	/** Number of keys to set */
	public static final int NUM_KEYS = 16;

	/** Player number */
	public int player = 0;

	/** true if navigation key setting mode */
	public boolean isNavSetting = false;

	/** Number of button currently being configured */
	protected int keynum;

	/** Frame counter */
	protected int frame;

	/** Frames UP has been held in menu mode (for cursor auto-repeat) */
	protected int upInputState;

	/** Frames DOWN has been held in menu mode (for cursor auto-repeat) */
	protected int downInputState;

	/** Nunber of frames left in key-set mode */
	protected int keyConfigRestFrame;

	/** Button settings */
	protected int[] keymap;

	/** Previous key input state */
	protected boolean[] previousKeyPressedState;

	/** Top-right "back" close button. */
	private ButtonSDL closeBtn;

	/**
	 * Button settings initialization
	 */
	protected void reset() {
		keynum = 0;
		frame = 0;
		upInputState = 0;
		downInputState = 0;
		keyConfigRestFrame = 0;

		keymap = new int[NUM_KEYS];
		previousKeyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];

		for(int i = 0; i < NUM_KEYS; i++) {
			if(!isNavSetting)
				keymap[i] = GameKeySDL.gamekey[player].keymap[i];
			else
				keymap[i] = GameKeySDL.gamekey[player].keymapNav[i];
		}
	}

	/**
	 * Get newly pressed key code
	 * @param prev Previous input state
	 * @param now New input state
	 * @return The newly pressed key code. It will return -1 if none are pressed.
	 */
	protected int getPressedKeyNumber(boolean[] prev, boolean[] now) {
		for(int i = 0; i < now.length; i++) {
			if(prev[i] != now[i]) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Get key name
	 * @param key Keycode (scancode)
	 * @return Key name
	 */
	protected String getKeyName(int key) {
		if((key < 0) || (key >= SDLConstants.SCANCODE_NAMES.length)) {
			return "(" + key + ")";
		}
		return SDLConstants.SCANCODE_NAMES[key];
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		if(!isNavSetting) {
			NormalFontSDL.printFontGrid(1,  1, "KEYBOARD SETTING (" + (player + 1) + "P)", NormalFontSDL.COLOR_ORANGE);
		} else {
			NormalFontSDL.printFontGrid(1,  1, "KEYBOARD NAVIGATION SETTING (" + (player + 1) + "P)", NormalFontSDL.COLOR_ORANGE);
		}

		NormalFontSDL.printFontGrid(2,  3, "UP          : " + getKeyName(keymap[GameKeySDL.BUTTON_UP]), (keynum == 0));
		NormalFontSDL.printFontGrid(2,  4, "DOWN        : " + getKeyName(keymap[GameKeySDL.BUTTON_DOWN]), (keynum == 1));
		NormalFontSDL.printFontGrid(2,  5, "LEFT        : " + getKeyName(keymap[GameKeySDL.BUTTON_LEFT]), (keynum == 2));
		NormalFontSDL.printFontGrid(2,  6, "RIGHT       : " + getKeyName(keymap[GameKeySDL.BUTTON_RIGHT]), (keynum == 3));
		if(!isNavSetting) {
			NormalFontSDL.printFontGrid(2,  7, "A (L/R-ROT) : " + getKeyName(keymap[GameKeySDL.BUTTON_A]), (keynum == 4));
			NormalFontSDL.printFontGrid(2,  8, "B (R/L-ROT) : " + getKeyName(keymap[GameKeySDL.BUTTON_B]), (keynum == 5));
			NormalFontSDL.printFontGrid(2,  9, "C (L/R-ROT) : " + getKeyName(keymap[GameKeySDL.BUTTON_C]), (keynum == 6));
			NormalFontSDL.printFontGrid(2, 10, "D (HOLD)    : " + getKeyName(keymap[GameKeySDL.BUTTON_D]), (keynum == 7));
			NormalFontSDL.printFontGrid(2, 11, "E (180-ROT) : " + getKeyName(keymap[GameKeySDL.BUTTON_E]), (keynum == 8));
		} else {
			NormalFontSDL.printFontGrid(2,  7, "A (SELECT)  : " + getKeyName(keymap[GameKeySDL.BUTTON_A]), (keynum == 4));
			NormalFontSDL.printFontGrid(2,  8, "B (CANCEL)  : " + getKeyName(keymap[GameKeySDL.BUTTON_B]), (keynum == 5));
			NormalFontSDL.printFontGrid(2,  9, "C           : " + getKeyName(keymap[GameKeySDL.BUTTON_C]), (keynum == 6));
			NormalFontSDL.printFontGrid(2, 10, "D           : " + getKeyName(keymap[GameKeySDL.BUTTON_D]), (keynum == 7));
			NormalFontSDL.printFontGrid(2, 11, "E           : " + getKeyName(keymap[GameKeySDL.BUTTON_E]), (keynum == 8));
		}
		NormalFontSDL.printFontGrid(2, 12, "F           : " + getKeyName(keymap[GameKeySDL.BUTTON_F]), (keynum == 9));
		NormalFontSDL.printFontGrid(2, 13, "QUIT        : " + getKeyName(keymap[GameKeySDL.BUTTON_QUIT]), (keynum == 10));
		NormalFontSDL.printFontGrid(2, 14, "PAUSE       : " + getKeyName(keymap[GameKeySDL.BUTTON_PAUSE]), (keynum == 11));
		NormalFontSDL.printFontGrid(2, 15, "GIVEUP      : " + getKeyName(keymap[GameKeySDL.BUTTON_GIVEUP]), (keynum == 12));
		NormalFontSDL.printFontGrid(2, 16, "RETRY       : " + getKeyName(keymap[GameKeySDL.BUTTON_RETRY]), (keynum == 13));
		NormalFontSDL.printFontGrid(2, 17, "FRAME STEP  : " + getKeyName(keymap[GameKeySDL.BUTTON_FRAMESTEP]), (keynum == 14));
		NormalFontSDL.printFontGrid(2, 18, "SCREEN SHOT : " + getKeyName(keymap[GameKeySDL.BUTTON_SCREENSHOT]), (keynum == 15));
		NormalFontSDL.printFontGrid(2, 19, "[SAVE & EXIT]", (keynum == 16));

		NormalFontSDL.printFontGrid(1, 3 + keynum, "b", NormalFontSDL.COLOR_RED);

		if(keyConfigRestFrame > 0) {
			NormalFontSDL.printFontGrid(1, 21, "PUSH KEY... " + GeneralUtil.getTime(keyConfigRestFrame), NormalFontSDL.COLOR_PINK);
		} else if(keynum < NUM_KEYS) {
			NormalFontSDL.printFontGrid(1, 21, "UP/DOWN:        MOVE CURSOR", NormalFontSDL.COLOR_GREEN);
			NormalFontSDL.printFontGrid(1, 22, "ENTER:          SET KEY", NormalFontSDL.COLOR_GREEN);
			NormalFontSDL.printFontGrid(1, 23, "DELETE:         SET TO NONE", NormalFontSDL.COLOR_GREEN);
			NormalFontSDL.printFontGrid(1, 24, "ESC/BACKSPACE:  CANCEL", NormalFontSDL.COLOR_GREEN);
		} else {
			NormalFontSDL.printFontGrid(1, 21, "UP/DOWN:        MOVE CURSOR", NormalFontSDL.COLOR_GREEN);
			NormalFontSDL.printFontGrid(1, 22, "ENTER:          SAVE & EXIT", NormalFontSDL.COLOR_GREEN);
			NormalFontSDL.printFontGrid(1, 23, "ESC/BACKSPACE:  CANCEL", NormalFontSDL.COLOR_GREEN);
		}

		closeBtn.render();
	}

	/*
	 * Update game state
	 */
	@Override
	public void update() {
		// Always poll page nav so edge detection stays in sync during key-set mode
		int pageEvent = PageNavigationSDL.checkPageEvent();

		// Track mouse so click + hover can drive the cursor in menu mode.
		MouseInputSDL.mouseInput.update();

		if(closeBtn.update(MouseInputSDL.mouseInput.getMouseX(), MouseInputSDL.mouseInput.getMouseY(), MouseInputSDL.mouseInput.isMouseClicked())) return;

		if(frame >= KEYACCEPTFRAME) {
			if(keyConfigRestFrame > 0) {
				// Key-set mode
				int key = getPressedKeyNumber(previousKeyPressedState, NullpoMinoSDL.keyPressedState);

				if(key != -1) {
					ResourceHolderSDL.soundManager.play("change");
					keymap[keynum] = key;
					frame = 0;
					upInputState = 0;
					downInputState = 0;
					keyConfigRestFrame = 0;
					return;
				}

				keyConfigRestFrame--;
			} else {
				// Mouse: hover moves cursor across the visible rows; click acts like Enter.
				boolean mouseConfirm = false;
				if(MouseInputSDL.mouseInput.isMouseMoved()) {
					int row = (MouseInputSDL.mouseInput.getMouseY() >> 4) - 3;
					if(row >= 0 && row <= NUM_KEYS && row != keynum) {
						ResourceHolderSDL.soundManager.play("cursor");
						keynum = row;
					}
				}
				if(MouseInputSDL.mouseInput.isMouseClicked()) {
					int row = (MouseInputSDL.mouseInput.getMouseY() >> 4) - 3;
					if(row >= 0 && row <= NUM_KEYS) {
						keynum = row;
						mouseConfirm = true;
					}
				}
				// Menu mode: track UP/DOWN hold for auto-repeat
				if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_UP]) upInputState++;
				else upInputState = 0;
				if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_DOWN]) downInputState++;
				else downInputState = 0;

				if(isMenuRepeatKey(upInputState)) {
					ResourceHolderSDL.soundManager.play("cursor");
					keynum--;
					if(keynum < 0) keynum = NUM_KEYS;
				}
				if(isMenuRepeatKey(downInputState)) {
					ResourceHolderSDL.soundManager.play("cursor");
					keynum++;
					if(keynum > NUM_KEYS) keynum = 0;
				}

				// Page Up / Page Down
				int prevKeynum = keynum;
				keynum = PageNavigationSDL.jumpToEnd(pageEvent, keynum, 0, NUM_KEYS);
				if(keynum != prevKeynum) frame = 0;

				// Enter (or mouse click on a row)
				if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_RETURN] || mouseConfirm) {
					ResourceHolderSDL.soundManager.play("decide");

					if(keynum >= NUM_KEYS) {
						// Save & Exit
						for(int i = 0; i < NUM_KEYS; i++) {
							if(!isNavSetting)
								GameKeySDL.gamekey[player].keymap[i] = keymap[i];
							else
								GameKeySDL.gamekey[player].keymapNav[i] = keymap[i];
						}
						GameKeySDL.gamekey[player].saveConfig(NullpoMinoSDL.propConfig);
						NullpoMinoSDL.saveConfig();
						NullpoMinoSDL.goBack();
					} else {
						// Set Key
						frame = 0;
						keyConfigRestFrame = 60 * 5;
					}
					return;
				}

				// Delete
				if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_DELETE]) {
					if((keynum < NUM_KEYS) && (keymap[keynum] != -1)) {
						ResourceHolderSDL.soundManager.play("change");
						keymap[keynum] = -1;
					}
				}

				// Backspace / Escape / mouse back / right-click
				if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_BACKSPACE]
						|| NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_ESCAPE]
						|| MouseInputSDL.mouseInput.isMouseBackClicked()
						|| MouseInputSDL.mouseInput.isMouseRightClicked()) {
					NullpoMinoSDL.goBack();
					return;
				}
				if(MouseInputSDL.mouseInput.isMouseForwardClicked()) {
					NullpoMinoSDL.goForward();
					return;
				}
			}
		} else {
			// Re-arm so the first frame after the entry gate reopens registers as a fresh press
			upInputState = 0;
			downInputState = 0;
		}

		for(int i = 0; i < NullpoMinoSDL.keyPressedState.length; i++) {
			previousKeyPressedState[i] = NullpoMinoSDL.keyPressedState[i];
		}
		frame++;
	}

	/** Mirrors {@link nullpomino.gui.GameKeyDummy#isMenuRepeatKey} for raw scancode polling. */
	private static boolean isMenuRepeatKey(int holdFrames) {
		return (holdFrames == 1) || ((holdFrames >= 25) && (holdFrames % 3 == 0));
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		reset();
		closeBtn = ButtonSDL.newCloseButton(new Runnable() { public void run() { NullpoMinoSDL.goBack(); } });
		NullpoMinoSDL.enableSpecialKeys = false;
		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, NullpoMinoSDL.GAME_NAME);
	}

	/*
	 * Called when leaving this state
	 */
	@Override
	public void leave() {
		reset();
		NullpoMinoSDL.enableSpecialKeys = true;
	}
}
