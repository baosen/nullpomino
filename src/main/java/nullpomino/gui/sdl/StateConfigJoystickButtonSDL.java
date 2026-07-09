// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.widget.ButtonSDL;

/**
 * Joystick buttonState of the configuration screen
 */
public class StateConfigJoystickButtonSDL extends BaseStateSDL {
	/** Key input Accepted to be enabled. frame count */
	public static final int KEYACCEPTFRAME = 20;

	/** Player number */
	public int player;

	/** UseJoystick Of number */
	protected int joyNumber;

	/** Number of button currently being configured */
	protected int keynum;

	/** Course frame count */
	protected int frame;

	/** Frames UP has been held in menu mode (for cursor auto-repeat) */
	protected int upInputState;

	/** Frames DOWN has been held in menu mode (for cursor auto-repeat) */
	protected int downInputState;

	/** Button settings */
	protected int buttonmap[];

	/** Previous frame OfJoystick Of input State */
	protected boolean previousJoyPressedState[];

	/** Top-right "back" close button. */
	private ButtonSDL closeBtn;

	/**
	 * Button settings initialization
	 */
	protected void reset() {
		keynum = 4;
		frame = 0;
		upInputState = 0;
		downInputState = 0;

		buttonmap = new int[GameKeySDL.MAX_BUTTON];

		joyNumber = NullpoMinoSDL.joyUseNumber[player];
		// Configured device may not be connected (or gone after hotplug)
		if(joyNumber >= NullpoMinoSDL.joyMaxButton.length) joyNumber = -1;

		if(joyNumber >= 0)
			previousJoyPressedState = new boolean[NullpoMinoSDL.joyMaxButton[joyNumber]];
		else
			previousJoyPressedState = null;

		for(int i = 0; i < GameKeySDL.MAX_BUTTON; i++) {
			buttonmap[i] = GameKeySDL.gamekey[player].buttonmap[i];
		}
	}

