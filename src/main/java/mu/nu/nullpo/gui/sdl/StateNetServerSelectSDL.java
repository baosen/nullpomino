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
		connectBtn = new ButtonSDL(16,  btnY, 108, 32, "CONNECT");
		connectBtn.primary = true;
		observeBtn = new ButtonSDL(128, btnY, 108, 32, "OBSERVE");
		addBtn     = new ButtonSDL(240, btnY,  80, 32, "ADD");
		deleteBtn  = new ButtonSDL(324, btnY,  80, 32, "DELETE");
		backBtn    = new ButtonSDL(540, btnY,  84, 32, "BACK");

		addServerInput = new TextInputSDL(16, 398, 400, 32);
		addServerInput.placeholder = "host:port";
		addServerInput.maxChars = 64;
		addOkBtn     = new ButtonSDL(420, 398, 80, 32, "OK");
		addOkBtn.primary = true;
		addCancelBtn = new ButtonSDL(504, 398, 120, 32, "CANCEL");

		adding = false;
		setFocus(nameInput);
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

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		if(adding) {
			if(addServerInput.update(mx, my, clicked)) setFocus(addServerInput);
			if(addOkBtn.update(mx, my, clicked)) commitAddServer();
			if(addCancelBtn.update(mx, my, clicked)) { adding = false; setFocus(serverTable); }
		} else {
			if(nameInput.update(mx, my, clicked))   setFocus(nameInput);
			if(teamInput.update(mx, my, clicked))   setFocus(teamInput);
			if(serverTable.update(mx, my, clicked)) setFocus(serverTable);
			if(serverTable.activated)               attemptConnect(false);

			if(connectBtn.update(mx, my, clicked)) attemptConnect(false);
			if(observeBtn.update(mx, my, clicked)) attemptConnect(true);
			if(addBtn.update(mx, my, clicked))     { adding = true; addServerInput.setText(""); setFocus(addServerInput); }
			if(deleteBtn.update(mx, my, clicked))  deleteSelectedServer();
			if(backBtn.update(mx, my, clicked))    NullpoMinoSDL.endNetplay();
		}

		// Deliver typed text and key events to the focused widget.
		String typed = NullpoMinoSDL.consumeTextInput();
		if(focused != null && typed.length() > 0) focused.handleTextInput(typed);
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			handleGlobalKey(ev);
			if(focused != null) focused.handleKey(ev);
		}
	}

	private void handleGlobalKey(NullpoMinoSDL.KeyEvent ev) {
		// Tab cycles focus between name → team → server table → back to name.
		if(ev.scancode == SDLConstants.SDL_SCANCODE_TAB && !ev.repeat && !adding) {
			if(focused == nameInput)       setFocus(teamInput);
			else if(focused == teamInput)  setFocus(serverTable);
			else                           setFocus(nameInput);
		}
		// Enter in the server table → connect.
		if(!adding && (ev.scancode == SDLConstants.SDL_SCANCODE_RETURN || ev.scancode == SDLConstants.SDL_SCANCODE_KP_ENTER)
				&& !ev.repeat && focused == serverTable) {
			attemptConnect(false);
		}
		if(!adding && ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
			NullpoMinoSDL.endNetplay();
		}
		if(adding && ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
			adding = false;
			setFocus(serverTable);
		}
		if(adding && (ev.scancode == SDLConstants.SDL_SCANCODE_RETURN || ev.scancode == SDLConstants.SDL_SCANCODE_KP_ENTER)
				&& !ev.repeat) {
			commitAddServer();
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
			statusLine = "Observer connect not yet supported in SDL lobby";
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
