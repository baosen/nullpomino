// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.util.CustomProperties;

/**
 * Joystick Settings MainMenu State
 */
public class StateConfigJoystickMainSDL extends BaseStateSDL {
	/** Player number */
	public int player;

	/** Top-right "back" close button. */
	private ButtonSDL closeBtn;

	/** Cursor position */
	protected int cursor;

	/** Gamepad number in use (-1 = none) */
	protected int joyUseNumber;

	/**
	 * Constructor
	 */
	public StateConfigJoystickMainSDL() {
		player = 0;
		cursor = 0;
	}

	/** Picker label: "NONE", "0 (XBOX 360 CONTROLLER)", or "0 (NOT CONNECTED)". */
	static String gamepadLabel(int number) {
		if(number == -1) return "NONE";
		if(number >= NullpoMinoSDL.joystickMax) return number + " (NOT CONNECTED)";
		return number + " (" + NullpoMinoSDL.joyName[number] + ")";
	}

	/**
	 * Load settings
	 * @param prop Property file to read from
	 */
	protected void loadConfig(CustomProperties prop) {
		// Same default as NullpoMinoSDL.initJoysticks() — this screen saves the
		// value back on OK, so a mismatched -1 here would disable the pad.
		joyUseNumber = prop.getProperty("joyUseNumber.p" + player, player == 0 ? 0 : -1);
	}

	/**
	 * Save settings
	 * @param prop Property file to save to
	 */
	protected void saveConfig(CustomProperties prop) {
		prop.setProperty("joyUseNumber.p" + player, joyUseNumber);
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		loadConfig(NullpoMinoSDL.propConfig);
		closeBtn = ButtonSDL.newCloseButton(new Runnable() { public void run() { NullpoMinoSDL.goBack(); } });
	}

	/*
	 * Draw the game screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFontGrid(1, 1, "GAMEPAD SETTING (" + (player+1) + "P)", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		NormalFontSDL.printFontGrid(2, 3, "[BUTTON SETTING]", (cursor == 0));
		NormalFontSDL.printFontGrid(2, 4, "[INPUT TEST]", (cursor == 1));
		NormalFontSDL.printFontGrid(2, 5, "GAMEPAD:" + gamepadLabel(joyUseNumber), (cursor == 2));

		if(cursor == 0) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_ButtonSetting"));
		if(cursor == 1) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_InputTest"));
		if(cursor == 2) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_JoyUseNumber"));

		closeBtn.render();
	}

	/*
	 * Update game state
	 */
	@Override
	public void update() {
		// Mouse: hover slides the cursor, click confirms, back/right-click/Escape cancels.
		MouseInputSDL.mouseInput.update();
		if(closeBtn.update(MouseInputSDL.mouseInput.getMouseX(), MouseInputSDL.mouseInput.getMouseY(), MouseInputSDL.mouseInput.isMouseClicked())) return;
		boolean mouseConfirm = false;
		if(MouseInputSDL.mouseInput.isMouseMoved()) {
			int row = (MouseInputSDL.mouseInput.getMouseY() >> 4) - 3;
			if(row >= 0 && row <= 2 && row != cursor) {
				cursor = row;
				ResourceHolderSDL.soundManager.play("cursor");
			}
		}
		if(MouseInputSDL.mouseInput.isMouseClicked()) {
			int row = (MouseInputSDL.mouseInput.getMouseY() >> 4) - 3;
			if(row >= 0 && row <= 2) {
				cursor = row;
				mouseConfirm = true;
			}
		}

		// Cursor movement
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_UP)) {
			cursor--;
			if(cursor < 0) cursor = 2;
			ResourceHolderSDL.soundManager.play("cursor");
		}
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_DOWN)) {
			cursor++;
			if(cursor > 2) cursor = 0;
			ResourceHolderSDL.soundManager.play("cursor");
		}

		// Page Up / Page Down
		cursor = PageNavigationSDL.jumpToEnd(PageNavigationSDL.checkPageEvent(), cursor, 0, 2);

		// Configuration changes
		int change = 0;
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_LEFT)) change = -1;
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_RIGHT)) change = 1;

		if(change != 0) {
			ResourceHolderSDL.soundManager.play("change");

			switch(cursor) {
			case 2:
				joyUseNumber += change;
				if(joyUseNumber < -1) joyUseNumber = NullpoMinoSDL.joystickMax - 1;
				if(joyUseNumber > NullpoMinoSDL.joystickMax - 1) joyUseNumber = -1;
				break;
			}
		}

		// Decision button
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_A) || mouseConfirm) {
			ResourceHolderSDL.soundManager.play("decide");

			saveConfig(NullpoMinoSDL.propConfig);
			NullpoMinoSDL.saveConfig();
			NullpoMinoSDL.joyUseNumber[player] = joyUseNumber;

			if(cursor == 0) {
				//[BUTTON SETTING]
				StateConfigJoystickButtonSDL stateJ = (StateConfigJoystickButtonSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_JOYSTICK_BUTTON];
				stateJ.player = player;
				NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_JOYSTICK_BUTTON);
			} else if(cursor == 1) {
				//[INPUT TEST]
				StateConfigJoystickTestSDL stateT = (StateConfigJoystickTestSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_JOYSTICK_TEST];
				stateT.player = player;
				NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_JOYSTICK_TEST);
			} else {
				NullpoMinoSDL.goBack();
			}
		}

		// Cancel button (BUTTON_B, Escape, mouse back, right-click)
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_B)
				|| NullpoMinoSDL.isEscapePushedThisFrame()
				|| MouseInputSDL.mouseInput.isMouseBackClicked()
				|| MouseInputSDL.mouseInput.isMouseRightClicked()) {
			loadConfig(NullpoMinoSDL.propConfig);
			NullpoMinoSDL.goBack();
		}
		else if(MouseInputSDL.mouseInput.isMouseForwardClicked()) {
			NullpoMinoSDL.goForward();
		}
	}
}
