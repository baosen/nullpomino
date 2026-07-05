// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import nullpomino.game.net.NetLanDiscovery;

/**
 * Periodically announces a joinable room session to the lounge (UDP
 * broadcast on desktop, an MQTT topic in the browser). Pull model: the
 * supplier is polled each cycle, and a null announce skips the cycle -
 * which is how the arbiter-only/room-exists gating in
 * {@link RoomSession#refreshBeaconSnapshot} takes effect.
 */
public interface RoomBeacon {
	/** Supplies the current announce each cycle; null = nothing to announce */
	interface Supplier {
		NetLanDiscovery.Announce get();
	}

	/** Start announcing. Idempotent. */
	void start(Supplier supplier);

	/** Stop announcing. Idempotent. */
	void stop();
}
