package nullpomino.gui.sdl;

import nullpomino.game.play.GameEngine;
import nullpomino.gui.sdl.binding.SDL3;

/**
 * Style select menu (SDL)
 */
public class StateConfigRuleStyleSelectSDL extends DummyMenuChooseStateSDL {
	/** Player ID */
	public int player = 0;

	public StateConfigRuleStyleSelectSDL() {
		super();
		maxCursor = GameEngine.MAX_GAMESTYLE - 1;
		minChoiceY = 3;
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFontGrid(1, 1, "SELECT " + (player+1) + "P STYLE", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		for(int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			NormalFontSDL.printFontGrid(2, 3 + i, GameEngine.GAMESTYLE_NAMES[i], (cursor == i));
		}

		super.render();
	}

	/*
	 * Decide
	 */
	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");
		StateConfigRuleSelectSDL stateR = (StateConfigRuleSelectSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_RULESELECT];
		stateR.player = player;
		stateR.style = cursor;
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_RULESELECT);
		return false;
	}

	/*
	 * Cancel
	 */
	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.goBack();
		return false;
	}
}
