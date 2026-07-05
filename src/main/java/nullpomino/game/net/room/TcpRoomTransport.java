// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP implementation of {@link RoomTransport}: a listen socket with an
 * accept thread, plus a single "RoomDialer" thread that performs the
 * blocking outbound connects one at a time, in submission order.
 */
public class TcpRoomTransport implements RoomTransport {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(TcpRoomTransport.class);

	private final RoomEventSink sink;
	private ServerSocket serverSocket;
	private volatile boolean shutdownRequested = false;
	private final CopyOnWriteArrayList<RoomPeerLink> links = new CopyOnWriteArrayList<RoomPeerLink>();

	/** Dial jobs, drained sequentially by the dialer thread */
	private final LinkedBlockingQueue<Runnable> dialQueue = new LinkedBlockingQueue<Runnable>();
	private Thread dialerThread;

	public TcpRoomTransport(RoomEventSink sink) {
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
	@Override
	public int startListening(int configuredPort) throws IOException {
		try {
			serverSocket = new ServerSocket(configuredPort);
		} catch (BindException e) {
			if(configuredPort == 0) throw e;
			log.warn("Port {} is busy, falling back to an ephemeral port", configuredPort);
			serverSocket = new ServerSocket(0);
		}

		Thread acceptThread = new Thread(this::acceptLoop, "RoomAccept");
		acceptThread.setDaemon(true);
		acceptThread.start();

		log.info("Room transport listening on port {}", serverSocket.getLocalPort());
		return serverSocket.getLocalPort();
	}

	private void acceptLoop() {
		try {
			while(!shutdownRequested) {
				Socket socket = serverSocket.accept();
				RoomPeerLink link = new RoomPeerLink(socket, false, sink);
				links.add(link);
				link.start();
				sink.onLinkAccepted(link);
			}
		} catch (IOException e) {
			if(!shutdownRequested) log.warn("Room accept loop stopped", e);
		}
	}

	@Override
	public void dial(String host, int port, int timeoutMs, DialCallback callback) {
		synchronized(this) {
			if(dialerThread == null) {
				dialerThread = new Thread(this::dialLoop, "RoomDialer");
				dialerThread.setDaemon(true);
				dialerThread.start();
			}
		}
		dialQueue.add(() -> {
			try {
				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(host, port), timeoutMs);
				RoomPeerLink link = new RoomPeerLink(socket, true, sink);
				links.add(link);
				link.start();
				callback.onDialed(link);
			} catch (IOException e) {
				log.info("Room dial to {}:{} failed", host, port, e);
				callback.onDialFailed(e.getMessage());
			}
		});
	}

	private void dialLoop() {
		try {
			while(!shutdownRequested) {
				dialQueue.take().run();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** @return The bound listen port, or -1 before startListening */
	@Override
	public int getListenPort() {
		ServerSocket s = serverSocket;
		return (s == null) ? -1 : s.getLocalPort();
	}

	/** @return The machine's LAN IPv4 address, or "?" when undeterminable */
	@Override
	public String getDisplayAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while(interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				if(!ni.isUp() || ni.isLoopback()) continue;
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while(addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if((addr instanceof Inet4Address) && addr.isSiteLocalAddress()) {
						return addr.getHostAddress();
					}
				}
			}
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			return "?";
		}
	}

	/** Close the listen socket and every link */
	@Override
	public void shutdown() {
		shutdownRequested = true;
		Thread dialer;
		synchronized(this) {
			dialer = dialerThread;
		}
		if(dialer != null) dialer.interrupt();
		try {
			if(serverSocket != null) serverSocket.close();
		} catch (IOException e) {
			log.debug("Exception on server socket close", e);
		}
		for(RoomPeerLink link: links) {
			link.close(RoomProtocol.DENY_SHUTDOWN);
		}
	}
}
