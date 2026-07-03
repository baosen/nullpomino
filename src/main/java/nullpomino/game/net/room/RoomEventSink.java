// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

/**
 * Receives transport-level events from {@link RoomTransport} and
 * {@link RoomPeerLink}. Implementations are called from reader/accept
 * threads and must be thread-safe and non-blocking (typically they just
 * enqueue a {@link RoomEvent} for the dispatcher).
 */
public interface RoomEventSink {
	/** A new inbound connection was accepted (link already started, not yet uid-bound) */
	void onLinkAccepted(RoomPeerLink link);

	/** A complete newline-terminated line arrived (without the newline) */
	void onLine(RoomPeerLink link, String line);

	/** The link died; fired exactly once per link */
	void onLinkClosed(RoomPeerLink link, String reason);
}
