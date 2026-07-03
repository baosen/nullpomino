// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.game.play.GameManager;
import nullpomino.gui.sdl.binding.SDL3;

/**
 * Title screen state (SDL)
 */
public class StateTitleSDL extends DummyMenuChooseStateSDL {
	/** Strings for menu choices */
	private static final String[] CHOICES = {"PLAY", "REPLAY", "NETPLAY", "OPTIONS", "EXIT"};

	/** UI Text identifier Strings */
	private static final String[] UI_TEXT = {
        "Title_Start", "Title_Replay", "Title_NetPlay", "Title_Config", "Title_Exit"
	};

	private static final int EXIT_CURSOR = 4;

	private static final int[] DESTINATIONS = {
			NullpoMinoSDL.STATE_SELECTMODE,
			NullpoMinoSDL.STATE_REPLAYSELECT,
			NullpoMinoSDL.STATE_NET_SERVERSELECT,
			NullpoMinoSDL.STATE_CONFIG_MAINMENU,
			-1
	};

	public StateTitleSDL () {
		maxCursor = CHOICES.length - 1;
		minChoiceY = 3;
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		// Update title bar
		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, "NullpoMino version" + GameManager.getVersionString());
		// Call GC
		System.gc();

	}

	/*
	 * Draw the game screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgTitle, null, null);

		NormalFontSDL.printFontGrid(1, 1, "NULLPOMINO+", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		renderChoices(2, 3, CHOICES);

		NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText(UI_TEXT[cursor]));

		// Bitmap font has no lowercase or hex digits a–f, so render via TTF.
		// Right-aligned to one grid cell from the screen edge to mirror the
		// margin the old version string used.
		String buildString = GameManager.getCommitHash() + " (" + (GameManager.isDevBuild() ? "debug" : "release") + ")";
		int buildWidth = NormalFontSDL.getTTFStringWidth(buildString);
		NormalFontSDL.printTTFFont(640 - 16 - buildWidth, 448, buildString, NormalFontSDL.COLOR_LIGHTGRAY);
	}

	@Override
	protected boolean onDecide() {
		// Skip the decide sound when quitting so it doesn't get cut off mid-
		// playback as the program tears down audio on its way out.
		if(cursor != EXIT_CURSOR) ResourceHolderSDL.soundManager.play("decide");
		NullpoMinoSDL.enterState(DESTINATIONS[cursor]);
		return false;
	}

	@Override
	protected boolean onCancel() {
		// The title is the root screen, so the back stack is normally empty
		// here — goBack() falls through to enterState(-1) and quits. If the
		// stack ever does contain a prior screen (e.g. the user tabbed back
		// via the main menu), it still does the right thing.
		NullpoMinoSDL.goBack();
		return true;
	}
}
