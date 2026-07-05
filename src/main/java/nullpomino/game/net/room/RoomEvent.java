// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

/**
 * One event on the room session's single dispatcher queue. All state
 * mutations happen on the dispatcher thread that drains these.
 */
public final class RoomEvent {
	public enum Type {
		/** A new inbound connection was accepted (link not yet bound to a uid) */
		LINK_ACCEPTED,
		/** A complete line arrived on a link */
		LINE,
		/** A link died (EOF, IO error, write overflow, or explicit close) */
		LINK_CLOSED,
		/** The local client wrote a line (client-to-server direction) */
		LOCAL_SEND,
		/** Periodic timer tick (liveness checks, timeouts) */
		TICK,
		/** Session shutdown requested */
		SHUTDOWN
	}

	public final Type type;
	public final RoomLink link;
	public final String line;
	public final String reason;

	private RoomEvent(Type type, RoomLink link, String line, String reason) {
		this.type = type;
		this.link = link;
		this.line = line;
		this.reason = reason;
	}

	public static RoomEvent linkAccepted(RoomLink link) {
		return new RoomEvent(Type.LINK_ACCEPTED, link, null, null);
	}

	public static RoomEvent line(RoomLink link, String line) {
		return new RoomEvent(Type.LINE, link, line, null);
	}

	public static RoomEvent linkClosed(RoomLink link, String reason) {
		return new RoomEvent(Type.LINK_CLOSED, link, null, reason);
	}

	public static RoomEvent localSend(String line) {
		return new RoomEvent(Type.LOCAL_SEND, null, line, null);
	}

	public static RoomEvent tick() {
		return new RoomEvent(Type.TICK, null, null, null);
	}

	public static RoomEvent shutdown(String reason) {
		return new RoomEvent(Type.SHUTDOWN, null, null, reason);
	}
}
