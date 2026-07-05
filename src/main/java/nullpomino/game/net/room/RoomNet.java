// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

/**
 * Bundle of platform-specific networking pieces a {@link RoomSession}
 * is built from. Desktop sessions run over TCP ({@link LanRoomNet});
 * the browser build supplies a WebRTC/MQTT implementation. Keeping the
 * platform pieces behind this seam is what keeps {@code java.net} out
 * of the web-reachable code.
 */
public interface RoomNet {
	/** @return A fresh transport delivering its events into the given sink */
	RoomTransport createTransport(RoomEventSink sink);
}
