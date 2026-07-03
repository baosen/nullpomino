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

	private static final int[] DESTINATIONS = {
			NullpoMinoSDL.STATE_SELECTMODE,
			NullpoMinoSDL.STATE_REPLAYSELECT,
			NullpoMinoSDL.STATE_NET_LOBBY,
			NullpoMinoSDL.STATE_CONFIG_MAINMENU,
			-1
	};

	/** Active menu rows: NETPLAY is dropped on the web build, where raw
	 * TCP/UDP netplay cannot work inside a browser. */
	private final String[] choices;
	private final String[] uiText;
	private final int[] destinations;
	private final int exitCursor;

	public StateTitleSDL () {
		boolean hideNetplay = NullpoMinoSDL.webMode;
		int count = CHOICES.length - (hideNetplay ? 1 : 0);
		choices = new String[count];
		uiText = new String[count];
		destinations = new int[count];
		int row = 0;
		for(int i = 0; i < CHOICES.length; i++) {
			if(hideNetplay && DESTINATIONS[i] == NullpoMinoSDL.STATE_NET_LOBBY) continue;
			choices[row] = CHOICES[i];
			uiText[row] = UI_TEXT[i];
			destinations[row] = DESTINATIONS[i];
			row++;
		}
		exitCursor = count - 1;
		maxCursor = count - 1;
		minChoiceY = 3;
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		// Update title bar
		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, NullpoMinoSDL.GAME_NAME + " version" + GameManager.getVersionString());
		// Call GC
		System.gc();

	}

	/*
	 * Draw the game screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgTitle, null, null);

		NormalFontSDL.printFontGrid(1, 1, NullpoMinoSDL.GAME_NAME_UPPER, NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		renderChoices(2, 3, choices);

		NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText(uiText[cursor]));

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
		if(cursor != exitCursor) ResourceHolderSDL.soundManager.play("decide");
		NullpoMinoSDL.enterState(destinations[cursor]);
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
