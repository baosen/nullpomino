// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.sdl.NullpoMinoSDL;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;

/**
 * Publishes a tiny netplay status object to {@code window.__nppNet}
 * twice a second. The whole UI is canvas-rendered, so end-to-end tests
 * (and curious humans) read this instead of scraping pixels:
 * {@code {state, lobbyMode, players, rooms, mqtt}}.
 */
public final class WebNetDebug {
	private WebNetDebug() {}

	@JSBody(params = {"state", "lobbyMode", "players", "rooms", "mqtt"}, script =
		"window.__nppNet = { state: state, lobbyMode: lobbyMode, players: players,"
		+ " rooms: rooms, mqtt: mqtt };")
	private static native void publish(int state, int lobbyMode, int players, int rooms, boolean mqtt);

	/** Install the sampler (idempotent enough - called once from the entry point) */
	public static void install() {
		Window.setInterval(WebNetDebug::sample, 500);
	}

	private static void sample() {
		int lobbyMode = -1;
		int players = 0;
		NetLobbyFrame lobby = NullpoMinoSDL.netLobby;
		if(lobby != null) {
			lobbyMode = lobby.lobbyMode;
			if(lobby.netPlayerClient != null) {
				players = lobby.netPlayerClient.getPlayerInfoList().size();
			}
		}
		WebNetHub hub = WebNetHub.get();
		int rooms = 0;
		try {
			rooms = nullpomino.game.net.NetPlatform.lounge().snapshotRooms().size();
		} catch (IllegalStateException e) {
			// lounge not installed yet
		}
		publish(NullpoMinoSDL.currentState, lobbyMode, players, rooms, hub.channel.isUp());
	}
}
