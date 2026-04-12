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

import mu.nu.nullpo.gui.GameKeyDummy;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.util.CustomProperties;

/**
 * Key input state manager for SDL3 (uses scancodes instead of keysyms)
 */
public class GameKeySDL extends GameKeyDummy {
	/** Key input state (Used by all game states) */
	public static GameKeySDL gamekey[];

	/** SDL3 hat bitmask values */
	public static final int SDL_HAT_UP    = 0x01;
	public static final int SDL_HAT_RIGHT = 0x02;
	public static final int SDL_HAT_DOWN  = 0x04;
	public static final int SDL_HAT_LEFT  = 0x08;

	/** Default key mappings (using SDL3 scancodes) */
	public static int[][][] DEFAULTKEYS =
	{
		// Ingame
		{
			// Blockbox type
			{
				SDLConstants.SDL_SCANCODE_UP, SDLConstants.SDL_SCANCODE_DOWN, SDLConstants.SDL_SCANCODE_LEFT, SDLConstants.SDL_SCANCODE_RIGHT,
				SDLConstants.SDL_SCANCODE_Z, SDLConstants.SDL_SCANCODE_X, SDLConstants.SDL_SCANCODE_A, SDLConstants.SDL_SCANCODE_SPACE, SDLConstants.SDL_SCANCODE_D, SDLConstants.SDL_SCANCODE_S,
				SDLConstants.SDL_SCANCODE_F12, SDLConstants.SDL_SCANCODE_ESCAPE, SDLConstants.SDL_SCANCODE_F9, SDLConstants.SDL_SCANCODE_F10, SDLConstants.SDL_SCANCODE_N, SDLConstants.SDL_SCANCODE_F5
			},
			// Guideline games type
			{
				SDLConstants.SDL_SCANCODE_SPACE, SDLConstants.SDL_SCANCODE_DOWN, SDLConstants.SDL_SCANCODE_LEFT, SDLConstants.SDL_SCANCODE_RIGHT,
				SDLConstants.SDL_SCANCODE_Z, SDLConstants.SDL_SCANCODE_UP, SDLConstants.SDL_SCANCODE_C, SDLConstants.SDL_SCANCODE_LSHIFT, SDLConstants.SDL_SCANCODE_X, SDLConstants.SDL_SCANCODE_V, SDLConstants.SDL_SCANCODE_F12,
				SDLConstants.SDL_SCANCODE_ESCAPE, SDLConstants.SDL_SCANCODE_F9, SDLConstants.SDL_SCANCODE_F10, SDLConstants.SDL_SCANCODE_N, SDLConstants.SDL_SCANCODE_F5
			},
			// NullpoMino classic type
			{
				SDLConstants.SDL_SCANCODE_UP, SDLConstants.SDL_SCANCODE_DOWN, SDLConstants.SDL_SCANCODE_LEFT, SDLConstants.SDL_SCANCODE_RIGHT,
				SDLConstants.SDL_SCANCODE_A, SDLConstants.SDL_SCANCODE_S, SDLConstants.SDL_SCANCODE_D, SDLConstants.SDL_SCANCODE_Z, SDLConstants.SDL_SCANCODE_X, SDLConstants.SDL_SCANCODE_C,
				SDLConstants.SDL_SCANCODE_ESCAPE, SDLConstants.SDL_SCANCODE_F1, SDLConstants.SDL_SCANCODE_F12, SDLConstants.SDL_SCANCODE_F9, SDLConstants.SDL_SCANCODE_N, SDLConstants.SDL_SCANCODE_F10
			},
		},
		// Menu
		{
			// Blockbox type
			{
				SDLConstants.SDL_SCANCODE_UP, SDLConstants.SDL_SCANCODE_DOWN, SDLConstants.SDL_SCANCODE_LEFT, SDLConstants.SDL_SCANCODE_RIGHT,
				SDLConstants.SDL_SCANCODE_RETURN, SDLConstants.SDL_SCANCODE_ESCAPE, SDLConstants.SDL_SCANCODE_A, SDLConstants.SDL_SCANCODE_SPACE, SDLConstants.SDL_SCANCODE_D, SDLConstants.SDL_SCANCODE_S,
				SDLConstants.SDL_SCANCODE_F12, SDLConstants.SDL_SCANCODE_F1, SDLConstants.SDL_SCANCODE_F9, SDLConstants.SDL_SCANCODE_F10, SDLConstants.SDL_SCANCODE_N, SDLConstants.SDL_SCANCODE_F5
			},
			// Guideline games type
			{
				SDLConstants.SDL_SCANCODE_UP, SDLConstants.SDL_SCANCODE_DOWN, SDLConstants.SDL_SCANCODE_LEFT, SDLConstants.SDL_SCANCODE_RIGHT,
				SDLConstants.SDL_SCANCODE_RETURN, SDLConstants.SDL_SCANCODE_ESCAPE, SDLConstants.SDL_SCANCODE_C, SDLConstants.SDL_SCANCODE_LSHIFT, SDLConstants.SDL_SCANCODE_X, SDLConstants.SDL_SCANCODE_V,
				SDLConstants.SDL_SCANCODE_F12, SDLConstants.SDL_SCANCODE_F1, SDLConstants.SDL_SCANCODE_F9, SDLConstants.SDL_SCANCODE_F10, SDLConstants.SDL_SCANCODE_N, SDLConstants.SDL_SCANCODE_F5
			},
			// NullpoMino classic type
			{
				SDLConstants.SDL_SCANCODE_UP, SDLConstants.SDL_SCANCODE_DOWN, SDLConstants.SDL_SCANCODE_LEFT, SDLConstants.SDL_SCANCODE_RIGHT,
				SDLConstants.SDL_SCANCODE_A, SDLConstants.SDL_SCANCODE_S, SDLConstants.SDL_SCANCODE_D, SDLConstants.SDL_SCANCODE_Z, SDLConstants.SDL_SCANCODE_X, SDLConstants.SDL_SCANCODE_C,
				SDLConstants.SDL_SCANCODE_ESCAPE, SDLConstants.SDL_SCANCODE_F1, SDLConstants.SDL_SCANCODE_F12, SDLConstants.SDL_SCANCODE_F9, SDLConstants.SDL_SCANCODE_N, SDLConstants.SDL_SCANCODE_F10
			},
		},
	};

