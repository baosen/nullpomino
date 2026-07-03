// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import nullpomino.util.CustomProperties;

/**
 * Settings for P2P room netplay, loaded from {@code config/etc/netroom.cfg}.
 */
public class RoomConfig {
	/** Config file location */
	public static final String CONFIG_FILE = "config/etc/netroom.cfg";

	/** TCP port the room transport listens on (0 = ephemeral) */
	public int listenPort = RoomProtocol.DEFAULT_PORT;

	/** Broadcast a LAN discovery beacon while a session is joinable */
	public boolean lanAnnounce = true;

	/** Interval between liveness pings on an idle link (ms) */
	public int pingInterval = RoomProtocol.PING_INTERVAL;

	/** A link with no inbound traffic for this long is dead (ms) */
	public int linkTimeout = RoomProtocol.LINK_TIMEOUT;

	/** Give up on a join after this long (ms) */
	public int joinTimeout = RoomProtocol.JOIN_TIMEOUT;

	/** Wait this long for an arbiter-migration claim before skipping the candidate (ms) */
	public int claimTimeout = RoomProtocol.CLAIM_TIMEOUT;

	/** @return Settings from the config file, falling back to defaults per key */
	public static RoomConfig load() {
		CustomProperties prop = CustomProperties.loadFromFileOrEmpty(CONFIG_FILE);
		RoomConfig config = new RoomConfig();
		config.listenPort = prop.getProperty("netroom.port", config.listenPort);
		config.lanAnnounce = prop.getProperty("netroom.lanAnnounce", config.lanAnnounce);
		config.pingInterval = prop.getProperty("netroom.pingInterval", config.pingInterval);
		config.linkTimeout = prop.getProperty("netroom.linkTimeout", config.linkTimeout);
		config.joinTimeout = prop.getProperty("netroom.joinTimeout", config.joinTimeout);
		config.claimTimeout = prop.getProperty("netroom.claimTimeout", config.claimTimeout);
		return config;
	}
}
