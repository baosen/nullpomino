// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import nullpomino.game.net.mesh.MeshEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NetPlayerClient} over a P2P mesh session instead of a server
 * socket. Synthesized lines from the mesh enter the inherited
 * {@link #processPacket} (mirrors update, listeners fan out - exactly the
 * socket reader path), and outbound {@link #send} routes into the mesh.
 * Never {@code start()}ed as a Thread; there is no socket.
 */
public class NetMeshPlayerClient extends NetPlayerClient {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(NetMeshPlayerClient.class);

	private final MeshEndpoint mesh;

	public NetMeshPlayerClient(MeshEndpoint mesh, String name, String team) {
		super();
		this.mesh = mesh;
		this.playerName = name;
		this.playerTeam = (team == null) ? "" : team.trim();
		this.host = mesh.getDisplayHost();
		this.port = mesh.getListenPort();
		this.ip = this.host;
	}

	/**
	 * Attach to the mesh and trigger the synthesized {@code welcome}, after
	 * which the inherited handlers drive the login sequence themselves.
	 */
	public void connect() {
		connectedFlag = true;
		mesh.setLineListener(this::onMeshLine);
		mesh.setClosedListener(this::onMeshClosed);
		mesh.clientReady();
	}

	private void onMeshLine(String line) {
		try {
			processPacket(line);
		} catch (IOException e) {
			log.error("Bad synthesized line: {}", line, e);
		}
	}

	private void onMeshClosed(String reason) {
		if(!connectedFlag) return;
		connectedFlag = false;

		Throwable cause = new IOException("Mesh session closed: " + reason);
		for(int i = 0; i < listeners.size(); i++) {
			try {
				listeners.get(i).netOnDisconnect(this, cause);
			} catch (Exception e) {
				log.debug("Uncaught Exception on NetMessageListener #{} (disconnect event)", i, e);
			}
		}
	}

	@Override
	public boolean send(String msg) {
		if(!connectedFlag) return false;
		mesh.sendLine(msg);
		return true;
	}

	@Override
	public boolean send(byte[] bytes) {
		return send(new String(bytes, StandardCharsets.UTF_8));
	}

	@Override
	public boolean isConnected() {
		return connectedFlag && mesh.isOpen();
	}

	@Override
	public void startPingTask() {
		// The mesh core owns keepalive
	}

	@Override
	public void startPingTask(long interval) {
		// The mesh core owns keepalive
	}

	@Override
	public void run() {
		log.warn("NetMeshPlayerClient is not a thread; use connect()");
	}
}
