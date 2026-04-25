// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * State of the keyboard setting screen
 */
public class StateConfigKeyboardNaviSDL extends DummyMenuChooseStateSDL {
	/** Player number */
	public int player = 0;

	public StateConfigKeyboardNaviSDL () {
		maxCursor = 1;
		minChoiceY = 3;
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
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFontGrid(1, 1, "KEYBOARD NAVIGATION SETTING (" + (player + 1) + "P)", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		NormalFontSDL.printFontGrid(2, 3, "COPY FROM GAME KEYS", (cursor == 0));
		NormalFontSDL.printFontGrid(2, 4, "CUSTOMIZE", (cursor == 1));
	}

	@Override
	protected boolean onDecide() {
		if (cursor == 0) {
			for(int i = 0; i < GameKeySDL.MAX_BUTTON; i++) {
				GameKeySDL.gamekey[player].keymapNav[i] = GameKeySDL.gamekey[player].keymap[i];
			}
		} else if (cursor == 1) {
			StateConfigKeyboardSDL stateK = (StateConfigKeyboardSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_KEYBOARD];
			stateK.player = player;
			stateK.isNavSetting = true;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_KEYBOARD);
			return true;
		}
		GameKeySDL.gamekey[player].saveConfig(NullpoMinoSDL.propConfig);
		NullpoMinoSDL.saveConfig();

		ResourceHolderSDL.soundManager.play("decide");
		NullpoMinoSDL.goBack();
		return true;
	}

	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.goBack();
		return false;
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
	}

	/*
	 * Called when leaving this state
	 */
	@Override
	public void leave() {
	}
}
