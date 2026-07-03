// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import nullpomino.game.net.NetLanDiscovery;
import nullpomino.game.net.room.RoomConfig;
import nullpomino.game.net.room.RoomProtocol;
import nullpomino.game.net.room.RoomSession;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.gui.sdl.widget.TableSDL;
import nullpomino.gui.sdl.widget.TextInputSDL;
import nullpomino.gui.sdl.widget.WidgetSDL;

/**
 * P2P session-select state: enters player name + team, then either CREATEs a
 * new room session (becoming its first arbiter), JOINs one discovered on the
 * LAN, or joins DIRECTly by host:port.  This is the entry point to netplay —
 * {@link #enter()} creates the shared {@link NullpoMinoSDL#netLobby} session
 * on first use, and both create and join land in the lobby (the room session
 * IS the lobby: room list, chat, everything downstream is unchanged).
 *
 * Also owns the "direct join" sub-mode: when the user clicks DIRECT, the
 * TextInput + button row is swapped for a host:port entry form inline.
 */
public class StateNetServerSelectSDL extends BaseStateSDL {
	private TextInputSDL nameInput;
	private TextInputSDL teamInput;
	private TableSDL sessionTable;
	private ButtonSDL createBtn;
	private ButtonSDL joinBtn;
	private ButtonSDL directBtn;
	private ButtonSDL backBtn;

	// Direct-join sub-mode
	private boolean directEntry;
	private TextInputSDL directInput;
	private ButtonSDL directOkBtn;
	private ButtonSDL directCancelBtn;

	/** LAN discovery listener, null when the UDP port couldn't be bound */
	private NetLanDiscovery.Listener lanListener;

	/** Room sessions currently shown in the table (deduped by session) */
	private List<NetLanDiscovery.Announce> roomRows = new ArrayList<NetLanDiscovery.Announce>();

	/** Change-detection key of the last snapshot rendered into the table */
	private String lanKey = "";

	private WidgetSDL focused;
	private String statusLine = "";

	@Override
	public void enter() {
		// A room session never outlives the netplay UI flow; a dead one
		// (disconnect bounce) is cleaned up here too
		NullpoMinoSDL.stopRoomSession();
		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, "NullpoMino P2P Netplay");

		if(NullpoMinoSDL.netLobby == null) {
			NullpoMinoSDL.netLobby = new NetLobbyFrame();
			NullpoMinoSDL.netLobby.init();
		}
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;

		// Every row keeps a uniform 4 px gap between widget bottoms and the
		// label below, and between labels and the widget they head.
		// Nickname label at y=36 → nameInput y=56 → Team label y=88 →
		// teamInput y=108 → SESSIONS label y=140 → sessionTable y=160.
		nameInput = new TextInputSDL(16, 56, 608, 28);
		nameInput.maxChars = 32;
		nameInput.placeholder = "Player name";
		nameInput.setText(nl.propConfig.getProperty("serverselect.txtfldPlayerName.text", ""));

		teamInput = new TextInputSDL(16, 108, 608, 28);
		teamInput.maxChars = 24;
		teamInput.setText(nl.propConfig.getProperty("serverselect.txtfldPlayerTeam.text", ""));

		TableSDL.Column[] cols = { new TableSDL.Column("SESSION", 580) };
		sessionTable = new TableSDL(16, 160, 608, 280, cols);
		sessionTable.showHeader = false;

		// Listen for sessions announced on the local network. Best-effort:
		// if the UDP port can't be bound the screen still allows DIRECT joins.
		roomRows = new ArrayList<NetLanDiscovery.Announce>();
		lanKey = "";
		try {
			lanListener = new NetLanDiscovery.Listener();
			lanListener.start();
		} catch (SocketException e) {
			lanListener = null;
		}
		refreshSessionTable();

		// Buttons pinned to the bottom (h=32, y=444 → ends at y=476, 4 px above
		// the 480 px logical viewport floor).
		int btnY = 444;
		createBtn = new ButtonSDL( 16, btnY, 128, 32, "CREATE", new Runnable() { public void run() { createSession(); } });
		createBtn.theme = ButtonSDL.THEME_GREEN;
		joinBtn   = new ButtonSDL(148, btnY,  96, 32, "JOIN",   new Runnable() { public void run() { joinSelected(); } });
		joinBtn.primary = true;
		directBtn = new ButtonSDL(248, btnY, 112, 32, "DIRECT", new Runnable() { public void run() { openDirectEntry(); } });
		backBtn   = new ButtonSDL(556, btnY,  68, 32, "BACK",   new Runnable() { public void run() { NullpoMinoSDL.endNetplay(); } });

