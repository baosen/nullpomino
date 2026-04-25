// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.SDL3;

/**
 * State of the configuration screen
 */
public class StateConfigMainMenuSDL extends DummyMenuChooseStateSDL {
	/** UI Text identifier Strings */
	private static final String[] UI_TEXT = {
		"ConfigMainMenu_General",
		"ConfigMainMenu_Rule",
		"ConfigMainMenu_GameTuning",
		"ConfigMainMenu_AI",
		"ConfigMainMenu_Keyboard",
		"ConfigMainMenu_KeyboardNavi",
		"ConfigMainMenu_KeyboardReset",
		"ConfigMainMenu_Joystick"
	};

	/** Player number */
	protected int player = 0;

	public StateConfigMainMenuSDL () {
		maxCursor = 7;
		minChoiceY = 3;
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFontGrid(1, 1, "OPTIONS", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		NormalFontSDL.printFontGrid(2, 3, "[GENERAL OPTIONS]", (cursor == 0));
		NormalFontSDL.printFontGrid(2, 4, "[RULE SELECT]:" + (player + 1) + "P", (cursor == 1));
		NormalFontSDL.printFontGrid(2, 5, "[GAME TUNING]:" + (player + 1) + "P", (cursor == 2));
		NormalFontSDL.printFontGrid(2, 6, "[AI SETTING]:" + (player + 1) + "P", (cursor == 3));
		NormalFontSDL.printFontGrid(2, 7, "[KEYBOARD SETTING]:" + (player + 1) + "P", (cursor == 4));
		NormalFontSDL.printFontGrid(2, 8, "[KEYBOARD NAVIGATION SETTING]:" + (player + 1) + "P", (cursor == 5));
		NormalFontSDL.printFontGrid(2, 9, "[KEYBOARD RESET]:" + (player + 1) + "P", (cursor == 6));
		NormalFontSDL.printFontGrid(2, 10, "[JOYSTICK SETTING]:" + (player + 1) + "P", (cursor == 7));

		NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText(UI_TEXT[cursor]));
	}

	@Override
	protected void onChange(int change) {
		player += change;
		if(player < 0) player = 1;
		if(player > 1) player = 0;
		ResourceHolderSDL.soundManager.play("change");
	}

	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");

		switch(cursor) {
		case 0:
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_GENERAL);
			break;
		case 1:
			StateConfigRuleStyleSelectSDL stateR = (StateConfigRuleStyleSelectSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_RULESTYLESELECT];
			stateR.player = player;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_RULESTYLESELECT);
			break;
		case 2:
			StateConfigGameTuningSDL stateT = (StateConfigGameTuningSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_GAMETUNING];
			stateT.player = player;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_GAMETUNING);
			break;
		case 3:
			StateConfigAISelectSDL stateA = (StateConfigAISelectSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_AISELECT];
			stateA.player = player;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_AISELECT);
			break;
		case 4:
			StateConfigKeyboardSDL stateK = (StateConfigKeyboardSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_KEYBOARD];
			stateK.player = player;
			stateK.isNavSetting = false;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_KEYBOARD);
			break;
		case 5:
			StateConfigKeyboardNaviSDL stateKN = (StateConfigKeyboardNaviSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_KEYBOARD_NAVI];
			stateKN.player = player;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_KEYBOARD_NAVI);
			break;
		case 6:
			StateConfigKeyboardResetSDL stateKR = (StateConfigKeyboardResetSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_KEYBOARD_RESET];
			stateKR.player = player;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_KEYBOARD_RESET);
			break;
		case 7:
			StateConfigJoystickMainSDL stateJ = (StateConfigJoystickMainSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_JOYSTICK_MAIN];
			stateJ.player = player;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_JOYSTICK_MAIN);
			break;
		}
		return false;
	}

	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.goBack();
		return false;
	}
}
