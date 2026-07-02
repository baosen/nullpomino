// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import nullpomino.util.CustomProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a {@link NetServer} inside the client process so a player can host
 * netplay games without a dedicated server ("Open to LAN" style hosting).
 * Also broadcasts a {@link NetLanDiscovery} beacon so other clients on the
 * local network can find the game.
 *
 * <p>Only one embedded server may run per JVM because NetServer's config
 * state is static.
 */
public class NetServerRunner {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(NetServerRunner.class);

	/** The embedded server, null when not hosting */
	private NetServer server;

	/** Thread running the server mainloop */
	private Thread serverThread;

	/** LAN discovery beacon, null when disabled */
	private NetLanDiscovery.Announcer announcer;

	/** The port the server is listening on */
	private int port = -1;

	/**
	 * Start hosting: bind the configured port, run the server mainloop on a
	 * daemon thread, and start the LAN beacon (unless disabled in config).
	 * The port is accepting connections when this method returns.
	 * @param hostPlayerName Player name announced in the LAN beacon
	 * @throws IOException When the port can't be bound (usually already in use)
	 */
	public void start(String hostPlayerName) throws IOException {
		if(isRunning()) return;

		server = NetServer.createEmbedded(-1);
		server.startListening();
		port = server.getLocalPort();

		serverThread = new Thread(server::run, "EmbeddedNetServer");
		serverThread.setDaemon(true);
		serverThread.start();
		log.info("Embedded server started on port {}", port);

		CustomProperties prop = CustomProperties.loadFromFileOrEmpty("config/etc/netserver.cfg");
		if(prop.getProperty("netserver.lanAnnounce", true)) {
			announcer = new NetLanDiscovery.Announcer(port, hostPlayerName);
			announcer.start();
		}
	}

	/**
	 * Stop hosting: stop the LAN beacon and shut down the server, which
	 * disconnects all clients and releases the port.
	 */
	public void stop() {
		if(announcer != null) {
			announcer.shutdown();
			announcer = null;
		}
		if(server != null) {
			server.requestShutdown();
			try {
				serverThread.join(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if(serverThread.isAlive()) {
				// Daemon thread, so it can never block app exit; just report it
				log.warn("Embedded server thread did not stop within 3 seconds");
			}
			server = null;
			serverThread = null;
			port = -1;
			log.info("Embedded server stopped");
		}
	}

	/** @return true while the embedded server is hosting */
	public boolean isRunning() {
		return (serverThread != null) && serverThread.isAlive();
	}

	/** @return The port the embedded server is listening on, or -1 when not hosting */
	public int getPort() {
		return port;
	}

	/**
	 * @return The machine's LAN IPv4 address to show to the hosting player,
	 *         or "?" when it can't be determined
	 */
	public static String getLanAddress() {
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
			log.debug("Failed to determine LAN address", e);
			return "?";
		}
	}
}
