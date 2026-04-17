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

import mu.nu.nullpo.game.net.NetPlayerClient;
import mu.nu.nullpo.gui.net.NetLobbyFrame;
import mu.nu.nullpo.gui.sdl.binding.SDL3;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.widget.ButtonSDL;
import mu.nu.nullpo.gui.sdl.widget.TableSDL;
import mu.nu.nullpo.gui.sdl.widget.TextInputSDL;
import mu.nu.nullpo.gui.sdl.widget.WidgetSDL;

/**
 * Server-select state: enters player name + team, picks a server from the list,
 * and connects.  This is the entry point to netplay — {@link #enter()} creates
 * the shared {@link NullpoMinoSDL#netLobby} session on first use.
 *
 * Also owns the "add server" sub-mode: when the user clicks Add, the TextInput
 * + button row is swapped for a server-address entry form inline (no separate
 * modal window).
 */
public class StateNetServerSelectSDL extends BaseStateSDL {
	private TextInputSDL nameInput;
	private TextInputSDL teamInput;
	private TableSDL serverTable;
	private ButtonSDL connectBtn;
	private ButtonSDL observeBtn;
	private ButtonSDL addBtn;
	private ButtonSDL deleteBtn;
	private ButtonSDL backBtn;

	// Add-server sub-mode
	private boolean adding;
	private TextInputSDL addServerInput;
	private ButtonSDL addOkBtn;
	private ButtonSDL addCancelBtn;

	private WidgetSDL focused;
	private String statusLine = "";

	@Override
	public void enter() {
		NullpoMinoSDL.stopObserverClient();
		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, "NullpoMino Netplay");

		if(NullpoMinoSDL.netLobby == null) {
			NullpoMinoSDL.netLobby = new NetLobbyFrame();
			NullpoMinoSDL.netLobby.init();
		}
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;

		nameInput = new TextInputSDL(16, 56, 608, 28);
		nameInput.maxChars = 32;
		nameInput.placeholder = "Player name";
		nameInput.setText(nl.propConfig.getProperty("serverselect.txtfldPlayerName.text", ""));

		teamInput = new TextInputSDL(16, 112, 608, 28);
		teamInput.maxChars = 24;
		teamInput.placeholder = "Team (optional)";
		teamInput.setText(nl.propConfig.getProperty("serverselect.txtfldPlayerTeam.text", ""));

		TableSDL.Column[] cols = { new TableSDL.Column("SERVER", 580) };
		serverTable = new TableSDL(16, 160, 608, 224, cols);
		refreshServerTable();

		int btnY = 398;
		connectBtn = new ButtonSDL( 16, btnY, 128, 32, "CONNECT", new Runnable() { public void run() { attemptConnect(false); } });
		connectBtn.primary = true;
		observeBtn = new ButtonSDL(148, btnY, 128, 32, "OBSERVE", new Runnable() { public void run() { attemptConnect(true); } });
		addBtn     = new ButtonSDL(280, btnY,  64, 32, "ADD",     new Runnable() { public void run() { openAddServer(); } });
		deleteBtn  = new ButtonSDL(348, btnY, 112, 32, "DELETE",  new Runnable() { public void run() { deleteSelectedServer(); } });
		backBtn    = new ButtonSDL(556, btnY,  68, 32, "BACK",    new Runnable() { public void run() { NullpoMinoSDL.endNetplay(); } });

		addServerInput = new TextInputSDL(16, 398, 400, 32);
		addServerInput.placeholder = "host:port";
		addServerInput.maxChars = 64;
		addOkBtn     = new ButtonSDL(420, 398, 80, 32, "OK",     new Runnable() { public void run() { commitAddServer(); } });
		addOkBtn.primary = true;
		addCancelBtn = new ButtonSDL(504, 398, 120, 32, "CANCEL", new Runnable() { public void run() { cancelAddServer(); } });

