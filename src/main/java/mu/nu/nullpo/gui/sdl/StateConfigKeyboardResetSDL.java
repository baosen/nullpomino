package mu.nu.nullpo.gui.sdl;

import mu.nu.nullpo.gui.sdl.binding.SDL3;

/**
 * Keyboard Reset menu (SDL)
 */
public class StateConfigKeyboardResetSDL extends DummyMenuChooseStateSDL {
	/** Player number */
	public int player = 0;

	/**
	 * Constructor
	 */
	public StateConfigKeyboardResetSDL() {
		super();
		maxCursor = 2;
		minChoiceY = 4;
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFontGrid(1, 1, "KEYBOARD RESET (" + (player+1) + "P)", NormalFontSDL.COLOR_ORANGE);
		NormalFontSDL.printFontGrid(1, 3, "RESET SETTINGS TO...", NormalFontSDL.COLOR_GREEN);

		NormalFontSDL.printFontGrid(1, 4 + cursor, "b", NormalFontSDL.COLOR_RED);

		NormalFontSDL.printFontGrid(2, 4, "BLOCKBOX STYLE (DEFAULT)", (cursor == 0));
		NormalFontSDL.printFontGrid(2, 5, "GUIDELINE STYLE", (cursor == 1));
		NormalFontSDL.printFontGrid(2, 6, "NULLPOMINO CLASSIC STYLE", (cursor == 2));

		super.render();
	}

	/*
	 * Decide
	 */
	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");
		GameKeySDL.gamekey[player].loadDefaultKeymap(cursor);
		GameKeySDL.gamekey[player].saveConfig(NullpoMinoSDL.propConfig);
		NullpoMinoSDL.saveConfig();
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		return false;
	}

	/*
	 * Cancel
	 */
	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		return false;
	}
}
