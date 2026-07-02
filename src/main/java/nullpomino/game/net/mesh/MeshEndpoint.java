// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

/**
 * The narrow seam the netplay client stack talks to instead of a server
 * socket. Synthesized server-to-client lines arrive through the line
 * listener (delivered serially from the mesh dispatcher thread - the same
 * threading role the socket reader thread has today); outbound
 * client-to-server lines go through {@link #sendLine}.
 */
public interface MeshEndpoint {
	/** Sink for synthesized server-to-client lines (no trailing newline). */
	interface LineListener {
		void onLine(String line);
	}

	/** Fired exactly once when the session dies or is left. */
	interface ClosedListener {
		void onClosed(String reason);
	}

	void setLineListener(LineListener listener);

	void setClosedListener(ClosedListener listener);

	/**
	 * Send a verbatim client-to-server line (trailing newline optional).
	 * Thread-safe, non-blocking.
	 */
	void sendLine(String line);

	/** The local client is attached and ready to receive the welcome line. */
	void clientReady();

	boolean isOpen();

	/** The TCP port this peer's mesh transport listens on */
	int getListenPort();

	/** Host string shown in the client UI (the arbiter address, or own LAN address) */
	String getDisplayHost();

	/** Session identifier (also carried in LAN beacons) */
	String getSessionId();

	/** Leave the session and release all resources. Idempotent. */
	void shutdown();
}
