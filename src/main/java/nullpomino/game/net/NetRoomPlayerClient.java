// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import nullpomino.game.net.room.RoomEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NetPlayerClient} over a P2P room session instead of a server
 * socket. Synthesized lines from the room session enter the inherited
 * {@link #processPacket} (mirrors update, listeners fan out - exactly the
 * socket reader path), and outbound {@link #send} routes into the room session.
 * Never {@code start()}ed as a Thread; there is no socket.
 */
public class NetRoomPlayerClient extends NetPlayerClient {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(NetRoomPlayerClient.class);

	private final RoomEndpoint room;

	public NetRoomPlayerClient(RoomEndpoint room, String name, String team) {
		super();
		this.room = room;
		this.playerName = name;
		this.playerTeam = (team == null) ? "" : team.trim();
		this.host = room.getDisplayHost();
		this.port = room.getListenPort();
		this.ip = this.host;
	}

	/**
	 * Attach to the room session and trigger the synthesized {@code welcome}, after
	 * which the inherited handlers drive the login sequence themselves.
	 */
	public void connect() {
		connectedFlag = true;
		room.setLineListener(this::onRoomLine);
		room.setClosedListener(this::onRoomClosed);
		room.clientReady();
	}

	private void onRoomLine(String line) {
		try {
			processPacket(line);
		} catch (IOException e) {
			log.error("Bad synthesized line: {}", line, e);
		}
	}

	private void onRoomClosed(String reason) {
		if(!connectedFlag) return;
		connectedFlag = false;

		Throwable cause = new IOException("Room session closed: " + reason);
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
		room.sendLine(msg);
		return true;
	}

	@Override
	public boolean send(byte[] bytes) {
		return send(new String(bytes, StandardCharsets.UTF_8));
	}

	@Override
	public boolean isConnected() {
		return connectedFlag && room.isOpen();
	}

	@Override
	public void startPingTask() {
		// The room core owns keepalive
	}

	@Override
	public void startPingTask(long interval) {
		// The room core owns keepalive
	}

	@Override
	public void run() {
		log.warn("NetRoomPlayerClient is not a thread; use connect()");
	}
}
