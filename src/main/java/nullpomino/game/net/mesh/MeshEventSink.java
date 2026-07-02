// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

/**
 * Receives transport-level events from {@link MeshTransport} and
 * {@link MeshPeerLink}. Implementations are called from reader/accept
 * threads and must be thread-safe and non-blocking (typically they just
 * enqueue a {@link MeshEvent} for the dispatcher).
 */
public interface MeshEventSink {
	/** A new inbound connection was accepted (link already started, not yet uid-bound) */
	void onLinkAccepted(MeshPeerLink link);

	/** A complete newline-terminated line arrived (without the newline) */
	void onLine(MeshPeerLink link, String line);

	/** The link died; fired exactly once per link */
	void onLinkClosed(MeshPeerLink link, String reason);
}
