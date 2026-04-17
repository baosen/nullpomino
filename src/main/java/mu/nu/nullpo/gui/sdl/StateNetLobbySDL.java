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
	private ButtonSDL viewBtn;
	private ButtonSDL createBtn;
	private ButtonSDL create1PBtn;
	private ButtonSDL createRatedBtn;
	private ButtonSDL rankingBtn;
	private ButtonSDL rulechangeBtn;
	private ButtonSDL teamBtn;
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
		roomTable = new TableSDL(8, 28, 624, 190, cols);

		// Chat input along the bottom, aligned with the chat log's width. SEND
		// sits flush with the screen's right edge (x=632 matches the room table).
		chatInput = new TextInputSDL(8, 448, 540, 28);
		chatInput.maxChars = 255;
		chatInput.placeholder = "Chat...";
		final NetLobbyFrame nlf = nl;
		sendBtn = new ButtonSDL(552, 448, 80, 28, "SEND", new Runnable() { public void run() { sendChat(nlf); } });
		sendBtn.primary = true;

		// Action row aligned with the room table (x=8, w=624). Seven buttons
		// at 4 px gaps meet the table's right edge at x=632. WATCH isn't on
		// the lobby row — the VIEW screen has its own WATCH button that
		// joins a room as a spectator.
		int actY = 224;
		joinBtn       = new ButtonSDL(  8, actY,  80, 28, "JOIN",    new Runnable() { public void run() { attemptJoinSelected(); } });
		joinBtn.primary = true;
		viewBtn       = new ButtonSDL( 92, actY,  80, 28, "VIEW",    new Runnable() { public void run() { viewSelectedRoom(); } });
		createBtn     = new ButtonSDL(176, actY, 112, 28, "CREATE",  new Runnable() { public void run() { enterCreateRoom(false, false); } });
		create1PBtn   = new ButtonSDL(292, actY,  40, 28, "1P",      new Runnable() { public void run() { enterCreateRoom(true,  false); } });
		createRatedBtn= new ButtonSDL(336, actY,  80, 28, "RATED",   new Runnable() { public void run() { enterCreateRoom(false, true);  } });
		rankingBtn    = new ButtonSDL(420, actY, 112, 28, "RANKING", new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_RANKING); } });
		rulechangeBtn = new ButtonSDL(536, actY,  96, 28, "RULES",   new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_RULECHANGE); } });
		// Top-right corner: TEAM-change shortcut + disconnect X. TEAM prefills
		// the chat with '/team ' and focuses it; the user types a name and
		// hits Enter to submit. Disconnect's right edge matches the room
		// table (x=632).
		teamBtn       = new ButtonSDL(496,    4, 100, 24, "TEAM",    new Runnable() { public void run() { beginTeamChange(); } });
		disconnectBtn = new ButtonSDL(604,    4,  28, 24, "X",       new Runnable() { public void run() { NullpoMinoSDL.endNetplay(); } });

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
		if(roomTable.activated) attemptJoinSelected();
		if(chatInput.update(mx, my, clicked)) setFocus(chatInput);
		nl.chatLogLobby.update(mx, my, clicked);

		// Button actions are wired in enter() and fire from ButtonSDL itself on click.
		sendBtn.update(mx, my, clicked);
		joinBtn.update(mx, my, clicked);
		viewBtn.update(mx, my, clicked);
		createBtn.update(mx, my, clicked);
		create1PBtn.update(mx, my, clicked);
		createRatedBtn.update(mx, my, clicked);
		rankingBtn.update(mx, my, clicked);
		rulechangeBtn.update(mx, my, clicked);
		teamBtn.update(mx, my, clicked);
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

	/**
	 * Ordered action-button row used for LEFT/RIGHT keyboard navigation.
	 * The disconnect button (top-right corner) is intentionally excluded — it's
	 * reached via mouse or by pressing ESC, which is the lobby's built-in quit.
	 */
	private ButtonSDL[] buttonRow() {
		return new ButtonSDL[] { joinBtn, viewBtn, createBtn, create1PBtn, createRatedBtn,
				rankingBtn, rulechangeBtn };
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
				else if(focused == roomTable) attemptJoinSelected();
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

	private void attemptJoinSelected() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		int idx = roomTable.getSelectedIndex();
		if(idx < 0 || idx >= nl.roomList.size()) { statusLine = "Select a room"; return; }
		NetRoomInfo r = nl.roomList.get(idx);
		// Always join as a participant; spectators come in via the VIEW screen's WATCH button.
		nl.joinRoom(r.roomID, false);
	}

	/**
	 * Open the currently selected room in the CreateRoom form in read-only
	 * detail mode so the user can inspect its settings without joining.
	 */
	private void viewSelectedRoom() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		int idx = roomTable.getSelectedIndex();
		if(idx < 0 || idx >= nl.roomList.size()) { statusLine = "Select a room"; return; }
		NetRoomInfo r = nl.roomList.get(idx);
		nl.currentViewDetailRoomID = r.roomID;
		nl.createRoomSinglePlayer = false;
		nl.createRoomRated = false;
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_CREATEROOM);
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

	/**
	 * Prefill the chat input with '/team ' and focus it so the user can type
	 * a new team name and hit Enter. sendChat() strips the prefix and routes
	 * the value to the server via changeteam.
	 */
	private void beginTeamChange() {
		chatInput.setText("/team ");
		setFocus(chatInput);
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
		viewBtn.render();
		createBtn.render();
		create1PBtn.render();
		createRatedBtn.render();
		rankingBtn.render();
		rulechangeBtn.render();
		teamBtn.render();
		disconnectBtn.render();

		// Chat log fills the main bottom-left panel, matched in width to the
		// chat input directly below it.
		nl.chatLogLobby.x = 8;  nl.chatLogLobby.y = 258;
		nl.chatLogLobby.w = 540; nl.chatLogLobby.h = 186;
		nl.chatLogLobby.render();

		chatInput.render();
		sendBtn.render();

		// Online player list (right column) — narrower now that the chat panel
		// grew. Names clipped to 5 chars to fit the 80 px column.
		NormalFontSDL.printFont(552, 258, "USERS", NormalFontSDL.COLOR_YELLOW);
		int py = 274;
		if(nl.netPlayerClient != null) {
			LinkedList<NetPlayerInfo> list = new LinkedList<NetPlayerInfo>(nl.netPlayerClient.getPlayerInfoList());
			int shown = 0;
			for(NetPlayerInfo p : list) {
				if(shown >= 10) break;
				String name = NormalFontSDL.safeString(nl.getPlayerNameWithTripCode(p));
				if(name.length() > 5) name = name.substring(0, 5);
				NormalFontSDL.printFont(552, py, name, NormalFontSDL.COLOR_WHITE);
				py += 16;
				shown++;
			}
		}

		if(statusLine.length() > 0) {
			// Right-aligned on the title row so it doesn't steal vertical space
			// from the room list. Clamp so it can't slide under the LOBBY header.
			String safe = NormalFontSDL.safeString(statusLine);
			int tx = 600 - safe.length() * 16;   // leave room for the top-right X button (x=604)
			if(tx < 192) tx = 192;                // clear 'LOBBY' + host:port on the left
			NormalFontSDL.printFont(tx, 8, safe, NormalFontSDL.COLOR_RED);
		}
	}
}
