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

import java.util.LinkedList;

import mu.nu.nullpo.game.net.NetPlayerInfo;
import mu.nu.nullpo.game.net.NetRoomInfo;
import mu.nu.nullpo.gui.net.NetLobbyFrame;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.widget.ButtonSDL;
import mu.nu.nullpo.gui.sdl.widget.TableSDL;
import mu.nu.nullpo.gui.sdl.widget.TextInputSDL;
import mu.nu.nullpo.gui.sdl.widget.WidgetSDL;

/**
 * Lobby state: room browser + lobby chat + online player list.  Replaces the
 * "Lobby tab" of the Swing NetLobbyFrame.
 */
public class StateNetLobbySDL extends BaseStateSDL {
	private TableSDL roomTable;
	private TextInputSDL chatInput;
	private ButtonSDL sendBtn;
	private ButtonSDL joinBtn;
	private ButtonSDL watchBtn;
	private ButtonSDL createBtn;
	private ButtonSDL create1PBtn;
	private ButtonSDL createRatedBtn;
	private ButtonSDL rankingBtn;
	private ButtonSDL rulechangeBtn;
	private ButtonSDL disconnectBtn;

	private WidgetSDL focused;
	private String statusLine = "";

	@Override
	public void enter() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_SERVERSELECT); return; }

		TableSDL.Column[] cols = {
			new TableSDL.Column("ID",    40),
			new TableSDL.Column("NAME", 150),
			new TableSDL.Column("RT",    32),
			new TableSDL.Column("RULE",  96),
			new TableSDL.Column("MODE",  96),
			new TableSDL.Column("STAT",  64),
			new TableSDL.Column("P",     40),
			new TableSDL.Column("S",     40),
		};
		roomTable = new TableSDL(8, 40, 624, 220, cols);

		// Chat input + buttons at bottom
		chatInput = new TextInputSDL(8, 390, 496, 28);
		chatInput.maxChars = 255;
		chatInput.placeholder = "Chat...";
		final NetLobbyFrame nlf = nl;
		sendBtn = new ButtonSDL(508, 390, 80, 28, "SEND", new Runnable() { public void run() { sendChat(nlf); } });
		sendBtn.primary = true;

		int actY = 266;
		joinBtn       = new ButtonSDL(  4, actY,  80, 28, "JOIN",    new Runnable() { public void run() { attemptJoinSelected(false); } });
		joinBtn.primary = true;
		watchBtn      = new ButtonSDL( 88, actY,  80, 28, "WATCH",   new Runnable() { public void run() { attemptJoinSelected(true); } });
		createBtn     = new ButtonSDL(172, actY,  96, 28, "CREATE",  new Runnable() { public void run() { enterCreateRoom(false, false); } });
		create1PBtn   = new ButtonSDL(272, actY,  40, 28, "1P",      new Runnable() { public void run() { enterCreateRoom(true,  false); } });
		createRatedBtn= new ButtonSDL(316, actY,  80, 28, "RATED",   new Runnable() { public void run() { enterCreateRoom(false, true);  } });
		rankingBtn    = new ButtonSDL(400, actY, 112, 28, "RANKING", new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_RANKING); } });
		rulechangeBtn = new ButtonSDL(516, actY,  80, 28, "RULES",   new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_RULECHANGE); } });
		disconnectBtn = new ButtonSDL(600, actY,  32, 28, "X",       new Runnable() { public void run() { NullpoMinoSDL.endNetplay(); } });

		// Default focus goes on the room table so arrow keys navigate rooms
		// immediately; pressing TAB or clicking the chat field switches to typing.
		setFocus(roomTable);
		statusLine = "";
	}

	@Override
	public void leave() {
		setFocus(null);
		NullpoMinoSDL.stopTextInput();
	}

	private void setFocus(WidgetSDL w) {
		if(focused == w) return;
		if(focused != null) focused.setFocused(false);
		focused = w;
		if(focused != null) focused.setFocused(true);
	}

	@Override
	public void update() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE); return; }
		nl.pump();
		MouseInputSDL.mouseInput.update();

		// If the session has disconnected (pump may have set lobbyMode), bail back to server select.
		if(nl.netPlayerClient == null || !nl.netPlayerClient.isConnected()) {
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_SERVERSELECT);
			return;
		}

		// roomjoinsuccess arrived on the reader thread and was drained by pump() above:
		// the session flips to IN-ROOM, which is our cue to hand off to the game state.
		if(nl.lobbyMode == NetLobbyFrame.LOBBYMODE_INROOM) {
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NETGAME);
			return;
		}

		// Refresh room-table rows from the session.
		refreshRoomTable(nl);

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		if(roomTable.update(mx, my, clicked)) setFocus(roomTable);
		if(roomTable.activated) attemptJoinSelected(false);
		if(chatInput.update(mx, my, clicked)) setFocus(chatInput);
		nl.chatLogLobby.update(mx, my, clicked);

		// Button actions are wired in enter() and fire from ButtonSDL itself on click.
		sendBtn.update(mx, my, clicked);
		joinBtn.update(mx, my, clicked);
		watchBtn.update(mx, my, clicked);
		createBtn.update(mx, my, clicked);
		create1PBtn.update(mx, my, clicked);
		createRatedBtn.update(mx, my, clicked);
		rankingBtn.update(mx, my, clicked);
		rulechangeBtn.update(mx, my, clicked);
		disconnectBtn.update(mx, my, clicked);

		// Deliver typed text and key events.  UP/DOWN navigate rows inside the room
		// table and jump to the chat input at the list boundary; from chat they move
		// focus back to the table.  HOME/END always drive the list.  PAGEUP/PAGEDOWN
		// stay with the chat-log scroll handler in handleGlobalKey.
		String typed = NullpoMinoSDL.consumeTextInput();
		if(focused != null && typed.length() > 0) focused.handleTextInput(typed);
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			handleGlobalKey(nl, ev);
			if(ev.scancode == SDLConstants.SDL_SCANCODE_HOME || ev.scancode == SDLConstants.SDL_SCANCODE_END) {
				roomTable.handleKey(ev);
				continue;
			}
			boolean up    = ev.scancode == SDLConstants.SDL_SCANCODE_UP;
			boolean down  = ev.scancode == SDLConstants.SDL_SCANCODE_DOWN;
			boolean left  = ev.scancode == SDLConstants.SDL_SCANCODE_LEFT;
			boolean right = ev.scancode == SDLConstants.SDL_SCANCODE_RIGHT;

			// LEFT/RIGHT cycle within the action-button row; text fields still get
			// the caret-movement behaviour elsewhere.
			if((left || right) && !ev.repeat && tryButtonRowNav(left)) continue;

			if((up || down) && !ev.repeat && tryWidgetNav(up)) continue;
			if((up || down) && ev.repeat && focused == roomTable) {
				roomTable.handleKey(ev);
				continue;
			}
			if(focused != null) focused.handleKey(ev);
		}
	}

	/** Ordered action-button row used for keyboard navigation. */
	private ButtonSDL[] buttonRow() {
		return new ButtonSDL[] { joinBtn, watchBtn, createBtn, create1PBtn, createRatedBtn,
				rankingBtn, rulechangeBtn, disconnectBtn };
	}

	/**
	 * Vertical nav cycle: roomTable → button row → chatInput → wrap.  On the
	 * room table, UP/DOWN scroll rows until the boundary, then jump out.
	 */
	private boolean tryWidgetNav(boolean up) {
		ButtonSDL[] row = buttonRow();
		if(focused == roomTable) {
			int sel = roomTable.getSelectedIndex();
			int rows = roomTable.getRowCount();
			if(up && sel <= 0) { setFocus(chatInput); return true; }
			if(!up && (rows == 0 || sel >= rows - 1)) { setFocus(row[0]); return true; }
			return false;
		}
		for(ButtonSDL b : row) {
			if(focused == b) {
				setFocus(up ? roomTable : chatInput);
				return true;
			}
		}
		if(focused == chatInput) {
			setFocus(up ? row[0] : roomTable);
			return true;
		}
		return false;
	}

	/** LEFT/RIGHT moves between buttons in the action row. */
	private boolean tryButtonRowNav(boolean left) {
		ButtonSDL[] row = buttonRow();
		for(int i = 0; i < row.length; i++) {
			if(focused == row[i]) {
				int next = left ? i - 1 : i + 1;
				if(next < 0) next = row.length - 1;
				if(next >= row.length) next = 0;
				setFocus(row[next]);
				return true;
			}
		}
		return false;
	}

	private void handleGlobalKey(NetLobbyFrame nl, NullpoMinoSDL.KeyEvent ev) {
		if(ev.repeat) return;
		switch(ev.scancode) {
			case SDLConstants.SDL_SCANCODE_ESCAPE:
				if(focused == chatInput) chatInput.setText("");
				else NullpoMinoSDL.endNetplay();
				break;
			case SDLConstants.SDL_SCANCODE_RETURN:
			case SDLConstants.SDL_SCANCODE_KP_ENTER:
				// Enter on text/table fires the default action; buttons handle their
				// own activation via handleKey + action Runnable.
				if(focused == chatInput) sendChat(nl);
				else if(focused == roomTable) attemptJoinSelected(false);
				break;
			case SDLConstants.SDL_SCANCODE_TAB: {
				boolean shift = (ev.keymod & SDLConstants.SDL_KMOD_SHIFT) != 0;
				tryWidgetNav(shift);
				break;
			}
			case SDLConstants.SDL_SCANCODE_PAGEUP:
				nl.chatLogLobby.pageUp();
				break;
			case SDLConstants.SDL_SCANCODE_PAGEDOWN:
				nl.chatLogLobby.pageDown();
				break;
			default: break;
		}
	}

	private void refreshRoomTable(NetLobbyFrame nl) {
		// Rebuild rows only when the size differs or IDs shifted — cheap since
		// roomList is a LinkedList and row counts are typically small.
		if(roomTable.getRowCount() == nl.roomList.size()) {
			boolean same = true;
			for(int i = 0; i < nl.roomList.size(); i++) {
				String[] existing = roomTable.getRow(i);
				if(existing == null || !existing[0].equals(Integer.toString(nl.roomList.get(i).roomID))) { same = false; break; }
			}
			if(same) {
				// Still update mutable columns (player count, status) in place.
				for(int i = 0; i < nl.roomList.size(); i++) {
					roomTable.setRow(i, nl.createRoomListRowData(nl.roomList.get(i)));
				}
				return;
			}
		}
		roomTable.clear();
		for(NetRoomInfo r : nl.roomList) {
			roomTable.addRow(nl.createRoomListRowData(r));
		}
	}

	private void attemptJoinSelected(boolean watch) {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		int idx = roomTable.getSelectedIndex();
		if(idx < 0 || idx >= nl.roomList.size()) { statusLine = "Select a room"; return; }
		NetRoomInfo r = nl.roomList.get(idx);
		nl.joinRoom(r.roomID, watch);
	}

	private void enterCreateRoom(boolean onePlayer, boolean rated) {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		nl.currentViewDetailRoomID = -1;
		nl.createRoomSinglePlayer = onePlayer;
		nl.createRoomRated = rated;
		nl.createRoomStyle = 0;
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_CREATEROOM);
	}

	private void sendChat(NetLobbyFrame nl) {
		String msg = chatInput.getText().trim();
		if(msg.length() == 0) return;
		nl.sendChat(false, msg);
		chatInput.setText("");
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		NormalFontSDL.printFont(8, 8, "LOBBY", NormalFontSDL.COLOR_CYAN);
		if(nl.netPlayerClient != null) {
			NormalFontSDL.printFont(96, 8,
					NormalFontSDL.safeString(nl.netPlayerClient.getHost() + ":" + nl.netPlayerClient.getPort()),
					NormalFontSDL.COLOR_WHITE);
		}

		roomTable.render();
		joinBtn.render();
		watchBtn.render();
		createBtn.render();
		create1PBtn.render();
		createRatedBtn.render();
		rankingBtn.render();
		rulechangeBtn.render();
		disconnectBtn.render();

		// Chat log occupies the centre-left strip; player list is a column on the right.
		nl.chatLogLobby.x = 8;  nl.chatLogLobby.y = 298;
		nl.chatLogLobby.w = 480; nl.chatLogLobby.h = 84;
		nl.chatLogLobby.render();

		chatInput.render();
		sendBtn.render();

		// Online player list (right column)
		NormalFontSDL.printFont(496, 298, "ONLINE", NormalFontSDL.COLOR_YELLOW);
		int py = 314;
		if(nl.netPlayerClient != null) {
			LinkedList<NetPlayerInfo> list = new LinkedList<NetPlayerInfo>(nl.netPlayerClient.getPlayerInfoList());
			int shown = 0;
			for(NetPlayerInfo p : list) {
				if(shown >= 5) break;
				String name = NormalFontSDL.safeString(nl.getPlayerNameWithTripCode(p));
				if(name.length() > 8) name = name.substring(0, 8);
				NormalFontSDL.printFont(496, py, name, NormalFontSDL.COLOR_WHITE);
				py += 16;
				shown++;
			}
		}

		if(statusLine.length() > 0) {
			NormalFontSDL.printFont(8, 424, NormalFontSDL.safeString(statusLine), NormalFontSDL.COLOR_RED);
		}
	}
}
