// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import java.util.Random;

import nullpomino.game.net.web.WebSignaling;
import nullpomino.game.play.GameManager;

/**
 * Per-page-load singleton for browser netplay: the config, this peer's
 * signaling clientId (the web analog of its IP address), the versioned
 * topic root, and the bonded broker channel shared by the lounge, the
 * room beacon and the WebRTC signaling. The channel connects on first
 * use and stays up for the page's lifetime - a room session must remain
 * reachable for signaling even while the lounge screen is closed.
 */
public final class WebNetHub {
	private static WebNetHub instance;

	public final WebNetConfig config;
	public final Random rand = new Random();
	/** This peer's signaling clientId, stable for one page load */
	public final String clientId;
	/** Versioned topic prefix, e.g. "npp/v7.5" */
	public final String topicRoot;
	public final MqttBondedChannel channel;

	private WebNetHub() {
		config = WebNetConfig.load();
		clientId = WebSignaling.generateId(rand);
		topicRoot = WebSignaling.root(config.topicRoot, String.valueOf(GameManager.getVersionMajor()));
		channel = new MqttBondedChannel(config, rand);
	}

	public static WebNetHub get() {
		if(instance == null) instance = new WebNetHub();
		return instance;
	}

	/** The bonded channel, connecting on first use */
	public MqttBondedChannel connectedChannel() {
		channel.connect();
		return channel;
	}
}
