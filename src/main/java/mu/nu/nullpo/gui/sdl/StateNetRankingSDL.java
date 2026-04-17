/*
    Copyright (c) 2010, NullNoname — see LICENSE for details.
*/
package mu.nu.nullpo.gui.sdl;

import mu.nu.nullpo.gui.net.NetLobbyFrame;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.widget.ButtonSDL;

/**
 * Placeholder for the multiplayer ranking screen (per-style tabbed tables in
 * the Swing version).  Not yet implemented — the state shows a "coming soon"
 * message and a back button.
 */
public class StateNetRankingSDL extends BaseStateSDL {
	private ButtonSDL backBtn;

	@Override
	public void enter() {
		backBtn = new ButtonSDL(260, 300, 120, 32, "BACK");
		backBtn.primary = true;
	}

	@Override
	public void update() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl != null) nl.pump();
		MouseInputSDL.mouseInput.update();
		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();
		if(backBtn.update(mx, my, clicked)) NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			if(ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
				NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
			}
		}
	}

	@Override
	public void render() {
		NormalFontSDL.printFont(80, 40, "MULTIPLAYER RANKING", NormalFontSDL.COLOR_CYAN);
		NormalFontSDL.printFont(40, 120, "RANKING SCREEN NOT YET PORTED TO SDL.", NormalFontSDL.COLOR_WHITE);
		backBtn.render();
	}
}
