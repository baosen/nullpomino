// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

import nullpomino.util.CustomProperties;

/**
 * Settings for P2P mesh netplay, loaded from {@code config/etc/netmesh.cfg}.
 */
public class MeshConfig {
	/** Config file location */
	public static final String CONFIG_FILE = "config/etc/netmesh.cfg";

	/** TCP port the mesh transport listens on (0 = ephemeral) */
	public int listenPort = MeshProtocol.DEFAULT_PORT;

	/** Broadcast a LAN discovery beacon while a session is joinable */
	public boolean lanAnnounce = true;

	/** Interval between liveness pings on an idle link (ms) */
	public int pingInterval = MeshProtocol.PING_INTERVAL;

	/** A link with no inbound traffic for this long is dead (ms) */
	public int linkTimeout = MeshProtocol.LINK_TIMEOUT;

	/** Give up on a join after this long (ms) */
	public int joinTimeout = MeshProtocol.JOIN_TIMEOUT;

	/** Wait this long for an arbiter-migration claim before skipping the candidate (ms) */
	public int claimTimeout = MeshProtocol.CLAIM_TIMEOUT;

	/** @return Settings from the config file, falling back to defaults per key */
	public static MeshConfig load() {
		CustomProperties prop = CustomProperties.loadFromFileOrEmpty(CONFIG_FILE);
		MeshConfig config = new MeshConfig();
		config.listenPort = prop.getProperty("netmesh.port", config.listenPort);
		config.lanAnnounce = prop.getProperty("netmesh.lanAnnounce", config.lanAnnounce);
		config.pingInterval = prop.getProperty("netmesh.pingInterval", config.pingInterval);
		config.linkTimeout = prop.getProperty("netmesh.linkTimeout", config.linkTimeout);
		config.joinTimeout = prop.getProperty("netmesh.joinTimeout", config.joinTimeout);
		config.claimTimeout = prop.getProperty("netmesh.claimTimeout", config.claimTimeout);
		return config;
	}
}
