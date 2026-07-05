// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.util.List;

/**
 * The netplay lounge as seen by the UI: discovered rooms, lounge chat
 * and presence, independent of how they travel (UDP LAN broadcast on
 * desktop, a public MQTT broker in the browser). Reuses
 * {@link NetLanDiscovery}'s data classes and packet codec, which are
 * transport-free.
 */
public interface LoungeService {
	/**
	 * Start receiving lounge traffic. Idempotent.
	 * @return false when the lounge is unavailable (UDP port busy / broker unreachable)
	 */
	boolean open();

	/** Stop receiving and announcing. Idempotent. */
	void close();

	/** Install the deduplicated lounge-chat sink (called from a background context) */
	void setChatConsumer(NetLanDiscovery.ChatConsumer consumer);

	/**
	 * Send a lounge chat line. Best-effort; the sender's own copy is
	 * dedupe-marked so the UI can echo locally without seeing it twice.
	 */
	void sendChat(String playerName, String message);

	/**
	 * Announce this player in the lounge. An empty name announces nothing;
	 * the beacon stops on {@link #close()} and entries age out via TTL.
	 * @param instanceId Random id stable for one lounge visit (rename-safe key)
	 */
	void setPresence(String playerName, String instanceId);

	/** @return Live room announces, session-deduplicated, sorted by host:port */
	List<NetLanDiscovery.Announce> snapshotRooms();

	/** @return Live lounge visitors, sorted by name */
	List<NetLanDiscovery.Presence> snapshotPresence();
}
