// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

/**
 * Receives transport-level events from a {@link RoomTransport} and
 * its {@link RoomLink}s. Implementations are called from transport threads or event contexts and must be thread-safe and non-blocking (typically they just
 * enqueue a {@link RoomEvent} for the dispatcher).
 */
public interface RoomEventSink {
	/** A new inbound connection was accepted (link already started, not yet uid-bound) */
	void onLinkAccepted(RoomLink link);

	/** A complete newline-terminated line arrived (without the newline) */
	void onLine(RoomLink link, String line);

	/** The link died; fired exactly once per link */
	void onLinkClosed(RoomLink link, String reason);
}
