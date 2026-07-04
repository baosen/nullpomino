// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.IOException;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Browser (TeaVM) stub of {@link NetBaseClient}. TeaVM's classlib has no
 * {@code java.net.Socket}, and the real client's {@code send}/{@code run} are
 * statically reachable from every mode's netplay lifecycle hooks — so the web
 * build compiles against this API-compatible, socket-free replacement instead.
 * Netplay is unreachable at runtime in the browser (the {@code StateNet*SDL}
 * states are never registered), so these no-ops are never hit; they exist only
 * to keep the reachable call graph free of {@code java.net}.
 */
public class NetBaseClient extends Thread {
	static final Logger log = LoggerFactory.getLogger(NetBaseClient.class);

	public static final int DEFAULT_PORT = 9200;
	public static final int BUF_SIZE = 2048;
	public static final int PING_INTERVAL = 5 * 1000;
	public static final int PING_AUTO_DISCONNECT_COUNT = 6;

	public volatile boolean threadRunning;
	public volatile boolean connectedFlag;

	protected String host;
	protected int port;
	protected String ip;
	protected StringBuilder notCompletePacketBuffer;
	protected LinkedList<NetMessageListener> listeners = new LinkedList<NetMessageListener>();
	protected int pingCount;

	public NetBaseClient() {
		super();
		this.host = null;
		this.port = DEFAULT_PORT;
	}

	public NetBaseClient(String host) {
		super("NET_" + host);
		this.host = host;
		this.port = DEFAULT_PORT;
	}

	public NetBaseClient(String host, int port) {
		super("NET_" + host + ":" + port);
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		// Networking is unavailable in the browser build.
		threadRunning = false;
		connectedFlag = false;
	}

	protected void processPacket(String fullMessage) throws IOException {
		String[] message = fullMessage.split("\t");
		for (int i = 0; i < listeners.size(); i++) {
			try {
				listeners.get(i).netOnMessage(this, message);
			} catch (Exception e) {
				log.error("Uncaught Exception on NetMessageListener #{} (message event)", i, e);
			}
		}
	}

	public boolean send(byte[] bytes) {
		return false;
	}

	public boolean send(String msg) {
		return false;
	}

	public boolean isConnected() {
		return false;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getIP() {
		return ip;
	}

	public void addListener(NetMessageListener l) {
		if (!listeners.contains(l)) listeners.add(l);
	}

	public boolean removeListener(NetMessageListener l) {
		return listeners.remove(l);
	}

	public void startPingTask() {
	}

	public void startPingTask(long interval) {
	}
}
