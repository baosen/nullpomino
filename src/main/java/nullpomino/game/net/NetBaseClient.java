// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.IOException;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netplay client base: the listener registry and the tab-delimited message
 * dispatch shared by every client. Carries no transport of its own —
 * {@link NetRoomPlayerClient} routes lines through the P2P room session and
 * overrides {@link #send} / {@link #isConnected}.
 */
public class NetBaseClient {
	/** Log */
	static final Logger log = LoggerFactory.getLogger(NetBaseClient.class);

	/** Regular always While you are connectedtrue */
	public volatile boolean connectedFlag;

	/** Destination host */
	protected String host;

	/** Destination port number */
	protected int port;

	/** IP address */
	protected String ip;

	/** Interface receiving messages */
	protected LinkedList<NetMessageListener> listeners = new LinkedList<NetMessageListener>();

	/**
	 * The various processing depending on the received message
	 * @param fullMessage Received Messages
	 * @throws IOException If there are any errors
	 */
	protected void processPacket(String fullMessage) throws IOException {
		String[] message = fullMessage.split("\t");	// Tab delimited

		// ListenerCall
		for(int i = 0; i < listeners.size(); i++) {
			try {
				listeners.get(i).netOnMessage(this, message);
			} catch (Exception e) {
				log.error("Uncaught Exception on NetMessageListener #{} (message event)", i, e);
			}
		}
	}

	/**
	 * Send a message (the base class has no transport; subclasses override)
	 * @param bytes Message to be sent
	 * @return true if successful
	 */
	public boolean send(byte[] bytes) {
		return false;
	}

	/**
	 * Send a message (the base class has no transport; subclasses override)
	 * @param msg Message to be sent
	 * @return true if successful
	 */
	public boolean send(String msg) {
		return false;
	}

	/**
	 * @return Regular always And are connectedtrue
	 */
	public boolean isConnected() {
		return false;
	}

	/**
	 * @return Destination host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return Destination port number
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return Server's IP address
	 */
	public String getIP() {
		return ip;
	}

	/**
	 * NewNetMessageListenerAdd
	 * @param l AddNetMessageListener
	 */
	public void addListener(NetMessageListener l) {
		if(!listeners.contains(l)) listeners.add(l);
	}

	/**
	 * SpecifiedNetMessageListenerDelete the
	 * @param l RemoveNetMessageListener
	 * @return Actually been removedtrue, I has not been added originallyfalse
	 */
	public boolean removeListener(NetMessageListener l) {
		return listeners.remove(l);
	}
}
