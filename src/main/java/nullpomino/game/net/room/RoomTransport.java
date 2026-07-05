// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import java.io.IOException;

/**
 * Listens for and dials room peer connections. Contains no protocol
 * logic - it only creates {@link RoomLink}s and pumps events into the
 * {@link RoomEventSink}. Desktop rooms use {@link TcpRoomTransport};
 * the browser build supplies a WebRTC implementation where "host" is a
 * signaling clientId and ports are meaningless.
 */
public interface RoomTransport {
	/** Receives the result of an asynchronous {@link #dial}; may fire on any thread */
	interface DialCallback {
		/** The link is started; the handshake (hello) is the caller's job */
		void onDialed(RoomLink link);

		/** The connection could not be established */
		void onDialFailed(String reason);
	}

	/**
	 * Start accepting inbound connections. TCP binds a listen socket; the
	 * WebRTC transport registers its signaling inbox instead.
	 * @param configuredPort Preferred port (0 = ephemeral; ignored on web)
	 * @return The actually bound port (0 on web)
	 * @throws IOException When listening cannot be started at all
	 */
	int startListening(int configuredPort) throws IOException;

	/**
	 * Dial a peer asynchronously. NEVER blocks the caller; the callback
	 * fires later from a transport-owned thread or event context. Dials
	 * are performed in submission order.
	 * @param host Peer address: IP/hostname on TCP, signaling clientId on WebRTC
	 */
	void dial(String host, int port, int timeoutMs, DialCallback callback);

	/** @return The bound listen port, or -1 before startListening (a dummy constant on web) */
	int getListenPort();

	/** @return Address to advertise/display for the local peer: LAN IPv4 on desktop, clientId on web */
	String getDisplayAddress();

	/** Stop listening and close every link */
	void shutdown();
}
