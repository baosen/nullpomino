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

import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.gui.sdl.binding.SDL3;

/**
 * Title screen state (SDL)
 */
public class StateTitleSDL extends DummyMenuChooseStateSDL {
	/** Strings for menu choices */
	private static final String[] CHOICES = {"START", "REPLAY", "NETPLAY", "OPTIONS", "EXIT"};

	/** UI Text identifier Strings */
	private static final String[] UI_TEXT = {
        "Title_Start", "Title_Replay", "Title_NetPlay", "Title_Config", "Title_Exit"
	};

	public StateTitleSDL () {
		maxCursor = 4;
		minChoiceY = 4;
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		// Update title bar
		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, "NullpoMino version" + GameManager.getVersionString());
		// Observer start
		NullpoMinoSDL.startObserverClient();
		// Call GC
		System.gc();

	}

	/*
	 * Draw the game screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgTitle, null, null);

		NormalFontSDL.printFontGrid(1, 1, "NULLPOMINO", NormalFontSDL.COLOR_ORANGE);
		NormalFontSDL.printFontGrid(1, 2, "VERSION " + GameManager.getVersionString(), NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 4 + cursor, "b", NormalFontSDL.COLOR_RED);

		renderChoices(2, 4, CHOICES);

		NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText(UI_TEXT[cursor]));
	}

	@Override
	protected boolean onDecide() {
		// Skip the decide sound when quitting so it doesn't get cut off mid-
		// playback as the program tears down audio on its way out.
		if(cursor != 4) ResourceHolderSDL.soundManager.play("decide");

		switch(cursor) {
		case 0:
			StateSelectModeSDL.isTopLevel = true;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
			break;
		case 1:
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_REPLAYSELECT);
			break;
		case 2:
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_SERVERSELECT);
			break;
		case 3:
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
			break;
		case 4:
			NullpoMinoSDL.enterState(-1);
			break;
		}
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
