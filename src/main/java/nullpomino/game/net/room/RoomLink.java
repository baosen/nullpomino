// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

/**
 * One connection to another room peer, independent of transport (TCP
 * socket on desktop, WebRTC data channel in the browser). Line-oriented:
 * implementations deliver complete newline-stripped lines to the
 * {@link RoomEventSink} and accept lines for sending from any thread.
 */
public abstract class RoomLink {
	/** Peer uid this link is bound to; -1 until the handshake binds it. Dispatcher-confined. */
	public int uid = -1;

	/** Time of the last inbound data (liveness) */
	public volatile long lastInboundMillis = System.currentTimeMillis();

	/**
	 * Queue a line for sending (newline appended). Non-blocking, callable
	 * from any thread; a no-op after close. Overflow closes the link.
	 */
	public abstract void sendLine(String line);

	/**
	 * Close the link. Idempotent; fires {@link RoomEventSink#onLinkClosed}
	 * exactly once, from whichever caller wins.
	 */
	public abstract void close(String reason);

	/**
	 * Close the link once everything queued so far has been sent - for
	 * farewell lines (deny/bye) that must reach the peer before the link
	 * dies. Falls back to an immediate close when that isn't possible.
	 */
	public abstract void closeAfterFlush(String reason);

	/** @return true once the link is closed */
	public abstract boolean isClosed();

	/** @return The peer's transport-level address: IP on TCP, signaling clientId on WebRTC */
	public abstract String getRemoteAddress();
}
