// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

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

	/** UseJoystick Of number */
	protected int joyUseNumber;

	/** Joystick direction key Threshold for the reaction */
	protected int joyBorder;

	/** Ignore analog stick */
	protected boolean joyIgnoreAxis;

	/** Ignore hat switch */
	protected boolean joyIgnorePOV;

	/**
	 * Constructor
	 */
	public StateConfigJoystickMainSDL() {
		player = 0;
		cursor = 0;
	}

	/**
	 * Load settings
	 * @param prop Property file to read from
	 */
	protected void loadConfig(CustomProperties prop) {
		joyUseNumber = prop.getProperty("joyUseNumber.p" + player, -1);
		joyBorder = prop.getProperty("joyBorder.p" + player, 0);
		joyIgnoreAxis = prop.getProperty("joyIgnoreAxis.p" + player, false);
		joyIgnorePOV = prop.getProperty("joyIgnorePOV.p" + player, false);
	}

	/**
	 * Save settings
	 * @param prop Property file to save to
	 */
	protected void saveConfig(CustomProperties prop) {
		prop.setProperty("joyUseNumber.p" + player, joyUseNumber);
		prop.setProperty("joyBorder.p" + player, joyBorder);
		prop.setProperty("joyIgnoreAxis.p" + player, joyIgnoreAxis);
		prop.setProperty("joyIgnorePOV.p" + player, joyIgnorePOV);
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

		NormalFontSDL.printFontGrid(1, 1, "JOYSTICK SETTING (" + (player+1) + "P)", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		NormalFontSDL.printFontGrid(2, 3, "[BUTTON SETTING]", (cursor == 0));
		NormalFontSDL.printFontGrid(2, 4, "[INPUT TEST]", (cursor == 1));
		NormalFontSDL.printFontGrid(2, 5, "JOYSTICK NUMBER:" + ((joyUseNumber == -1) ? "NOTHING" : String.valueOf(joyUseNumber)), (cursor == 2));
		NormalFontSDL.printFontGrid(2, 6, "JOYSTICK BORDER:" + joyBorder, (cursor == 3));
		NormalFontSDL.printFontGrid(2, 7, "IGNORE AXIS:" + GeneralUtil.getONorOFF(joyIgnoreAxis), (cursor == 4));
		NormalFontSDL.printFontGrid(2, 8, "IGNORE POV:" + GeneralUtil.getONorOFF(joyIgnorePOV), (cursor == 5));

		if(cursor == 0) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_ButtonSetting"));
		if(cursor == 1) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_InputTest"));
		if(cursor == 2) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_JoyUseNumber"));
		if(cursor == 3) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_JoyBorder"));
		if(cursor == 4) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_JoyIgnoreAxis"));
		if(cursor == 5) NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText("ConfigJoystickMain_JoyIgnorePOV"));

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
			if(row >= 0 && row <= 5 && row != cursor) {
				cursor = row;
				ResourceHolderSDL.soundManager.play("cursor");
			}
		}
		if(MouseInputSDL.mouseInput.isMouseClicked()) {
			int row = (MouseInputSDL.mouseInput.getMouseY() >> 4) - 3;
			if(row >= 0 && row <= 5) {
				cursor = row;
				mouseConfirm = true;
			}
		}

		// Cursor movement
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_UP)) {
			cursor--;
			if(cursor < 0) cursor = 5;
			ResourceHolderSDL.soundManager.play("cursor");
		}
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_DOWN)) {
			cursor++;
			if(cursor > 5) cursor = 0;
			ResourceHolderSDL.soundManager.play("cursor");
		}

		// Page Up / Page Down
		cursor = PageNavigationSDL.jumpToEnd(PageNavigationSDL.checkPageEvent(), cursor, 0, 5);

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
			case 3:
				joyBorder += change;
				if(joyBorder < 0) joyBorder = 32768;
				if(joyBorder > 32768) joyBorder = 0;
				break;
			case 4:
				joyIgnoreAxis = !joyIgnoreAxis;
				break;
			case 5:
				joyIgnorePOV = !joyIgnorePOV;
				break;
			}
		}

		// Decision button
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_A) || mouseConfirm) {
			ResourceHolderSDL.soundManager.play("decide");

			saveConfig(NullpoMinoSDL.propConfig);
			NullpoMinoSDL.saveConfig();
			NullpoMinoSDL.joyUseNumber[player] = joyUseNumber;
			NullpoMinoSDL.joyIgnoreAxis[player] = joyIgnoreAxis;
			NullpoMinoSDL.joyIgnorePOV[player] = joyIgnorePOV;
			GameKeySDL.gamekey[player].joyBorder = joyBorder;

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
