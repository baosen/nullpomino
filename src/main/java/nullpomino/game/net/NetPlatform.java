// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import nullpomino.game.net.room.RoomNet;

/**
 * Holder for the platform's netplay implementation, installed once by
 * the entry point before any netplay UI runs: the desktop main installs
 * the TCP/UDP LAN implementation, the browser entry point installs the
 * WebRTC/MQTT one. This class itself is {@code java.net}-free so it is
 * safe in the web-reachable graph.
 */
public final class NetPlatform {
	private static volatile RoomNet roomNet;
	private static volatile LoungeService lounge;

	private NetPlatform() {}

	/** Install the platform's netplay implementation. Called once at startup. */
	public static void install(RoomNet net, LoungeService loungeService) {
		roomNet = net;
		lounge = loungeService;
	}

	/** @return The installed room networking (never null once the entry point ran) */
	public static RoomNet roomNet() {
		RoomNet net = roomNet;
		if(net == null) throw new IllegalStateException("NetPlatform.install was never called");
		return net;
	}

	/** @return The installed lounge service (never null once the entry point ran) */
	public static LoungeService lounge() {
		LoungeService service = lounge;
		if(service == null) throw new IllegalStateException("NetPlatform.install was never called");
		return service;
	}
}
