/*
    Copyright (c) 2010, NullNoname — see LICENSE for details.
*/
package mu.nu.nullpo.gui.sdl;

import mu.nu.nullpo.gui.net.NetLobbyFrame;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.widget.ButtonSDL;

/**
 * Placeholder for the CreateRoom / CreateRoom1P / CreateRated form.  The SDL
 * rewrite of the full Swing form (~20 spinners + 15 checkboxes + 3 dropdowns
 * across 6 tabs) is not yet implemented — the UI shows a message and routes
 * back to the lobby.  Hooking the form up to the widget toolkit is tracked in
 * Phase 7 of the migration plan.
 */
public class StateNetCreateRoomSDL extends BaseStateSDL {
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
		NormalFontSDL.printFont(80, 40, "CREATE ROOM", NormalFontSDL.COLOR_CYAN);
		NormalFontSDL.printFont(40, 120,
				"FULL CREATE-ROOM FORM NOT YET PORTED TO SDL.",
				NormalFontSDL.COLOR_WHITE);
		NormalFontSDL.printFont(40, 144,
				"USE A LEGACY SWING CLIENT TO CREATE CUSTOM ROOMS FOR NOW,",
				NormalFontSDL.COLOR_WHITE);
		NormalFontSDL.printFont(40, 168,
				"OR PICK AN EXISTING ROOM FROM THE LOBBY.",
				NormalFontSDL.COLOR_WHITE);
		backBtn.render();
	}
}
