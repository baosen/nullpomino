/*
    Copyright (c) 2010, NullNoname — see LICENSE for details.
*/
package mu.nu.nullpo.gui.sdl;

import java.util.LinkedList;

import mu.nu.nullpo.game.net.NetPlayerInfo;
import mu.nu.nullpo.game.net.NetRoomInfo;
import mu.nu.nullpo.gui.net.NetLobbyFrame;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.widget.ButtonSDL;
import mu.nu.nullpo.gui.sdl.widget.TextInputSDL;
import mu.nu.nullpo.gui.sdl.widget.WidgetSDL;

/**
 * In-room state: seated player list, room chat, sit in/out, leave-room.  When
 * the server starts the game, {@code StateNetGameSDL} takes over rendering; we
 * re-enter here when the player returns to the room.
 */
public class StateNetRoomSDL extends BaseStateSDL {
	private TextInputSDL chatInput;
	private ButtonSDL sendBtn;
	private ButtonSDL sitInBtn;
	private ButtonSDL sitOutBtn;
	private ButtonSDL teamBtn;
	private ButtonSDL settingsBtn;
	private ButtonSDL leaveBtn;

	private WidgetSDL focused;
	private String statusLine = "";

	@Override
	public void enter() {
		chatInput = new TextInputSDL(8, 390, 496, 28);
		chatInput.maxChars = 255;
		chatInput.placeholder = "Chat...";
		sendBtn = new ButtonSDL(508, 390, 80, 28, "SEND");
		sendBtn.primary = true;

		int ay = 420;
		sitInBtn    = new ButtonSDL(  8, ay,  80, 28, "JOIN");
		sitInBtn.primary = true;
		sitOutBtn   = new ButtonSDL( 92, ay,  80, 28, "SIT OUT");
		teamBtn     = new ButtonSDL(176, ay,  80, 28, "TEAM");
		settingsBtn = new ButtonSDL(260, ay,  96, 28, "SETTINGS");
		leaveBtn    = new ButtonSDL(548, ay,  84, 28, "LEAVE");

		setFocus(chatInput);
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

		if(nl.netPlayerClient == null || !nl.netPlayerClient.isConnected()) {
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_SERVERSELECT);
			return;
		}

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		if(chatInput.update(mx, my, clicked)) setFocus(chatInput);
		nl.chatLogRoom.update(mx, my, clicked);

