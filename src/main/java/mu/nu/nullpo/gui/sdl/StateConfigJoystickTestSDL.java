/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of NullNoname nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package mu.nu.nullpo.gui.sdl;

import mu.nu.nullpo.gui.sdl.binding.SDL3;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;

/**
 * Joystick State of the test screen
 */
public class StateConfigJoystickTestSDL extends BaseStateSDL {
	/** Key input Accepted to be enabled. frame count */
	public static final int KEYACCEPTFRAME = 20;

	/** Player number */
	public int player;

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
		lastPressButton = -1;
		frame = 0;
		if(joyNumber >= 0) previousJoyPressedState = new boolean[NullpoMinoSDL.joyMaxButton[joyNumber]];
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
			NormalFontSDL.printFontGrid(1, 3, "NO JOYSTICK", NormalFontSDL.COLOR_RED);
		} else if(frame >= KEYACCEPTFRAME) {
			NormalFontSDL.printFontGrid(1, 3, "JOYSTICK NUMBER:" + joyNumber, NormalFontSDL.COLOR_RED);

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
	}

	/*
	 * Update game state
	 */
	@Override
	public void update() {
		if(frame >= KEYACCEPTFRAME) {
			// Backspace & Enter/Return
			if(NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_BACKSPACE] || NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_RETURN]) {
				NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_JOYSTICK_MAIN);
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
