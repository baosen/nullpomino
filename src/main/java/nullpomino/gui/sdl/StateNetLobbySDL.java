// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import nullpomino.game.net.LoungeService;
import nullpomino.game.net.NetLanDiscovery;
import nullpomino.game.net.NetPlatform;
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
 * The netplay lounge — the direct entry point from the title menu.
 * Sessionless by default: the room table is fed from discovery beacons
 * (each room is its own P2P session; UDP broadcast on LAN, an MQTT topic on
 * web), and lobby chat reaches every peer on this screen the same way.
 * CREATE spins up a new room session and opens
 * the create-room form; JOIN/VIEW connect to the selected beacon's session
 * (VIEW as spectator); {@code /join host:port} in chat reaches rooms UDP
 * discovery can't (internet play). Errors and progress appear as colored
 * lines in the chat log.
 */
public class StateNetLobbySDL extends BaseStateSDL {
	private static final int PENDING_NONE = 0, PENDING_CREATE = 1, PENDING_JOIN = 2;

	/** Failsafe backstop over the room core's own 10s join timeout */
	private static final long PENDING_FAILSAFE_MS = 20000;

	private TextInputSDL nameInput;
	private TextInputSDL teamInput;
	private TableSDL roomTable;
	private TextInputSDL chatInput;
	private ButtonSDL joinBtn;
	private ButtonSDL viewBtn;
	private ButtonSDL createBtn;
	private ButtonSDL rankingBtn;
	private ButtonSDL rulechangeBtn;
	private ButtonSDL disconnectBtn;

	private WidgetSDL focused;

	/** Platform lounge (rooms/chat/presence); open only while on this screen */
	private LoungeService lounge;
	private boolean loungeOpen;

	/** Rooms currently shown in the table, index-parallel to its rows */
	private List<NetLanDiscovery.Announce> roomRows = new ArrayList<NetLanDiscovery.Announce>();

	/** Change-detection key of the last beacon snapshot rendered into the table */
	private String lanKey = "";

	/** What we're waiting on (guards double-clicks; survives the create-form detour) */
	private int pendingAction = PENDING_NONE;
	private boolean pendingWatch;
	private boolean autoRoomJoinSent;
	private long pendingSince;
	private String pendingHostPort = "";

	/** msgId source for outgoing lounge chat + presence instance ids */
	private final Random chatRand = new Random();

	/** Last input values mirrored into propConfig (eager persistence) */
	private String lastMirroredName;
	private String lastMirroredTeam;

	/** Presence identity while on this screen */
	private String presenceInstanceId = "";
	private String presenceName = "";

	@Override
	public void enter() {
		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, NullpoMinoSDL.GAME_NAME + " P2P Netplay");

		// The lounge is the netplay entry point: it owns the shared session object
		if(NullpoMinoSDL.netLobby == null) {
			NullpoMinoSDL.netLobby = new NetLobbyFrame();
			NullpoMinoSDL.netLobby.init();
		}
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;

		// Session reconciliation: a dead or absent client is the NORMAL lounge
		// state (fresh entry, post-game teardown, post-form-cancel). A LIVE
		// client means we're mid-flow (back from the create-room form awaiting
		// roomcreatesuccess) and everything - including pendingAction - is kept.
		boolean live = (nl.netPlayerClient != null) && nl.netPlayerClient.isConnected();
		if(!live) {
			resetToLounge(nl);
		}

		// Header strip y=4..28: NAME + TEAM inputs replace the old LOBBY header
		nameInput = new TextInputSDL(76, 4, 188, 24);
		nameInput.maxChars = 32;
		nameInput.placeholder = "Player name";
		nameInput.setText(nl.propConfig.getProperty("serverselect.txtfldPlayerName.text", ""));

		teamInput = new TextInputSDL(344, 4, 188, 24);
		teamInput.maxChars = 24;
		teamInput.setText(nl.propConfig.getProperty("serverselect.txtfldPlayerTeam.text", ""));

