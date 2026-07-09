// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.widget.ButtonSDL;

/**
 * Joystick State of the test screen
 */
public class StateConfigJoystickTestSDL extends BaseStateSDL {
	/** Key input Accepted to be enabled. frame count */
	public static final int KEYACCEPTFRAME = 20;

	/** Player number */
	public int player;

	/** Top-right "back" close button. */
	private ButtonSDL closeBtn;

	/** UseJoystick Of number */
	protected int joyNumber;

	/** I was last pressed button */
	protected int lastPressButton;

	/** Course frame count */
	protected int frame;

	/** Previous frame OfJoystick Of input State */
	protected boolean previousJoyPressedState[];

	/**
	 * Constructor
	 */
	public StateConfigJoystickTestSDL() {
		player = 0;
	}

	/**
	 * Various reset
	 */
	protected void reset() {
		joyNumber = NullpoMinoSDL.joyUseNumber[player];
		// Configured gamepad may not be connected (or gone after hotplug)
		if(joyNumber >= NullpoMinoSDL.joystickMax) joyNumber = -1;
		lastPressButton = -1;
		frame = 0;
		if(joyNumber >= 0) previousJoyPressedState = new boolean[SDLConstants.SDL_GAMEPAD_NUM_BUTTONS];
		else previousJoyPressedState = null;
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

		NormalFontSDL.printFontGrid(1, 1, "JOYSTICK INPUT TEST (" + (player + 1) + "P)", NormalFontSDL.COLOR_ORANGE);

		if(joyNumber < 0) {
			NormalFontSDL.printFontGrid(1, 3, "NO GAMEPAD", NormalFontSDL.COLOR_RED);
		} else if(frame >= KEYACCEPTFRAME) {
			NormalFontSDL.printFontGrid(1, 3, "GAMEPAD:" + joyNumber + " (" + NullpoMinoSDL.joyName[joyNumber] + ")", NormalFontSDL.COLOR_RED);

			NormalFontSDL.printFontGrid(1, 5, "LAST PRESSED BUTTON:" + ((lastPressButton == -1) ? "NONE" : String.valueOf(lastPressButton)));

			NormalFontSDL.printFontGrid(1, 7, "AXIS X:" + NullpoMinoSDL.joyAxisX[joyNumber]);
			NormalFontSDL.printFontGrid(1, 8, "AXIS Y:" + NullpoMinoSDL.joyAxisY[joyNumber]);

			String strHat = "";
			int hat = NullpoMinoSDL.joyHatState[joyNumber];
			if(hat == 0) {
				strHat = "CENTER ";
			} else {
				if((hat & GameKeySDL.SDL_HAT_UP) != 0) strHat += "UP ";
				if((hat & GameKeySDL.SDL_HAT_DOWN) != 0) strHat += "DOWN ";
				if((hat & GameKeySDL.SDL_HAT_LEFT) != 0) strHat += "LEFT ";
				if((hat & GameKeySDL.SDL_HAT_RIGHT) != 0) strHat += "RIGHT ";
			}
			NormalFontSDL.printFontGrid(1, 10, "POV:" + strHat);
		}

		if(frame >= KEYACCEPTFRAME) {
			NormalFontSDL.printFontGrid(1, 23, "ENTER/BACKSPACE: EXIT", NormalFontSDL.COLOR_GREEN);
		}

		closeBtn.render();
	}

	/*
	 * Update game state
	 */
	@Override
	public void update() {
		// Hotplug may have re-enumerated gamepads since reset(); re-sync before
		// indexing joyPressedState with a stale device number.
		if(joyNumber >= 0 && joyNumber >= NullpoMinoSDL.joystickMax) {
			reset();
		}

		MouseInputSDL.mouseInput.update();

		if(closeBtn.update(MouseInputSDL.mouseInput.getMouseX(), MouseInputSDL.mouseInput.getMouseY(), MouseInputSDL.mouseInput.isMouseClicked())) return;

		if(frame >= KEYACCEPTFRAME) {
			// Backspace, Enter/Return, Escape, mouse back / right-click → exit
			if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_BACKSPACE]
					|| NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_RETURN]
					|| NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_ESCAPE]
					|| MouseInputSDL.mouseInput.isMouseClicked()
					|| MouseInputSDL.mouseInput.isMouseBackClicked()
					|| MouseInputSDL.mouseInput.isMouseRightClicked()) {
				NullpoMinoSDL.goBack();
				return;
			}
			else if(MouseInputSDL.mouseInput.isMouseForwardClicked()) {
				NullpoMinoSDL.goForward();
				return;
			}
			// Joystick input
			else if(previousJoyPressedState != null) {
				int key = getPressedKeyNumber(previousJoyPressedState, NullpoMinoSDL.joyPressedState[joyNumber]);

				if(key != -1) {
					ResourceHolderSDL.soundManager.play("change");
					lastPressButton = key;
				}
			}
		}

		if(previousJoyPressedState != null) {
			System.arraycopy(NullpoMinoSDL.joyPressedState[joyNumber], 0, previousJoyPressedState, 0, previousJoyPressedState.length);
		}
		frame++;
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