	/**
	 * Pressed buttonOf numberReturns
	 * @param prev Previous frame In input State
	 * @param now This frame In input State
	 * @return Pressed buttonOf number, If you do not-1
	 */
	protected int getPressedKeyNumber(boolean[] prev, boolean[] now) {
		for(int i = 0; i < now.length; i++) {
			if(prev[i] != now[i]) {
				return i;
			}
		}

		return -1;
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFontGrid(1, 1, "JOYSTICK BUTTON SETTING (" + (player + 1) + "P)", NormalFontSDL.COLOR_ORANGE);

		if(previousJoyPressedState == null)
			NormalFontSDL.printFontGrid(1, 3, "NO JOYSTICK", NormalFontSDL.COLOR_RED);
		else
			NormalFontSDL.printFontGrid(1, 3, "JOYSTICK NUMBER:" + joyNumber, NormalFontSDL.COLOR_RED);

		NormalFontSDL.printFontGrid(2, 5, "A (L/R-ROT)    : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_A]), (keynum == 4));
		NormalFontSDL.printFontGrid(2, 6, "B (R/L-ROT)    : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_B]), (keynum == 5));
		NormalFontSDL.printFontGrid(2, 7, "C (L/R-ROT)    : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_C]), (keynum == 6));
		NormalFontSDL.printFontGrid(2, 8, "D (HOLD)       : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_D]), (keynum == 7));
		NormalFontSDL.printFontGrid(2, 9, "E (180-ROT)    : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_E]), (keynum == 8));
		NormalFontSDL.printFontGrid(2, 10, "F              : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_F]), (keynum == 9));
		NormalFontSDL.printFontGrid(2, 11, "QUIT           : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_QUIT]), (keynum == 10));
		NormalFontSDL.printFontGrid(2, 12, "PAUSE          : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_PAUSE]), (keynum == 11));
		NormalFontSDL.printFontGrid(2, 13, "GIVEUP         : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_GIVEUP]), (keynum == 12));
		NormalFontSDL.printFontGrid(2, 14, "RETRY          : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_RETRY]), (keynum == 13));
		NormalFontSDL.printFontGrid(2, 15, "FRAME STEP     : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_FRAMESTEP]), (keynum == 14));
		NormalFontSDL.printFontGrid(2, 16, "SCREEN SHOT    : " + String.valueOf(buttonmap[GameKeySDL.BUTTON_SCREENSHOT]), (keynum == 15));

		NormalFontSDL.printFontGrid(1, 5 + keynum - 4, "b", NormalFontSDL.COLOR_RED);

		NormalFontSDL.printFontGrid(1, 20, "UP/DOWN:   MOVE CURSOR", NormalFontSDL.COLOR_GREEN);
		NormalFontSDL.printFontGrid(1, 21, "ENTER:     OK",     NormalFontSDL.COLOR_GREEN);
		NormalFontSDL.printFontGrid(1, 22, "DELETE:    NO SET", NormalFontSDL.COLOR_GREEN);
		NormalFontSDL.printFontGrid(1, 23, "BACKSPACE: CANCEL", NormalFontSDL.COLOR_GREEN);

		closeBtn.render();
	}

	/*
	 * Update game state
	 */
	@Override
	public void update() {
		// Hotplug may have re-enumerated devices since reset(); re-sync before
		// indexing joyPressedState/joyMaxButton with stale sizes.
		if(joyNumber >= 0 && (joyNumber >= NullpoMinoSDL.joyMaxButton.length
				|| previousJoyPressedState.length != NullpoMinoSDL.joyMaxButton[joyNumber])) {
			int cursor = keynum;
			int[] pending = buttonmap;
			reset();
			keynum = cursor;
			buttonmap = pending;
		}

		MouseInputSDL.mouseInput.update();

		if(closeBtn.update(MouseInputSDL.mouseInput.getMouseX(), MouseInputSDL.mouseInput.getMouseY(), MouseInputSDL.mouseInput.isMouseClicked())) return;

		// PageUp/PageDown is edge-detected; poll once per frame so the
		// shared previous-state stays in sync even during the entry gate.
		int pageEvent = PageNavigationSDL.checkPageEvent();

		if(frame >= KEYACCEPTFRAME) {
			// Mouse: hover moves the cursor across the visible rows; click selects.
			if(MouseInputSDL.mouseInput.isMouseMoved()) {
				int row = (MouseInputSDL.mouseInput.getMouseY() >> 4) - 5 + 4;
				if(row >= 4 && row <= 15 && row != keynum) {
					ResourceHolderSDL.soundManager.play("cursor");
					keynum = row;
				}
			}
			if(MouseInputSDL.mouseInput.isMouseClicked()) {
				int row = (MouseInputSDL.mouseInput.getMouseY() >> 4) - 5 + 4;
				if(row >= 4 && row <= 15) {
					keynum = row;
				}
			}

			// Track UP/DOWN hold for auto-repeat. Throttling here used to be
			// "frame = 0 after every move", which gave one cursor step per
			// KEYACCEPTFRAME (3 steps/sec) and made the menu feel laggy. Use
			// the same hold counter pattern as StateConfigKeyboardSDL instead.
			if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_UP]) upInputState++;
			else upInputState = 0;
			if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_DOWN]) downInputState++;
			else downInputState = 0;

			if(isMenuRepeatKey(upInputState)) {
				ResourceHolderSDL.soundManager.play("cursor");
				keynum--;
				if(keynum < 4) keynum = 15;
			}
			if(isMenuRepeatKey(downInputState)) {
				ResourceHolderSDL.soundManager.play("cursor");
				keynum++;
				if(keynum > 15) keynum = 4;
			}

			keynum = PageNavigationSDL.jumpToEnd(pageEvent, keynum, 4, 15);

			// Delete
			if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_DELETE]) {
				ResourceHolderSDL.soundManager.play("change");
				buttonmap[keynum] = -1;
				frame = 0;
			}
			// Backspace / Escape / mouse back / right-click
			else if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_BACKSPACE]
					|| NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_ESCAPE]
					|| MouseInputSDL.mouseInput.isMouseBackClicked()
					|| MouseInputSDL.mouseInput.isMouseRightClicked()) {
				NullpoMinoSDL.goBack();
				return;
			}
			else if(MouseInputSDL.mouseInput.isMouseForwardClicked()) {
				NullpoMinoSDL.goForward();
				return;
			}
			// Enter/Return
			else if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_RETURN]) {
				ResourceHolderSDL.soundManager.play("decide");

				for(int i = 0; i < GameKeySDL.MAX_BUTTON; i++) {
					GameKeySDL.gamekey[player].buttonmap[i] = buttonmap[i];
				}
				GameKeySDL.gamekey[player].saveConfig(NullpoMinoSDL.propConfig);
				NullpoMinoSDL.saveConfig();

				NullpoMinoSDL.goBack();
				return;
			}
			// Joystick input
			else if(previousJoyPressedState != null) {
				int key = getPressedKeyNumber(previousJoyPressedState, NullpoMinoSDL.joyPressedState[joyNumber]);

				if(key != -1) {
					ResourceHolderSDL.soundManager.play("change");
					buttonmap[keynum] = key;
					frame = 0;
				}
			}
		} else {
			// Re-arm hold counters so the first frame after the entry gate
			// reopens registers as a fresh press rather than mid-repeat.
			upInputState = 0;
			downInputState = 0;
		}

		if(previousJoyPressedState != null) {
			System.arraycopy(NullpoMinoSDL.joyPressedState[joyNumber], 0, previousJoyPressedState, 0, previousJoyPressedState.length);
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
