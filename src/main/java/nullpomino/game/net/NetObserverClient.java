// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.IOException;

import nullpomino.game.play.GameManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client(ObserverUse)
 */
public class NetObserverClient extends NetBaseClient {
	/** Log */
	static final Logger log = LoggerFactory.getLogger(NetObserverClient.class);

	/** ServerVersion */
	protected volatile float serverVersion = 0f;

	/** Number of players */
	protected volatile int playerCount = 0;

	/** Observercount */
	protected volatile int observerCount = 0;

	/**
	 * Default constructor
	 */
	public NetObserverClient() {
		super();
	}

	/**
	 * Constructor
	 * @param host Destination host
	 */
	public NetObserverClient(String host) {
		super(host);
	}

	/**
	 * Constructor
	 * @param host Destination host
	 * @param port Destination port number
	 */
	public NetObserverClient(String host, int port) {
		super(host, port);
	}

	/*
	 * The various processing depending on the received message
	 */
	@Override
	protected void processPacket(String fullMessage) throws IOException {
		String[] message = fullMessage.split("\t");	// Tab delimited

		// Connection completion
		if(message[0].equals("welcome")) {
			//welcome\t[VERSION]\t[PLAYERS]\t[OBSERVERS]\t[VERSION MINOR]\t[VERSION STRING]\t[PING INTERVAL]\t[DEV BUILD]
			serverVersion = Float.parseFloat(message[1]);
			playerCount = Integer.parseInt(message[2]);
			observerCount = Integer.parseInt(message[3]);

			long pingInterval = (message.length > 6) ? Long.parseLong(message[6]) : PING_INTERVAL;
			if(pingInterval != PING_INTERVAL) {
				startPingTask(pingInterval);
			}

			send("observerlogin\t" + GameManager.getVersionMajor() + "\t" + GameManager.getVersionMinor() + "\t" + GameManager.isDevBuild() + "\n");
		}
		// PeoplecountUpdate
		if(message[0].equals("observerupdate")) {
			//observerupdate\t[PLAYERS]\t[OBSERVERS]
			playerCount = Integer.parseInt(message[1]);
			observerCount = Integer.parseInt(message[2]);
		}

		super.processPacket(fullMessage);
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public int getObserverCount() {
		return observerCount;
	}
}
