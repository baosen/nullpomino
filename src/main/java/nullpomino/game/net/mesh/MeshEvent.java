// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

/**
 * One event on the mesh session's single dispatcher queue. All state
 * mutations happen on the dispatcher thread that drains these.
 */
public final class MeshEvent {
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
	public final MeshPeerLink link;
	public final String line;
	public final String reason;

	private MeshEvent(Type type, MeshPeerLink link, String line, String reason) {
		this.type = type;
		this.link = link;
		this.line = line;
		this.reason = reason;
	}

	public static MeshEvent linkAccepted(MeshPeerLink link) {
		return new MeshEvent(Type.LINK_ACCEPTED, link, null, null);
	}

	public static MeshEvent line(MeshPeerLink link, String line) {
		return new MeshEvent(Type.LINE, link, line, null);
	}

	public static MeshEvent linkClosed(MeshPeerLink link, String reason) {
		return new MeshEvent(Type.LINK_CLOSED, link, null, reason);
	}

	public static MeshEvent localSend(String line) {
		return new MeshEvent(Type.LOCAL_SEND, null, line, null);
	}

	public static MeshEvent tick() {
		return new MeshEvent(Type.TICK, null, null, null);
	}

	public static MeshEvent shutdown(String reason) {
		return new MeshEvent(Type.SHUTDOWN, null, null, reason);
	}
}
