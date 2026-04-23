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
package nullpomino.gui.sdl;

import java.util.LinkedList;

import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.gui.sdl.widget.TableSDL;
import nullpomino.gui.sdl.widget.TextInputSDL;
import nullpomino.gui.sdl.widget.WidgetSDL;

/**
 * Lobby state: room browser + lobby chat + online player list.  Replaces the
 * "Lobby tab" of the Swing NetLobbyFrame.
 */
public class StateNetLobbySDL extends BaseStateSDL {
	private TableSDL roomTable;
	private TextInputSDL chatInput;
	private ButtonSDL joinBtn;
	private ButtonSDL viewBtn;
	private ButtonSDL createBtn;
	private ButtonSDL rankingBtn;
	private ButtonSDL rulechangeBtn;
	private ButtonSDL disconnectBtn;

	private WidgetSDL focused;
	private String statusLine = "";

	@Override
	public void enter() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_NET_SERVERSELECT); return; }

		// ID dropped — the roomID isn't useful to a human browsing the list
		// (the name + mode say more) and freeing its column gives NAME and
		// MODE the room they need for realistic values.
		TableSDL.Column[] cols = {
			new TableSDL.Column("NAME",  144),
			new TableSDL.Column("RATED",  84),
			new TableSDL.Column("RULE",   84),
			new TableSDL.Column("MODE",  108),
			new TableSDL.Column("STATUS",100),
			new TableSDL.Column("PLY",    52),
			new TableSDL.Column("SPC",    52),
		};
		roomTable = new TableSDL(8, 28, 624, 190, cols);

		// Chat input along the bottom, aligned with the chat log's width.
		// Enter sends the message (handled in handleGlobalKey), so no explicit
		// SEND button — that frees the right column at y=448 for the USERS
		// list to extend further down.
		chatInput = new TextInputSDL(8, 448, 540, 28);
		chatInput.maxChars = 255;
		chatInput.placeholder = "Type and press Enter to send...";

		// Action row aligned with the room table (x=8, w=624). Buttons are
		// visually grouped by purpose with extra spacing between groups:
		//   [JOIN VIEW] | [CREATE] | [RULES RANKING]
		// Intra-group gap = 4 px, inter-group gap = 16 px. The create-room
		// flavour (multiplayer / 1P / rated) lives inside the form itself
		// as a MODE TYPE selector, so one button opens the form for all
		// three flavours.
		int actY = 224;
		// Each group gets its own colour theme so buttons inside a group
		// match and adjacent groups don't. Themes: join = blue, create =
		// green, other = violet.
		joinBtn       = new ButtonSDL(  8, actY,  80, 28, "JOIN",    new Runnable() { public void run() { attemptJoinSelected(); } });
		joinBtn.theme = ButtonSDL.THEME_BLUE;
		viewBtn       = new ButtonSDL( 92, actY,  80, 28, "VIEW",    new Runnable() { public void run() { viewSelectedRoom(); } });
		viewBtn.theme = ButtonSDL.THEME_BLUE;

		createBtn     = new ButtonSDL(188, actY, 196, 28, "CREATE",  new Runnable() { public void run() { enterCreateRoom(); } });
		createBtn.theme = ButtonSDL.THEME_GREEN;

		rulechangeBtn = new ButtonSDL(400, actY,  96, 28, "RULES",   new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_RULECHANGE); } });
		rulechangeBtn.theme = ButtonSDL.THEME_VIOLET;
		rankingBtn    = new ButtonSDL(500, actY, 128, 28, "RANKING", new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_RANKING); } });
		rankingBtn.theme = ButtonSDL.THEME_VIOLET;
		// Top-right corner: disconnect X. Team changes are available via the
		// '/team <name>' chat command (see NetLobbyFrame.sendChat). Right edge
		// matches the room table (x=632); bottom edge (y=24) matches the
		// LOBBY header's baseline so the X has the same 4 px gap to the room
		// list that the header does.
		disconnectBtn = new ButtonSDL(604,    4,  28, 20, "X",       new Runnable() { public void run() { NullpoMinoSDL.endNetplay(); } });

		// Default focus goes on the chat input so a user can type right away;
		// UP arrow / click on a room switches to room navigation.
		setFocus(chatInput);
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
		if(nl == null) { NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE); return; }
		nl.pump();
		MouseInputSDL.mouseInput.update();

		// If the session has disconnected (pump may have set lobbyMode), bail back
		// to server-select — but give a short grace window after a /name-style
		// reconnect so the handshake has time to complete without bouncing us out.
		if(nl.netPlayerClient == null
				|| (!nl.netPlayerClient.isConnected()
					&& System.currentTimeMillis() - nl.lastConnectAt > 5000)) {
			NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_NET_SERVERSELECT);
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

		// Mouse back button aliases Escape → goBack walks to server-select
		// (where we came from), same as every other screen's cancel gesture.
		// The top-right X button remains the explicit "disconnect" action.
		if(MouseInputSDL.mouseInput.isMouseBackClicked()) {
			NullpoMinoSDL.goBack();
			return;
		}

		if(roomTable.update(mx, my, clicked)) setFocus(roomTable);
		if(roomTable.activated) attemptJoinSelected();
		if(chatInput.update(mx, my, clicked)) setFocus(chatInput);
		nl.chatLogLobby.update(mx, my, clicked);

		// Button actions are wired in enter() and fire from ButtonSDL itself on click.
		joinBtn.update(mx, my, clicked);
		viewBtn.update(mx, my, clicked);
		createBtn.update(mx, my, clicked);
		rulechangeBtn.update(mx, my, clicked);
		rankingBtn.update(mx, my, clicked);
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
		return new ButtonSDL[] { joinBtn, viewBtn, createBtn, rulechangeBtn, rankingBtn };
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
				NullpoMinoSDL.goBack();
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

	/**
	 * Translate the 8-column {@code createRoomListRowData} output (which still
	 * includes the ID at index 0) into the 7-column schema the lobby table
	 * displays.  ID is dropped and name becomes the first visible column.
	 */
	private static String[] rowFromRoom(NetLobbyFrame nl, NetRoomInfo r) {
		String[] full = nl.createRoomListRowData(r);
		return new String[] { full[1], full[2], full[3], full[4], full[5], full[6], full[7] };
	}

	private void refreshRoomTable(NetLobbyFrame nl) {
		// If the visible rooms are identical in count + ID order, just refresh
		// the data cells in place so scrollbar + selection don't flicker.
		if(roomTable.getRowCount() == nl.roomList.size()) {
			boolean same = true;
			for(int i = 0; i < nl.roomList.size(); i++) {
				if(rowRoomID(i) != nl.roomList.get(i).roomID) { same = false; break; }
			}
			if(same) {
				for(int i = 0; i < nl.roomList.size(); i++) {
					roomTable.setRow(i, rowFromRoom(nl, nl.roomList.get(i)));
				}
				return;
			}
		}

		// Rebuild from scratch but preserve selection by roomID so the
		// highlight doesn't jump when rooms are added/removed.
		int prevSel = roomTable.getSelectedIndex();
		int selRoomID = (prevSel >= 0 && prevSel < rowRoomIDs.size()) ? rowRoomIDs.get(prevSel) : -1;

		roomTable.clear();
		rowRoomIDs.clear();
		for(NetRoomInfo r : nl.roomList) {
			roomTable.addRow(rowFromRoom(nl, r));
			rowRoomIDs.add(r.roomID);
		}
		if(selRoomID != -1) {
			for(int i = 0; i < rowRoomIDs.size(); i++) {
				if(rowRoomIDs.get(i) == selRoomID) { roomTable.setSelectedIndex(i); break; }
			}
		}
	}

	/** Parallel array of roomIDs matching each row in {@link #roomTable}. */
	private final java.util.ArrayList<Integer> rowRoomIDs = new java.util.ArrayList<Integer>();

	private int rowRoomID(int i) {
		return (i >= 0 && i < rowRoomIDs.size()) ? rowRoomIDs.get(i) : -1;
	}

	private void attemptJoinSelected() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		int idx = roomTable.getSelectedIndex();
		if(idx < 0 || idx >= nl.roomList.size()) { statusLine = "Select a room"; return; }
		NetRoomInfo r = nl.roomList.get(idx);
		// Always join as a participant; spectators come in via the VIEW screen's WATCH button.
		nl.joinRoom(r.roomID, false);
	}

	/** Draw a short dim vertical divider (2x2 dots stacked) between button groups. */
	private static void drawGroupSeparator(int x, int y, int h) {
		com.sun.jna.Pointer rnd = NullpoMinoSDL.renderer;
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(rnd, SDLConstants.SDL_BLENDMODE_BLEND);
		SDL3.setDrawColor(rnd, 140, 140, 160, 160);
		SDL3.INSTANCE.SDL_RenderFillRect(rnd,
				new nullpomino.gui.sdl.binding.SDLStructs.SDL_FRect(x, y + 6, 2, h - 12));
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
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_CREATEROOM);
	}

	private void enterCreateRoom() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		nl.currentViewDetailRoomID = -1;
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

		// Share the menu.png background with Mode Select so the whole netplay
		// flow sits on a consistent backdrop (in-game excluded — it renders its
		// own field chrome).
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

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
		rulechangeBtn.render();
		rankingBtn.render();
		disconnectBtn.render();

		// Thin dim separator line between each button group so the visual
		// grouping reads at a glance. Placed in the middle of the inter-group
		// gaps (x=180 between VIEW|CREATE, x=392 between CREATE|RULES).
		drawGroupSeparator(180, 224, 28);
		drawGroupSeparator(392, 224, 28);

		// Chat log fills the main bottom-left panel, matched in width to the
		// chat input directly below it.
		nl.chatLogLobby.x = 8;  nl.chatLogLobby.y = 258;
		nl.chatLogLobby.w = 540; nl.chatLogLobby.h = 186;
		nl.chatLogLobby.render();

		chatInput.render();

		// Online player list (right column). The SEND button used to cap the
		// bottom at y=448; with it gone the column extends down through the
		// chat-input row (which only occupies x=8..548) to y=476, fitting 13
		// rows of 5-char names in the 80 px column.
		NormalFontSDL.printFont(552, 258, "USERS", NormalFontSDL.COLOR_YELLOW);
		int py = 274;
		if(nl.netPlayerClient != null) {
			LinkedList<NetPlayerInfo> list = new LinkedList<NetPlayerInfo>(nl.netPlayerClient.getPlayerInfoList());
			int shown = 0;
			for(NetPlayerInfo p : list) {
				if(shown >= 13) break;
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