		TableSDL.Column[] cols = {
			new TableSDL.Column("NAME",         144, true),
			new TableSDL.Column("RULE",          84, true),
			new TableSDL.Column("MODE",         152, true),
			new TableSDL.Column("STATUS",       100, true),
			new TableSDL.Column("PLAYERS",       60, true),
			new TableSDL.Column("SPECTATORS",    84, true),
		};
		roomTable = new TableSDL(8, 32, 624, 188, cols);
		// Small header font (8px/char) fits full words in narrow columns
		// instead of abbreviating PLAYERS/SPECTATORS to PLY/SPC.
		roomTable.headerFontScale = 0.5f;

		// Chat input along the bottom; Enter sends (handled in handleGlobalKey).
		// The USERS column (fed by presence beacons) takes the right edge.
		chatInput = new TextInputSDL(8, 448, 540, 28);
		chatInput.maxChars = 255;
		chatInput.placeholder = "Type and press Enter to send...  (/help for commands)";

		// Action row aligned with the room table (x=8, w=624), grouped
		// [JOIN VIEW] | [CREATE] | [RULES RANKING] with themed colours.
		int actY = 224;
		joinBtn       = new ButtonSDL(  8, actY,  80, 28, "JOIN",    new Runnable() { public void run() { attemptJoinSelected(false); } });
		joinBtn.theme = ButtonSDL.THEME_BLUE;
		viewBtn       = new ButtonSDL( 92, actY,  80, 28, "VIEW",    new Runnable() { public void run() { attemptJoinSelected(true); } });
		viewBtn.theme = ButtonSDL.THEME_BLUE;

		createBtn     = new ButtonSDL(188, actY, 196, 28, "CREATE",  new Runnable() { public void run() { createRoom(); } });
		createBtn.theme = ButtonSDL.THEME_GREEN;

