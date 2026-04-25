// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.net;

import java.io.IOException;

import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;

/**
 * Lobby event interface (also used by netplay modes)
 */
public interface NetLobbyListener {
	/**
	 * Initialization Completed
	 * @param lobby NetLobbyFrame
	 */
	public void netlobbyOnInit(NetLobbyFrame lobby);

	/**
	 * Login completed
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 */
	public void netlobbyOnLoginOK(NetLobbyFrame lobby, NetPlayerClient client);

	/**
	 * When you enter a room
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 * @param roomInfo NetRoomInfo
	 */
	public void netlobbyOnRoomJoin(NetLobbyFrame lobby, NetPlayerClient client, NetRoomInfo roomInfo);

	/**
	 * When you returned to lobby
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 */
	public void netlobbyOnRoomLeave(NetLobbyFrame lobby, NetPlayerClient client);

	/**
	 * When disconnected
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 * @param ex A Throwable that caused disconnection (null if unknown or normal termination)
	 */
	public void netlobbyOnDisconnect(NetLobbyFrame lobby, NetPlayerClient client, Throwable ex);

	/**
	 * Message received
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 * @param message Message (Already sepatated by tabs)
	 * @throws IOException When something bad occurs
	 */
	public void netlobbyOnMessage(NetLobbyFrame lobby, NetPlayerClient client, String[] message) throws IOException;

	/**
	 * When the lobby window is closed
	 * @param lobby NetLobbyFrame
	 */
	public void netlobbyOnExit(NetLobbyFrame lobby);
}
