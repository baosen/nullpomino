// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

/**
 * Desktop {@link RoomNet}: TCP mesh links. Referenced only from the
 * desktop entry point and tests, never from web-reachable code, which
 * keeps the {@code java.net} transport out of the TeaVM call graph.
 */
public class LanRoomNet implements RoomNet {
	@Override
	public RoomTransport createTransport(RoomEventSink sink) {
		return new TcpRoomTransport(sink);
	}

	@Override
	public RoomDispatcher createDispatcher() {
		return new ThreadRoomDispatcher();
	}

	@Override
	public RoomBeacon createBeacon() {
		return new LanRoomBeacon();
	}
}