		directInput = new TextInputSDL(16, btnY, 400, 32);
		directInput.placeholder = "host:port";
		directInput.maxChars = 64;
		directOkBtn     = new ButtonSDL(420, btnY,  80, 32, "OK",     new Runnable() { public void run() { joinDirect(); } });
		directOkBtn.primary = true;
		directCancelBtn = new ButtonSDL(504, btnY, 120, 32, "CANCEL", new Runnable() { public void run() { cancelDirectEntry(); } });

		directEntry = false;
		// If the player hasn't set a name yet, focus the name field so they can type it
		// immediately; otherwise focus the session list so arrow keys navigate by default.
		setFocus(nameInput.getText().length() == 0 ? (WidgetSDL)nameInput : (WidgetSDL)sessionTable);
		statusLine = "";
	}

	@Override
	public void leave() {
		if(lanListener != null) {
			lanListener.shutdown();
			lanListener = null;
		}
		setFocus(null);
		NullpoMinoSDL.stopTextInput();
	}

	private void refreshSessionTable() {
		// Remember the selected session so refreshes don't move the cursor
		String preferred = null;
		int sel = sessionTable.getSelectedIndex();
		if(sel >= 0 && sel < roomRows.size()) preferred = roomRows.get(sel).sessionId;

		sessionTable.clear();
		for(NetLanDiscovery.Announce a : roomRows) {
			sessionTable.addRow(new String[] {
				a.lobbyName + " - " + a.players + "P - " + a.playerName + " - " + a.hostPort()
			}, NormalFontSDL.COLOR_GREEN);
		}

		if(preferred != null) {
			for(int i = 0; i < roomRows.size(); i++) {
				if(preferred.equals(roomRows.get(i).sessionId)) {
					sessionTable.setSelectedIndex(i);
					break;
				}
			}
		} else if(sessionTable.getRowCount() > 0) {
			sessionTable.setSelectedIndex(0);
		}
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

		// Merge freshly discovered sessions into the table when the set changes
		if(lanListener != null) {
			List<NetLanDiscovery.Announce> snapshot = new ArrayList<NetLanDiscovery.Announce>();
			for(NetLanDiscovery.Announce a : lanListener.snapshot()) {
				if(a.room) snapshot.add(a);
			}
			snapshot = NetLanDiscovery.dedupeBySession(snapshot);
			StringBuilder key = new StringBuilder();
			for(NetLanDiscovery.Announce a : snapshot) {
				key.append(a.sessionId).append('/').append(a.players).append('/').append(a.hostPort()).append('\n');
			}
			if(!lanKey.equals(key.toString())) {
				lanKey = key.toString();
				roomRows = snapshot;
				refreshSessionTable();
			}
		}

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		// Mouse back button aliases Escape — exit the direct-join dialog if
		// it's open, otherwise walk back to the title via the shared back
		// stack. The BACK button remains the explicit endNetplay trigger.
		if(MouseInputSDL.mouseInput.isMouseBackClicked()) {
			if(directEntry) cancelDirectEntry();
			else { NullpoMinoSDL.goBack(); return; }
		}
		if(!directEntry && MouseInputSDL.mouseInput.isMouseForwardClicked()) {
			NullpoMinoSDL.goForward();
			return;
		}

		if(directEntry) {
			if(directInput.update(mx, my, clicked)) setFocus(directInput);
			directOkBtn.update(mx, my, clicked);      // action runs on click
			directCancelBtn.update(mx, my, clicked);  // action runs on click
		} else {
			if(nameInput.update(mx, my, clicked))    setFocus(nameInput);
			if(teamInput.update(mx, my, clicked))    setFocus(teamInput);
			if(sessionTable.update(mx, my, clicked)) setFocus(sessionTable);
			if(sessionTable.activated)               joinSelected();

			// Button actions are wired in enter() and fire from ButtonSDL itself on click.
			createBtn.update(mx, my, clicked);
			joinBtn.update(mx, my, clicked);
			directBtn.update(mx, my, clicked);
			backBtn.update(mx, my, clicked);
		}

		// Deliver typed text and key events to the focused widget.  UP/DOWN act as
		// widget-to-widget navigation with boundary jumps: inside the session table
		// they scroll rows, but at the top/bottom row (or inside a text field where
		// they do nothing useful) they move focus to the adjacent widget.
		// PAGEUP/PAGEDOWN/HOME/END always drive the list for quick jumps.
		String typed = NullpoMinoSDL.consumeTextInput();
		if(focused != null && typed.length() > 0) focused.handleTextInput(typed);
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			handleGlobalKey(ev);
			if(directEntry) {
				if(focused != null) focused.handleKey(ev);
				continue;
			}
			if(isListJumpKey(ev)) {
				sessionTable.handleKey(ev);
				continue;
			}
			boolean up    = ev.scancode == SDLConstants.SDL_SCANCODE_UP;
			boolean down  = ev.scancode == SDLConstants.SDL_SCANCODE_DOWN;
			boolean left  = ev.scancode == SDLConstants.SDL_SCANCODE_LEFT;
			boolean right = ev.scancode == SDLConstants.SDL_SCANCODE_RIGHT;

			// LEFT/RIGHT cycle within the button row; text fields still get them for caret movement.
			if((left || right) && !ev.repeat && tryButtonRowNav(left)) continue;

			// UP/DOWN cycle widget rows (with boundary behaviour on the session table).
			if((up || down) && !ev.repeat && tryWidgetNav(up)) continue;
			if((up || down) && ev.repeat && focused == sessionTable) {
				sessionTable.handleKey(ev);
				continue;
			}
			if(focused != null) focused.handleKey(ev);
		}
	}

	private void openDirectEntry() {
		directEntry = true;
		directInput.setText("");
		setFocus(directInput);
	}

	private void cancelDirectEntry() {
		directEntry = false;
		setFocus(sessionTable);
	}

	/**
	 * Return the ordered button row (row 3) for navigation.  Built fresh each
	 * call — cheap and keeps the order explicit.
	 */
	private ButtonSDL[] buttonRow() {
		return new ButtonSDL[] { createBtn, joinBtn, directBtn, backBtn };
	}

	/**
	 * Move focus to the previous/next widget in the
	 * nameInput → teamInput → sessionTable → [button row] → (wrap) cycle.
	 * When on the session table, only jumps at the top/bottom row; otherwise
	 * returns false so the table handles the key for row navigation.
	 *
	 * @param up true for UP, false for DOWN
	 * @return true if focus moved (caller should skip normal dispatch)
	 */
	private boolean tryWidgetNav(boolean up) {
		ButtonSDL[] row = buttonRow();
		if(focused == nameInput) {
			setFocus(up ? row[row.length - 1] : teamInput);
			return true;
		}
		if(focused == teamInput) {
			setFocus(up ? nameInput : sessionTable);
			return true;
		}
		if(focused == sessionTable) {
			int sel = sessionTable.getSelectedIndex();
			int rows = sessionTable.getRowCount();
			if(up && sel <= 0) { setFocus(teamInput); return true; }
			if(!up && (rows == 0 || sel >= rows - 1)) { setFocus(row[0]); return true; }
			return false;
		}
		for(ButtonSDL b : row) {
			if(focused == b) {
				setFocus(up ? sessionTable : nameInput);
				return true;
			}
		}
		return false;
	}

	/**
	 * LEFT/RIGHT navigation within the button row.  Returns true if handled;
	 * otherwise (e.g. we're on a text field that uses LEFT/RIGHT for caret
	 * movement) the caller falls back to normal dispatch.
	 */
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

	private static boolean isListJumpKey(NullpoMinoSDL.KeyEvent ev) {
		return ev.scancode == SDLConstants.SDL_SCANCODE_PAGEUP
				|| ev.scancode == SDLConstants.SDL_SCANCODE_PAGEDOWN
				|| ev.scancode == SDLConstants.SDL_SCANCODE_HOME
				|| ev.scancode == SDLConstants.SDL_SCANCODE_END;
	}

	private void handleGlobalKey(NullpoMinoSDL.KeyEvent ev) {
		// TAB acts like DOWN — cycles focus forward through every widget row.
		if(ev.scancode == SDLConstants.SDL_SCANCODE_TAB && !ev.repeat && !directEntry) {
			boolean shift = (ev.keymod & SDLConstants.SDL_KMOD_SHIFT) != 0;
			tryWidgetNav(shift);
		}
		// Enter in the session table → join. (Buttons handle Enter themselves via action.)
		if(!directEntry && (ev.scancode == SDLConstants.SDL_SCANCODE_RETURN || ev.scancode == SDLConstants.SDL_SCANCODE_KP_ENTER)
				&& !ev.repeat && focused == sessionTable) {
			joinSelected();
		}
		if(!directEntry && ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
			NullpoMinoSDL.goBack();
		}
		if(directEntry && ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
			cancelDirectEntry();
		}
	}

	/** CREATE: start a new room session as its first arbiter and enter the lobby */
	private void createSession() {
		String name = nameInput.getText().trim();
		if(name.length() == 0) { statusLine = "Enter a name first"; return; }

		RoomSession session;
		try {
			session = RoomSession.create(name, RoomConfig.load(), null);
		} catch(IOException e) {
			statusLine = "CREATE FAILED: " + e.getMessage();
			return;
		}
		enterSession(session, name);
	}

	/** JOIN: connect to the session selected in the table */
	private void joinSelected() {
		int idx = sessionTable.getSelectedIndex();
		if(idx < 0 || idx >= roomRows.size()) { statusLine = "Select a session"; return; }
		NetLanDiscovery.Announce a = roomRows.get(idx);
		startJoin(a.address, a.port);
	}

	/** DIRECT: connect by hand-typed host:port (for firewalled/remote sessions) */
	private void joinDirect() {
		String s = directInput.getText().trim();
		if(s.length() == 0) return;

		int portSplit = s.indexOf(':');
		String host = portSplit == -1 ? s : s.substring(0, portSplit);
		int port = RoomProtocol.DEFAULT_PORT;
		if(portSplit != -1) {
			try { port = Integer.parseInt(s.substring(portSplit + 1).trim()); }
			catch(NumberFormatException ignore) { statusLine = "Bad port in " + s; return; }
		}
		directEntry = false;
		startJoin(host, port);
	}

	private void startJoin(String host, int port) {
		String name = nameInput.getText().trim();
		if(name.length() == 0) { statusLine = "Enter a name first"; return; }

		RoomSession session;
		try {
			session = RoomSession.join(host, port, name, RoomConfig.load(), null);
		} catch(IOException e) {
			statusLine = "JOIN FAILED: " + e.getMessage();
			return;
		}
		enterSession(session, name);
	}

	private void enterSession(RoomSession session, String name) {
		NullpoMinoSDL.roomSession = session;
		NullpoMinoSDL.netLobby.connectToRoom(name, teamInput.getText(), session);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		// Share the menu.png background with Mode Select so the whole netplay
		// flow sits on a consistent backdrop (in-game excluded — it renders its
		// own field chrome).
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		// Header
		NormalFontSDL.printFont(16, 16, NormalFontSDL.safeString(nl.getUIText("Title_NetLobby")), NormalFontSDL.COLOR_CYAN);
		NormalFontSDL.printFont(16, 36, NormalFontSDL.safeString(nl.getUIText("ServerSelect_LabelName")), NormalFontSDL.COLOR_WHITE);
		nameInput.render();
		NormalFontSDL.printFont(16, 88, NormalFontSDL.safeString(nl.getUIText("ServerSelect_LabelTeam")), NormalFontSDL.COLOR_WHITE);
		teamInput.render();
		NormalFontSDL.printFont(16, 140, "P2P SESSIONS", NormalFontSDL.COLOR_WHITE);
		sessionTable.render();

		if(sessionTable.getRowCount() == 0) {
			NormalFontSDL.printFont(32, 180, "NO SESSIONS FOUND ON LAN", NormalFontSDL.COLOR_DARKBLUE);
			NormalFontSDL.printFont(32, 200, "CREATE ONE OR JOIN BY ADDRESS", NormalFontSDL.COLOR_DARKBLUE);
		}

		if(directEntry) {
			directInput.render();
			directOkBtn.render();
			directCancelBtn.render();
		} else {
			createBtn.render();
			joinBtn.render();
			directBtn.render();
			backBtn.render();
		}

		if(statusLine.length() > 0) {
			// Right-aligned on the title row so the message doesn't overlap
			// the NETPLAY header or the Nickname label below it.
			String safe = NormalFontSDL.safeString(statusLine);
			int tx = 632 - safe.length() * 16;
			if(tx < 136) tx = 136;  // clear the 'NETPLAY' header
			NormalFontSDL.printFont(tx, 16, safe, NormalFontSDL.COLOR_RED);
		}
	}
}