		adding = false;
		// If the player hasn't set a name yet, focus the name field so they can type it
		// immediately; otherwise focus the server list so arrow keys navigate by default.
		setFocus(nameInput.getText().length() == 0 ? (WidgetSDL)nameInput : (WidgetSDL)serverTable);
		statusLine = "";
	}

	@Override
	public void leave() {
		setFocus(null);
		NullpoMinoSDL.stopTextInput();
	}

	private void refreshServerTable() {
		serverTable.clear();
		for(String s : NullpoMinoSDL.netLobby.serverList) serverTable.addRow(new String[] { s });
		String preferred = NullpoMinoSDL.netLobby.propConfig.getProperty("serverselect.listboxServerList.value", "");
		if(preferred != null && preferred.length() > 0) {
			for(int i = 0; i < NullpoMinoSDL.netLobby.serverList.size(); i++) {
				if(preferred.equals(NullpoMinoSDL.netLobby.serverList.get(i))) {
					serverTable.setSelectedIndex(i);
					break;
				}
			}
		} else if(serverTable.getRowCount() > 0) {
			serverTable.setSelectedIndex(0);
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
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE); return; }
		nl.pump();
		MouseInputSDL.mouseInput.update();

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		if(adding) {
			if(addServerInput.update(mx, my, clicked)) setFocus(addServerInput);
			addOkBtn.update(mx, my, clicked);      // action runs on click
			addCancelBtn.update(mx, my, clicked);  // action runs on click
		} else {
			if(nameInput.update(mx, my, clicked))   setFocus(nameInput);
			if(teamInput.update(mx, my, clicked))   setFocus(teamInput);
			if(serverTable.update(mx, my, clicked)) setFocus(serverTable);
			if(serverTable.activated)               attemptConnect(false);

			// Button actions are wired in enter() and fire from ButtonSDL itself on click.
			connectBtn.update(mx, my, clicked);
			observeBtn.update(mx, my, clicked);
			addBtn.update(mx, my, clicked);
			deleteBtn.update(mx, my, clicked);
			backBtn.update(mx, my, clicked);
		}

		// Deliver typed text and key events to the focused widget.  UP/DOWN act as
		// widget-to-widget navigation with boundary jumps: inside the server table
		// they scroll rows, but at the top/bottom row (or inside a text field where
		// they do nothing useful) they move focus to the adjacent widget.
		// PAGEUP/PAGEDOWN/HOME/END always drive the list for quick jumps.
		String typed = NullpoMinoSDL.consumeTextInput();
		if(focused != null && typed.length() > 0) focused.handleTextInput(typed);
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			handleGlobalKey(ev);
			if(adding) {
				if(focused != null) focused.handleKey(ev);
				continue;
			}
			if(isListJumpKey(ev)) {
				serverTable.handleKey(ev);
				continue;
			}
			boolean up    = ev.scancode == SDLConstants.SDL_SCANCODE_UP;
			boolean down  = ev.scancode == SDLConstants.SDL_SCANCODE_DOWN;
			boolean left  = ev.scancode == SDLConstants.SDL_SCANCODE_LEFT;
			boolean right = ev.scancode == SDLConstants.SDL_SCANCODE_RIGHT;

			// LEFT/RIGHT cycle within the button row; text fields still get them for caret movement.
			if((left || right) && !ev.repeat && tryButtonRowNav(left)) continue;

			// UP/DOWN cycle widget rows (with boundary behaviour on the server table).
			if((up || down) && !ev.repeat && tryWidgetNav(up)) continue;
			if((up || down) && ev.repeat && focused == serverTable) {
				serverTable.handleKey(ev);
				continue;
			}
			if(focused != null) focused.handleKey(ev);
		}
	}

	private void openAddServer() {
		adding = true;
		addServerInput.setText("");
		setFocus(addServerInput);
	}

	private void cancelAddServer() {
		adding = false;
		setFocus(serverTable);
	}

	/**
	 * Return the ordered button row (row 3) for navigation.  Built fresh each
	 * call — cheap and keeps the order explicit.
	 */
	private ButtonSDL[] buttonRow() {
		return new ButtonSDL[] { connectBtn, observeBtn, addBtn, deleteBtn, backBtn };
	}

	/**
	 * Move focus to the previous/next widget in the
	 * nameInput → teamInput → serverTable → [button row] → (wrap) cycle.
	 * When on the server table, only jumps at the top/bottom row; otherwise
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
			setFocus(up ? nameInput : serverTable);
			return true;
		}
		if(focused == serverTable) {
			int sel = serverTable.getSelectedIndex();
			int rows = serverTable.getRowCount();
			if(up && sel <= 0) { setFocus(teamInput); return true; }
			if(!up && (rows == 0 || sel >= rows - 1)) { setFocus(row[0]); return true; }
			return false;
		}
		for(ButtonSDL b : row) {
			if(focused == b) {
				setFocus(up ? serverTable : nameInput);
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
		if(ev.scancode == SDLConstants.SDL_SCANCODE_TAB && !ev.repeat && !adding) {
			boolean shift = (ev.keymod & SDLConstants.SDL_KMOD_SHIFT) != 0;
			tryWidgetNav(shift);
		}
		// Enter in the server table → connect. (Buttons handle Enter themselves via action.)
		if(!adding && (ev.scancode == SDLConstants.SDL_SCANCODE_RETURN || ev.scancode == SDLConstants.SDL_SCANCODE_KP_ENTER)
				&& !ev.repeat && focused == serverTable) {
			attemptConnect(false);
		}
		if(!adding && ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
			NullpoMinoSDL.endNetplay();
		}
		if(adding && ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
			cancelAddServer();
		}
	}

	private void attemptConnect(boolean observer) {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		String name = nameInput.getText().trim();
		if(name.length() == 0) { statusLine = "Enter a name first"; return; }

		int idx = serverTable.getSelectedIndex();
		if(idx < 0 || idx >= nl.serverList.size()) { statusLine = "Select a server"; return; }
		String server = nl.serverList.get(idx);

		int portSplit = server.indexOf(':');
		String host = portSplit == -1 ? server : server.substring(0, portSplit);
		int port = NetPlayerClient.DEFAULT_PORT;
		if(portSplit != -1) {
			try { port = Integer.parseInt(server.substring(portSplit + 1).trim()); }
			catch(NumberFormatException ignore) { statusLine = "Bad port in " + server; return; }
		}

		nl.propConfig.setProperty("serverselect.listboxServerList.value", server);

		if(observer) {
			// Observer mode: the NetObserverClient polls the title screen's
			// observer.cfg and starts a read-only stream on re-entry to STATE_TITLE.
			// Persist the server choice + enable flag, then bail back.
			if(nl.propObserver == null) nl.propObserver = new mu.nu.nullpo.util.CustomProperties();
			nl.propObserver.setProperty("observer.enable", true);
			nl.propObserver.setProperty("observer.host", host);
			nl.propObserver.setProperty("observer.port", port);
			try {
				java.io.FileOutputStream out = new java.io.FileOutputStream("config/setting/netobserver.cfg");
				nl.propObserver.store(out, "NullpoMino Netplay Observer Config");
				out.close();
			} catch(java.io.IOException e) {
				statusLine = "FAILED TO SAVE OBSERVER CONFIG";
				return;
			}
			NullpoMinoSDL.endNetplay();  // Returns to the title; startObserverClient runs there.
			return;
		}

		nl.connectToServer(name, teamInput.getText(), host, port);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
	}

	private void commitAddServer() {
		String s = addServerInput.getText().trim();
		if(s.length() == 0) return;
		NullpoMinoSDL.netLobby.serverList.add(s);
		NullpoMinoSDL.netLobby.saveServerList();
		refreshServerTable();
		adding = false;
		setFocus(serverTable);
	}

	private void deleteSelectedServer() {
		int idx = serverTable.getSelectedIndex();
		if(idx < 0 || idx >= NullpoMinoSDL.netLobby.serverList.size()) return;
		NullpoMinoSDL.netLobby.serverList.remove(idx);
		NullpoMinoSDL.netLobby.saveServerList();
		refreshServerTable();
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		// Header
		NormalFontSDL.printFont(16, 16, NormalFontSDL.safeString(nl.getUIText("Title_NetLobby")), NormalFontSDL.COLOR_CYAN);
		NormalFontSDL.printFont(16, 36, NormalFontSDL.safeString(nl.getUIText("ServerSelect_LabelName")), NormalFontSDL.COLOR_WHITE);
		nameInput.render();
		NormalFontSDL.printFont(16, 92, NormalFontSDL.safeString(nl.getUIText("ServerSelect_LabelTeam")), NormalFontSDL.COLOR_WHITE);
		teamInput.render();
		NormalFontSDL.printFont(16, 144, "SERVERS", NormalFontSDL.COLOR_WHITE);
		serverTable.render();

		if(adding) {
			addServerInput.render();
			addOkBtn.render();
			addCancelBtn.render();
		} else {
			connectBtn.render();
			observeBtn.render();
			addBtn.render();
			deleteBtn.render();
			backBtn.render();
		}

		if(statusLine.length() > 0) {
			NormalFontSDL.printFont(16, 440, NormalFontSDL.safeString(statusLine), NormalFontSDL.COLOR_RED);
		}
	}
}