		rulechangeBtn = new ButtonSDL(400, actY,  96, 28, "RULES",   new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_RULECHANGE); } });
		rulechangeBtn.theme = ButtonSDL.THEME_VIOLET;
		rankingBtn    = new ButtonSDL(500, actY, 128, 28, "RANKING", new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_RANKING); } });
		rankingBtn.theme = ButtonSDL.THEME_VIOLET;

		// Top-right corner: the hard quit (tears down everything netplay)
		disconnectBtn = ButtonSDL.newCloseButton(new Runnable() { public void run() { NullpoMinoSDL.endNetplay(); } });

		// Listen for room beacons + lounge chat. Best-effort: if the lounge
		// can't open (UDP port busy / broker unreachable), CREATE and /join
		// still work on desktop.
		roomRows = new ArrayList<NetLanDiscovery.Announce>();
		lanKey = "";
		lounge = NetPlatform.lounge();
		lounge.setChatConsumer(new NetLanDiscovery.ChatConsumer() {
			public void onChat(String playerName, String message) {
				// ChatLogSDL appends are synchronized - safe from any thread
				NetLobbyFrame lobby = NullpoMinoSDL.netLobby;
				if(lobby != null) lobby.chatLogLobby.appendUser(playerName, Calendar.getInstance(), message);
			}
		});
		loungeOpen = lounge.open();
		if(!loungeOpen) {
			nl.chatLogLobby.appendSystem("LOUNGE UNAVAILABLE - CREATE OR /JOIN <HOST:PORT> STILL WORK",
				NormalFontSDL.COLOR_RED);
		}
		refreshRoomTable();

		// Announce our presence while on this screen so other lounges list us.
		// Blank names announce nothing.
		lastMirroredName = nameInput.getText();
		lastMirroredTeam = teamInput.getText();
		presenceName = nameInput.getText().trim();
		presenceInstanceId = Long.toHexString(chatRand.nextLong());
		lounge.setPresence(presenceName, presenceInstanceId);

		// New players type their name first; everyone else lands on the chat
		setFocus(nameInput.getText().length() == 0 ? (WidgetSDL)nameInput : (WidgetSDL)chatInput);
	}

	@Override
	public void leave() {
		if(lounge != null) {
			lounge.close();
			loungeOpen = false;
		}
		// Persist name/team for chat-only visitors too (connectToRoom also
		// writes them on use). NEVER touch the room session here - this also
		// runs on the CREATE-form and INROOM handoff transitions.
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl != null) {
			nl.propConfig.setProperty("serverselect.txtfldPlayerName.text", nameInput.getText());
			nl.propConfig.setProperty("serverselect.txtfldPlayerTeam.text", teamInput.getText());
			nl.saveConfig();
		}
		setFocus(null);
		NullpoMinoSDL.stopTextInput();
	}

	/** Back to the sessionless lounge: reap any session and clear pending state */
	private void resetToLounge(NetLobbyFrame nl) {
		NullpoMinoSDL.stopRoomSession();
		nl.netPlayerClient = null;
		nl.lobbyMode = NetLobbyFrame.LOBBYMODE_DISCONNECTED;
		nl.roomList.clear();
		pendingAction = PENDING_NONE;
		autoRoomJoinSent = false;
		pendingHostPort = "";
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

		// Mirror the inputs into config the moment they change (in-memory; the
		// file is written by leave()/shutdown()), so the nickname survives every
		// exit path - X, app quit, window close - not just ESC. Also feeds the
		// presence beacon.
		String nameNow = nameInput.getText();
		if(!nameNow.equals(lastMirroredName)) {
			lastMirroredName = nameNow;
			nl.propConfig.setProperty("serverselect.txtfldPlayerName.text", nameNow);
			presenceName = nameNow.trim();
			if(lounge != null) lounge.setPresence(presenceName, presenceInstanceId);
		}
		String teamNow = teamInput.getText();
		if(!teamNow.equals(lastMirroredTeam)) {
			lastMirroredTeam = teamNow;
			nl.propConfig.setProperty("serverselect.txtfldPlayerTeam.text", teamNow);
		}

		boolean haveClient = nl.netPlayerClient != null;

		// (a) roomjoinsuccess/roomcreatesuccess drained by pump(): hand off to the game
		if(haveClient && nl.lobbyMode == NetLobbyFrame.LOBBYMODE_INROOM) {
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NETGAME);
			return;
		}

		// (b) the session died: recover in place (dispatchDisconnect already
		// appended its own red line; add context if we were mid-action).
		// NOTE: lobbyMode stays DISCONNECTED until the login handshake finishes,
		// so it cannot distinguish "connecting" from "dead" - the seam's
		// isConnected() can (true from connectToRoom until the session closes).
		if(haveClient && !nl.netPlayerClient.isConnected()) {
			if(pendingAction == PENDING_JOIN) {
				nl.chatLogLobby.appendSystem("JOIN FAILED - CONNECTION LOST", NormalFontSDL.COLOR_RED);
			} else if(pendingAction == PENDING_CREATE) {
				nl.chatLogLobby.appendSystem("ROOM CREATE FAILED - CONNECTION LOST", NormalFontSDL.COLOR_RED);
			}
			resetToLounge(nl);
		}

		// (c) join flow: once login synthesis lands us in the session's lobby,
		// enter its (single) room exactly once
		if(pendingAction == PENDING_JOIN && haveClient && !autoRoomJoinSent
			&& nl.lobbyMode == NetLobbyFrame.LOBBYMODE_LOBBY && !nl.roomList.isEmpty()) {
			nl.joinRoom(nl.roomList.getFirst().roomID, pendingWatch);
			autoRoomJoinSent = true;
		}

		// (d) failsafe against pathological hangs (a session with no room)
		if(pendingAction != PENDING_NONE
			&& System.currentTimeMillis() - pendingSince > PENDING_FAILSAFE_MS) {
			nl.chatLogLobby.appendSystem(
				pendingAction == PENDING_JOIN ? "JOIN TIMED OUT" : "ROOM CREATE TIMED OUT",
				NormalFontSDL.COLOR_RED);
			resetToLounge(nl);
		}

		refreshRoomTable();

		// Guard double-clicks while a create/join is in flight
		boolean idle = (pendingAction == PENDING_NONE);
		joinBtn.enabled = idle;
		viewBtn.enabled = idle;
		createBtn.enabled = idle;

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		// Mouse back button aliases ESC: cancel a pending join in place,
		// otherwise walk back to the title. X remains the hard teardown.
		if(MouseInputSDL.mouseInput.isMouseBackClicked()) {
			if(pendingAction == PENDING_JOIN) cancelPendingJoin(nl);
			else { NullpoMinoSDL.goBack(); return; }
		}
		if(MouseInputSDL.mouseInput.isMouseForwardClicked()) {
			NullpoMinoSDL.goForward();
			return;
		}

		if(nameInput.update(mx, my, clicked)) setFocus(nameInput);
		if(teamInput.update(mx, my, clicked)) setFocus(teamInput);
		if(roomTable.update(mx, my, clicked)) setFocus(roomTable);
		if(roomTable.activated) attemptJoinSelected(false);
		if(chatInput.update(mx, my, clicked)) setFocus(chatInput);
		nl.chatLogLobby.update(mx, my, clicked);

		// Button actions are wired in enter() and fire from ButtonSDL itself on click.
		joinBtn.update(mx, my, clicked);
		viewBtn.update(mx, my, clicked);
		createBtn.update(mx, my, clicked);
		rulechangeBtn.update(mx, my, clicked);
		rankingBtn.update(mx, my, clicked);
		disconnectBtn.update(mx, my, clicked);

		// Deliver typed text and key events. UP/DOWN walk the widget cycle with
		// boundary jumps inside the room table; HOME/END go to the focused text
		// input's caret when one is focused, else jump the table; PAGEUP/PAGEDOWN
		// stay with the chat-log scroll in handleGlobalKey.
		String typed = NullpoMinoSDL.consumeTextInput();
		if(focused != null && typed.length() > 0) focused.handleTextInput(typed);
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			handleGlobalKey(nl, ev);
			if(ev.scancode == SDLConstants.SDL_SCANCODE_HOME || ev.scancode == SDLConstants.SDL_SCANCODE_END) {
				if(focused instanceof TextInputSDL) focused.handleKey(ev);
				else roomTable.handleKey(ev);
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
	 * The X button (top-right corner) is intentionally excluded - it's
	 * reached via mouse only.
	 */
	private ButtonSDL[] buttonRow() {
		return new ButtonSDL[] { joinBtn, viewBtn, createBtn, rulechangeBtn, rankingBtn };
	}

	/**
	 * Vertical nav cycle: nameInput → teamInput → roomTable → button row →
	 * chatInput → wrap. On the room table, UP/DOWN scroll rows until the
	 * boundary, then jump out.
	 */
	private boolean tryWidgetNav(boolean up) {
		ButtonSDL[] row = buttonRow();
		if(focused == nameInput) {
			setFocus(up ? chatInput : teamInput);
			return true;
		}
		if(focused == teamInput) {
			setFocus(up ? nameInput : roomTable);
			return true;
		}
		if(focused == roomTable) {
			int sel = roomTable.getSelectedIndex();
			int rows = roomTable.getRowCount();
			if(up && sel <= 0) { setFocus(teamInput); return true; }
			if(!up) {
				if(rows == 0) { setFocus(row[0]); return true; }
				if(sel >= rows - 1) { setFocus(row[0]); return true; }
			}
			return false;
		}
		for(ButtonSDL b : row) {
			if(focused == b) {
				setFocus(up ? roomTable : chatInput);
				return true;
			}
		}
		if(focused == chatInput) {
			setFocus(up ? row[0] : nameInput);
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
				if(pendingAction == PENDING_JOIN) cancelPendingJoin(nl);
				else NullpoMinoSDL.goBack();
				break;
			case SDLConstants.SDL_SCANCODE_RETURN:
			case SDLConstants.SDL_SCANCODE_KP_ENTER:
				// Enter on text/table fires the default action; buttons handle their
				// own activation via handleKey + action Runnable.
				if(focused == chatInput) sendChat(nl);
				else if(focused == roomTable) attemptJoinSelected(false);
				else if(focused == nameInput || focused == teamInput) tryWidgetNav(false);
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

	// ---------------- room table from beacons ----------------

	private void refreshRoomTable() {
		if(!loungeOpen) return;   // lounge unavailable: the table stays empty

		List<NetLanDiscovery.Announce> snap = lounge.snapshotRooms();

		// The key covers every displayed field + join identity, so any beacon
		// change triggers exactly one rebuild (beacons tick at 1.5s)
		StringBuilder key = new StringBuilder();
		for(NetLanDiscovery.Announce a : snap) {
			key.append(a.sessionId).append('/').append(a.hostPort()).append('/')
				.append(a.roomName).append('/').append(a.ruleName).append('/')
				.append(a.mode).append('/').append(a.playing).append('/').append(a.seated).append('/')
				.append(a.maxPlayers).append('/').append(a.spectators).append('\n');
		}
		if(lanKey.equals(key.toString())) return;
		lanKey = key.toString();

		// Rebuild, preserving the selection by sessionId
		int prevSel = roomTable.getSelectedIndex();
		String selSession = (prevSel >= 0 && prevSel < roomRows.size()) ? roomRows.get(prevSel).sessionId : null;

		roomRows = snap;
		roomTable.clear();
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		for(NetLanDiscovery.Announce a : snap) {
			roomTable.addRow(rowFromAnnounce(nl, a));
		}
		if(selSession != null) {
			for(int i = 0; i < roomRows.size(); i++) {
				if(selSession.equals(roomRows.get(i).sessionId)) { roomTable.setSelectedIndex(i); break; }
			}
		} else if(roomTable.getRowCount() > 0) {
			roomTable.setSelectedIndex(0);   // Enter-to-join works immediately
		}
	}

	/** Table row from a room beacon, mirroring the old createRoomListRowData columns */
	private static String[] rowFromAnnounce(NetLobbyFrame nl, NetLanDiscovery.Announce a) {
		return new String[] {
			a.roomName,
			a.ruleName.length() == 0 ? nl.getUIText("RoomTable_RuleName_Any") : a.ruleName.toUpperCase(),
			a.mode,
			nl.getUIText(a.playing ? "RoomTable_Status_Playing" : "RoomTable_Status_Waiting"),
			a.seated + "/" + a.maxPlayers,
			Integer.toString(a.spectators),
		};
	}

	// ---------------- create / join actions ----------------

	/** JOIN (participant) or VIEW (spectator) on the selected beacon row */
	private void attemptJoinSelected(boolean watch) {
		if(pendingAction != PENDING_NONE) return;
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		int idx = roomTable.getSelectedIndex();
		if(idx < 0 || idx >= roomRows.size()) {
			nl.chatLogLobby.appendSystem("SELECT A ROOM FIRST", NormalFontSDL.COLOR_RED);
			return;
		}
		NetLanDiscovery.Announce a = roomRows.get(idx);
		startJoin(a.address, a.port, watch);
	}

	/** Join a room session by address (beacon row or /join command) */
	private void startJoin(String host, int port, boolean watch) {
		if(pendingAction != PENDING_NONE) return;
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		String name = nameInput.getText().trim();
		if(name.length() == 0) {
			nl.chatLogLobby.appendSystem("ENTER A NAME FIRST", NormalFontSDL.COLOR_RED);
			setFocus(nameInput);
			return;
		}

		try {
			connectJoinedRoom(nl, host, port, name, teamInput.getText());
		} catch(IOException e) {
			nl.chatLogLobby.appendSystem("JOIN FAILED: " + e.getMessage(), NormalFontSDL.COLOR_RED);
			return;
		}
		pendingAction = PENDING_JOIN;
		pendingWatch = watch;
		autoRoomJoinSent = false;
		pendingSince = System.currentTimeMillis();
		pendingHostPort = host + ":" + port;
		nl.chatLogLobby.appendSystem("JOINING " + pendingHostPort + " ...", NormalFontSDL.COLOR_BLUE);
	}

	/** Join and attach a room transport; overridden by the headless state test seam. */
	protected void connectJoinedRoom(NetLobbyFrame nl, String host, int port, String playerName, String team)
			throws IOException {
		RoomSession session = RoomSession.join(host, port, playerName, RoomConfig.load(), null, NetPlatform.roomNet());
		NullpoMinoSDL.roomSession = session;
		nl.connectToRoom(playerName, team, session);
	}

	/** CREATE: open the create-room form; the form owns session creation on OK. */
	private void createRoom() {
		if(pendingAction != PENDING_NONE) return;
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		String name = nameInput.getText().trim();
		if(name.length() == 0) {
			nl.chatLogLobby.appendSystem("ENTER A NAME FIRST", NormalFontSDL.COLOR_RED);
			setFocus(nameInput);
			return;
		}

		// The form's OK creates the room session, sends roomcreate, waits for the
		// INROOM handoff and enters the game itself — so the create-room screen
		// stays on the back stack and in-game BACK returns here (not to the lounge).
		nl.currentViewDetailRoomID = -1;
		nl.createRoomStyle = 0;
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_CREATEROOM);
	}

	private void cancelPendingJoin(NetLobbyFrame nl) {
		nl.chatLogLobby.appendSystem("JOIN CANCELLED", NormalFontSDL.COLOR_RED);
		resetToLounge(nl);
	}

	// ---------------- chat ----------------

	private void sendChat(NetLobbyFrame nl) {
		String msg = chatInput.getText().trim();
		chatInput.setText("");
		if(msg.length() == 0) return;

		// /join host[:port] - reach rooms LAN discovery can't (internet play)
		String lower = msg.toLowerCase();
		if(lower.startsWith("/join ") || lower.equals("/join")) {
			String arg = lower.equals("/join") ? "" : msg.substring("/join ".length()).trim();
			if(arg.length() == 0) {
				nl.chatLogLobby.appendSystem("USAGE: /JOIN <HOST[:PORT]>", NormalFontSDL.COLOR_YELLOW);
				return;
			}
			int portSplit = arg.indexOf(':');
			String host = portSplit == -1 ? arg : arg.substring(0, portSplit);
			int port = RoomProtocol.DEFAULT_PORT;
			if(portSplit != -1) {
				try { port = Integer.parseInt(arg.substring(portSplit + 1).trim()); }
				catch(NumberFormatException e) {
					nl.chatLogLobby.appendSystem("BAD PORT IN " + arg, NormalFontSDL.COLOR_RED);
					return;
				}
			}
			startJoin(host, port, false);
			return;
		}

		boolean live = (nl.netPlayerClient != null) && nl.netPlayerClient.isConnected();
		if(live) {
			// Transiently in a session (about to enter a room): normal path
			nl.sendChat(false, msg);
			return;
		}

		// Sessionless lounge chat
		if(lower.startsWith("/name ") || lower.equals("/name")) {
			// Same effect as editing the NAME box (mirrored + announced by update())
			String arg = lower.equals("/name") ? "" : msg.substring("/name ".length()).trim();
			if(arg.length() == 0) {
				nl.chatLogLobby.appendSystem("USAGE: /NAME <NICKNAME>", NormalFontSDL.COLOR_YELLOW);
			} else {
				nameInput.setText(arg);
				nl.chatLogLobby.appendSystem("NAME SET TO " + arg, NormalFontSDL.COLOR_GREEN);
			}
			return;
		}
		if(lower.startsWith("/team")) {
			// Same effect as editing the TEAM box (read when creating/joining);
			// bare /team clears it
			String arg = msg.length() > 5 ? msg.substring(5).trim() : "";
			teamInput.setText(arg);
			nl.chatLogLobby.appendSystem(arg.length() == 0 ? "TEAM CLEARED" : "TEAM SET TO " + arg,
				NormalFontSDL.COLOR_GREEN);
			return;
		}
		if(lower.equals("/help") || lower.equals("/?")) {
			nl.chatLogLobby.appendSystem("COMMANDS:", NormalFontSDL.COLOR_YELLOW);
			nl.chatLogLobby.appendSystem("/JOIN <HOST[:PORT]> - JOIN A ROOM BY ADDRESS", NormalFontSDL.COLOR_YELLOW);
			nl.chatLogLobby.appendSystem("/NAME <NICK> - SET YOUR NICKNAME", NormalFontSDL.COLOR_YELLOW);
			nl.chatLogLobby.appendSystem("/TEAM [<NAME>] - SET OR CLEAR YOUR TEAM", NormalFontSDL.COLOR_YELLOW);
			nl.chatLogLobby.appendSystem("/HELP - SHOW THIS LIST", NormalFontSDL.COLOR_YELLOW);
			return;
		}
		if(msg.startsWith("/")) {
			// Don't broadcast command typos to the whole LAN
			nl.chatLogLobby.appendSystem("UNKNOWN COMMAND - TRY /HELP", NormalFontSDL.COLOR_YELLOW);
			return;
		}

		String name = nameInput.getText().trim();
		if(name.length() == 0) name = "???";
		// Local echo first (deterministic on every platform); the looped-back
		// copy dedupes away via the msgId the lounge pre-registers
		nl.chatLogLobby.appendUser(name, Calendar.getInstance(), msg);
		lounge.sendChat(name, msg);
	}

	/** Draw a short dim vertical divider (2x2 dots stacked) between button groups. */
	private static void drawGroupSeparator(int x, int y, int h) {
		nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer rnd = NullpoMinoSDL.renderer;
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(rnd, SDLConstants.SDL_BLENDMODE_BLEND);
		SDL3.setDrawColor(rnd, 140, 140, 160, 160);
		SDL3.INSTANCE.SDL_RenderFillRect(rnd,
				new nullpomino.gui.sdl.binding.SDLStructs.SDL_FRect(x, y + 6, 2, h - 12));
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		// Share the menu.png background with Mode Select so the whole netplay
		// flow sits on a consistent backdrop (in-game excluded — it renders its
		// own field chrome).
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		// Header strip: NAME / TEAM inputs + the back button
		NormalFontSDL.printFont(8, 8, "NAME", NormalFontSDL.COLOR_WHITE);
		nameInput.render();
		NormalFontSDL.printFont(276, 8, "TEAM", NormalFontSDL.COLOR_WHITE);
		teamInput.render();
		disconnectBtn.render();

		roomTable.render();
		if(roomTable.getRowCount() == 0) {
			NormalFontSDL.printFont(24, 96, NullpoMinoSDL.webMode
				? "NO ROOMS FOUND ONLINE" : "NO ROOMS FOUND ON LAN", NormalFontSDL.COLOR_DARKBLUE);
			NormalFontSDL.printFont(24, 116, NullpoMinoSDL.webMode
				? "CREATE ONE OR TYPE /JOIN <ROOM CODE>"
				: "CREATE ONE OR TYPE /JOIN <HOST:PORT>", NormalFontSDL.COLOR_DARKBLUE);
		}

		joinBtn.render();
		viewBtn.render();
		createBtn.render();
		rulechangeBtn.render();
		rankingBtn.render();

		// Thin dim separator line between each button group so the visual
		// grouping reads at a glance.
		drawGroupSeparator(180, 224, 28);
		drawGroupSeparator(392, 224, 28);

		// Chat log fills the bottom-left panel, matched to the input below.
		nl.chatLogLobby.x = 8;  nl.chatLogLobby.y = 258;
		nl.chatLogLobby.w = 540; nl.chatLogLobby.h = 186;
		nl.chatLogLobby.render();

		chatInput.render();

		// Lounge visitors (from presence beacons): our own name first, then
		// everyone else - our looped-back beacon is skipped by instanceId.
		NormalFontSDL.printFont(552, 258, "USERS", NormalFontSDL.COLOR_YELLOW);
		int py = 274;
		int shown = 0;
		// Column runs from x=552 to the room table's right edge (x=632); the
		// bitmap font's old 5-char cap was really this same pixel budget
		// (5 * 16px), so size it off the TTF font's real advance instead of a
		// magic char count.
		int usersListMaxChars = Math.max(1, (632 - 552 - 4) / NormalFontSDL.getTTFCharWidthPx());
		String ownName = presenceName;
		if(ownName.length() > 0) {
			String name = ownName.length() > usersListMaxChars ? ownName.substring(0, usersListMaxChars) : ownName;
			NormalFontSDL.printTTFFont(552, py, name, NormalFontSDL.COLOR_WHITE);
			py += 16;
			shown++;
		}
		if(loungeOpen) {
			for(NetLanDiscovery.Presence visitor : lounge.snapshotPresence()) {
				if(shown >= 13) break;
				if(presenceInstanceId.equals(visitor.instanceId)) continue;
				String name = visitor.playerName == null ? "" : visitor.playerName;
				if(name.length() > usersListMaxChars) name = name.substring(0, usersListMaxChars);
				NormalFontSDL.printTTFFont(552, py, name, NormalFontSDL.COLOR_WHITE);
				py += 16;
				shown++;
			}
		}
	}
}