		if(sendBtn.update(mx, my, clicked)) sendChat(nl);
		if(sitInBtn.update(mx, my, clicked))  nl.netPlayerClient.send("changestatus\tfalse\n");
		if(sitOutBtn.update(mx, my, clicked)) nl.netPlayerClient.send("changestatus\ttrue\n");
		if(teamBtn.update(mx, my, clicked))   chatInput.setText("/team ");
		if(settingsBtn.update(mx, my, clicked)) {
			NetPlayerInfo me = nl.netPlayerClient.getYourPlayerInfo();
			if(me != null && me.roomID != -1) {
				nl.currentViewDetailRoomID = me.roomID;
				NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_CREATEROOM);
			}
		}
		if(leaveBtn.update(mx, my, clicked)) {
			nl.netPlayerClient.send("roomjoin\t-1\tfalse\n");
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
		}

		String typed = NullpoMinoSDL.consumeTextInput();
		if(focused != null && typed.length() > 0) focused.handleTextInput(typed);
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			handleGlobalKey(nl, ev);
			if(focused != null) focused.handleKey(ev);
		}
	}

	private void handleGlobalKey(NetLobbyFrame nl, NullpoMinoSDL.KeyEvent ev) {
		if(ev.repeat) return;
		switch(ev.scancode) {
			case SDLConstants.SDL_SCANCODE_ESCAPE:
				if(focused == chatInput) chatInput.setText("");
				else NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
				break;
			case SDLConstants.SDL_SCANCODE_RETURN:
			case SDLConstants.SDL_SCANCODE_KP_ENTER:
				if(focused == chatInput) sendChat(nl);
				break;
			case SDLConstants.SDL_SCANCODE_PAGEUP:    nl.chatLogRoom.pageUp(); break;
			case SDLConstants.SDL_SCANCODE_PAGEDOWN:  nl.chatLogRoom.pageDown(); break;
			default: break;
		}
	}

	private void sendChat(NetLobbyFrame nl) {
		String msg = chatInput.getText().trim();
		if(msg.length() == 0) return;
		nl.sendChat(true, msg);
		chatInput.setText("");
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;
		NetRoomInfo room = null;
		NetPlayerInfo me = null;
		if(nl.netPlayerClient != null) {
			me = nl.netPlayerClient.getYourPlayerInfo();
			if(me != null && me.roomID != -1) room = nl.netPlayerClient.getRoomInfo(me.roomID);
		}

		NormalFontSDL.printFont(8, 8, "ROOM", NormalFontSDL.COLOR_CYAN);
		if(room != null) {
			NormalFontSDL.printFont(80, 8, NormalFontSDL.safeString("#" + room.roomID + " " + room.strName),
					NormalFontSDL.COLOR_WHITE);
			NormalFontSDL.printFont(8, 28, NormalFontSDL.safeString(
					"MODE: " + room.strMode + "   RULE: " + (room.ruleLock ? room.ruleName : "ANY")),
					NormalFontSDL.COLOR_YELLOW);
			NormalFontSDL.printFont(8, 44, NormalFontSDL.safeString(
					"PLAYERS: " + room.playerSeatedCount + "/" + room.maxPlayers
					+ "   SPECTATORS: " + room.spectatorCount),
					NormalFontSDL.COLOR_WHITE);
		} else {
			NormalFontSDL.printFont(80, 8, "(NO ROOM)", NormalFontSDL.COLOR_DARKBLUE);
		}

		// Seated-player list (left side of stats area)
		NormalFontSDL.printFont(8, 68, "PLAYERS", NormalFontSDL.COLOR_YELLOW);
		int py = 84;
		if(room != null && nl.netPlayerClient != null) {
			LinkedList<NetPlayerInfo> pList = nl.updateSameRoomPlayerInfoList();
			for(NetPlayerInfo p : pList) {
				String status = p.playing ? " [P]" : (p.ready ? " [R]" : (p.seatID == -1 ? " [Q]" : ""));
				String name = nl.getPlayerNameWithTripCode(p);
				if(me != null && p.uid == me.uid) name = "*" + name;
				int color = p.playing ? NormalFontSDL.COLOR_GREEN
						: p.ready ? NormalFontSDL.COLOR_YELLOW
						: p.seatID == -1 ? NormalFontSDL.COLOR_DARKBLUE
						: NormalFontSDL.COLOR_WHITE;
				NormalFontSDL.printFont(8, py, NormalFontSDL.safeString(name + status), color);
				py += 16;
				if(py > 370) break;
			}
		}

		// Chat log (right side)
		nl.chatLogRoom.x = 240; nl.chatLogRoom.y = 68;
		nl.chatLogRoom.w = 392; nl.chatLogRoom.h = 312;
		nl.chatLogRoom.render();

		chatInput.render();
		sendBtn.render();

		// Bottom action row visibility based on seat state
		if(me != null) {
			boolean seated = me.seatID >= 0;
			sitInBtn.visible  = !seated;
			sitOutBtn.visible = seated;
			teamBtn.enabled   = me.roomID != -1;
		}
		sitInBtn.render();
		sitOutBtn.render();
		teamBtn.render();
		settingsBtn.render();
		leaveBtn.render();

		if(statusLine.length() > 0) {
			NormalFontSDL.printFont(8, 464, NormalFontSDL.safeString(statusLine), NormalFontSDL.COLOR_RED);
		}
	}
}
