// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.SDL3;

/**
 * State of the configuration screen
 */
public class StateConfigMainMenuSDL extends DummyMenuChooseStateSDL {
	private static final StatePreparer NO_PREP = (state, player) -> {};

	private static final MenuEntry[] ENTRIES = {
			new MenuEntry(
					"[GENERAL OPTIONS]",
					"ConfigMainMenu_General",
					NullpoMinoSDL.STATE_CONFIG_GENERAL,
					false,
					NO_PREP),
			new MenuEntry(
					"[RULE SELECT]",
					"ConfigMainMenu_Rule",
					NullpoMinoSDL.STATE_CONFIG_RULESTYLESELECT,
					true,
					(state, player) -> ((StateConfigRuleStyleSelectSDL)state).player = player),
			new MenuEntry(
					"[GAME TUNING]",
					"ConfigMainMenu_GameTuning",
					NullpoMinoSDL.STATE_CONFIG_GAMETUNING,
					true,
					(state, player) -> ((StateConfigGameTuningSDL)state).player = player),
			new MenuEntry(
					"[AI SETTING]",
					"ConfigMainMenu_AI",
					NullpoMinoSDL.STATE_CONFIG_AISELECT,
					true,
					(state, player) -> ((StateConfigAISelectSDL)state).player = player),
			new MenuEntry(
					"[KEYBOARD SETTING]",
					"ConfigMainMenu_Keyboard",
					NullpoMinoSDL.STATE_CONFIG_KEYBOARD,
					true,
					(state, player) -> {
						StateConfigKeyboardSDL keyboard = (StateConfigKeyboardSDL)state;
						keyboard.player = player;
						keyboard.isNavSetting = false;
					}),
			new MenuEntry(
					"[KEYBOARD NAVIGATION SETTING]",
					"ConfigMainMenu_KeyboardNavi",
					NullpoMinoSDL.STATE_CONFIG_KEYBOARD_NAVI,
					true,
					(state, player) -> ((StateConfigKeyboardNaviSDL)state).player = player),
			new MenuEntry(
					"[KEYBOARD RESET]",
					"ConfigMainMenu_KeyboardReset",
					NullpoMinoSDL.STATE_CONFIG_KEYBOARD_RESET,
					true,
					(state, player) -> ((StateConfigKeyboardResetSDL)state).player = player),
			new MenuEntry(
					"[GAMEPAD SETTING]",
					"ConfigMainMenu_Joystick",
					NullpoMinoSDL.STATE_CONFIG_JOYSTICK_MAIN,
					true,
					(state, player) -> ((StateConfigJoystickMainSDL)state).player = player),
	};

	/** UI Text identifier Strings */
	private static final String[] UI_TEXT = uiTextKeys();

	/** Player number */
	protected int player = 0;

	public StateConfigMainMenuSDL () {
		maxCursor = ENTRIES.length - 1;
		minChoiceY = 3;
	}

	@Override
	public void enter() {
		closeBtn = newCloseButton();
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFontGrid(1, 1, "OPTIONS", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		for(int i = 0; i < ENTRIES.length; i++) {
			NormalFontSDL.printFontGrid(2, 3 + i, ENTRIES[i].label(player), cursor == i);
		}

		NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText(UI_TEXT[cursor]));

		super.render();
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
		ENTRIES[cursor].enter(player);
		return false;
	}

	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.goBack();
		return false;
	}

	private static String[] uiTextKeys() {
		String[] keys = new String[ENTRIES.length];
		for(int i = 0; i < keys.length; i++) {
			keys[i] = ENTRIES[i].uiTextKey;
		}
		return keys;
	}

	private record MenuEntry(String label, String uiTextKey,
			int stateID, boolean playerSpecific, StatePreparer preparer) {
		String label(int player) {
			return playerSpecific ? label + ":" + (player + 1) + "P" : label;
		}

		void enter(int player) {
			preparer.prepare(NullpoMinoSDL.gameStates[stateID], player);
			NullpoMinoSDL.enterState(stateID);
		}
	}

	@FunctionalInterface
	private interface StatePreparer {
		void prepare(BaseStateSDL state, int player);
	}
}
