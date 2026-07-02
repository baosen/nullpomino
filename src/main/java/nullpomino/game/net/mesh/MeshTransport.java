// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for and dials mesh peer connections. Contains no protocol
 * logic - it only creates {@link MeshPeerLink}s and pumps events into
 * the {@link MeshEventSink}.
 */
public class MeshTransport {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(MeshTransport.class);

	private final MeshEventSink sink;
	private ServerSocket serverSocket;
	private volatile boolean shutdownRequested = false;
	private final CopyOnWriteArrayList<MeshPeerLink> links = new CopyOnWriteArrayList<MeshPeerLink>();

	public MeshTransport(MeshEventSink sink) {
		this.sink = sink;
	}

	/**
	 * Bind the listen socket and start the accept thread. If the configured
	 * port is busy, falls back to an ephemeral port with a warning (LAN
	 * discovery carries the real port; internet play needs the forwarded one).
	 * @param configuredPort Preferred port (0 = ephemeral)
	 * @return The actually bound port
	 * @throws IOException When even an ephemeral bind fails
	 */
	public int startListening(int configuredPort) throws IOException {
		try {
			serverSocket = new ServerSocket(configuredPort);
		} catch (BindException e) {
			if(configuredPort == 0) throw e;
			log.warn("Port {} is busy, falling back to an ephemeral port", configuredPort);
			serverSocket = new ServerSocket(0);
		}

		Thread acceptThread = new Thread(this::acceptLoop, "MeshAccept");
		acceptThread.setDaemon(true);
		acceptThread.start();

		log.info("Mesh transport listening on port {}", serverSocket.getLocalPort());
		return serverSocket.getLocalPort();
	}

	private void acceptLoop() {
		try {
			while(!shutdownRequested) {
				Socket socket = serverSocket.accept();
				MeshPeerLink link = new MeshPeerLink(socket, false, sink);
				links.add(link);
				link.start();
				sink.onLinkAccepted(link);
			}
		} catch (IOException e) {
			if(!shutdownRequested) log.warn("Mesh accept loop stopped", e);
		}
	}

	/**
	 * Dial a peer. BLOCKS up to timeoutMs - callers on the dispatcher thread
	 * should dial from a helper thread.
	 * @return The started link (handshake is the caller's job)
	 * @throws IOException When the connection fails
	 */
	public MeshPeerLink dial(String host, int port, int timeoutMs) throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(host, port), timeoutMs);
		MeshPeerLink link = new MeshPeerLink(socket, true, sink);
		links.add(link);
		link.start();
		return link;
	}

	/** @return The bound listen port, or -1 before startListening */
	public int getListenPort() {
		ServerSocket s = serverSocket;
		return (s == null) ? -1 : s.getLocalPort();
	}

	/** Close the listen socket and every link */
	public void shutdown() {
		shutdownRequested = true;
		try {
			if(serverSocket != null) serverSocket.close();
		} catch (IOException e) {
			log.debug("Exception on server socket close", e);
		}
		for(MeshPeerLink link: links) {
			link.close(MeshProtocol.DENY_SHUTDOWN);
		}
	}
}