	/**
	 * Init everything
	 */
	public static void initGlobalGameKeySDL() {
		gamekey = new GameKeySDL[2];
		gamekey[0] = new GameKeySDL(0);
		gamekey[1] = new GameKeySDL(1);
	}

	/**
	 * Constructor with player number param
	 * @param pl Player number
	 */
	public GameKeySDL(int pl) {
		super(pl);
	}

	/**
	 * Update button input status (keyboard only)
	 * @param keyboard Keyboard input array (indexed by scancode)
	 */
	public void update(boolean[] keyboard) {
		update(keyboard, null, 0, 0, 0, false);
	}

	/**
	 * Update button input status (keyboard only, with ingame flag)
	 * @param keyboard Keyboard input array (indexed by scancode)
	 * @param ingame true if ingame
	 */
	public void update(boolean[] keyboard, boolean ingame) {
		update(keyboard, null, 0, 0, 0, ingame);
	}

	/**
	 * Update button input status
	 * @param keyboard Keyboard input array (indexed by scancode)
	 * @param joyButton Joystick button input array (Can be null)
	 * @param joyX Joystick X axis
	 * @param joyY Joystick Y axis
	 * @param hat Joystick hat bitmask (SDL_HAT_UP/DOWN/LEFT/RIGHT)
	 */
	public void update(boolean[] keyboard, boolean[] joyButton, int joyX, int joyY, int hat) {
		update(keyboard, joyButton, joyX, joyY, hat, false);
	}

	/**
	 * Update button input status
	 * @param keyboard Keyboard input array (indexed by scancode)
	 * @param joyButton Joystick button input array (Can be null)
	 * @param joyX Joystick X axis
	 * @param joyY Joystick Y axis
	 * @param hat Joystick hat bitmask (SDL_HAT_UP/DOWN/LEFT/RIGHT)
	 * @param ingame true if ingame
	 */
	public void update(boolean[] keyboard, boolean[] joyButton, int joyX, int joyY, int hat, boolean ingame) {
		for(int i = 0; i < MAX_BUTTON; i++) {
			int[] kmap = ingame ? keymap : keymapNav;
			boolean flag = keyboard[kmap[i]];

			if(i == BUTTON_UP) {
				if( (flag) || (joyY < -joyBorder) || ((hat & SDL_HAT_UP) != 0) ) {
					inputstate[i]++;
				} else {
					inputstate[i] = 0;
				}
			} else if(i == BUTTON_DOWN) {
				if( (flag) || (joyY > joyBorder) || ((hat & SDL_HAT_DOWN) != 0) ) {
					inputstate[i]++;
				} else {
					inputstate[i] = 0;
				}
			} else if(i == BUTTON_LEFT) {
				if((flag) || (joyX < -joyBorder) || ((hat & SDL_HAT_LEFT) != 0) ) {
					inputstate[i]++;
				} else {
					inputstate[i] = 0;
				}
			} else if(i == BUTTON_RIGHT) {
				if((flag) || (joyX > joyBorder) || ((hat & SDL_HAT_RIGHT) != 0) ) {
					inputstate[i]++;
				} else {
					inputstate[i] = 0;
				}
			} else {
				// Misc buttons
				boolean flag2 = false;

				if(joyButton != null) {
					try {
						flag2 = joyButton[buttonmap[i]];
					} catch (ArrayIndexOutOfBoundsException e) {}
				}

				if((flag) || (flag2)) {
					inputstate[i]++;
				} else {
					inputstate[i] = 0;
				}
			}
		}
	}

	/**
	 * Load key settings
	 * @param prop Property file to read from
	 */
	public void loadConfig(CustomProperties prop) {
		super.loadConfig(prop);
		joyBorder = prop.getProperty("joyBorder.p" + player, 0);
	}

	/**
	 * Reset keyboard settings to default (Uses Blockbox type settings)
	 */
	public void loadDefaultKeymap() {
		loadDefaultKeymap(0);
	}

	/**
	 * Reset keyboard settings to default
	 * @param type Settings type (0=Blockbox 1=Guideline 2=NullpoMino-Classic)
	 */
	public void loadDefaultKeymap(int type) {
		loadDefaultGameKeymap(type);
		loadDefaultMenuKeymap(type);
	}

	/**
	 * Reset in-game keyboard settings to default. Menu keys are unchanged.
	 * @param type Settings type (0=Blockbox 1=Guideline 2=NullpoMino-Classic)
	 */
	public void loadDefaultGameKeymap(int type) {
		for(int i = 0; i < keymap.length; i++) {
			keymap[i] = DEFAULTKEYS[0][type][i];
		}
	}

	/**
	 * Reset menu keyboard settings to default. In-game keys are unchanged.
	 * @param type Settings type (0=Blockbox 1=Guideline 2=NullpoMino-Classic)
	 */
	public void loadDefaultMenuKeymap(int type) {
		for(int i = 0; i < keymapNav.length; i++) {
			keymapNav[i] = DEFAULTKEYS[1][type][i];
		}
	}
}
