// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import nullpomino.util.CustomProperties;

/**
 * Settings for browser netplay, loaded from {@code config/etc/netweb.cfg}
 * (re-seeded from the site on every page load, so server-side edits reach
 * players on reload). Only the web build reads this.
 */
public class WebNetConfig {
	/** Config file location */
	public static final String CONFIG_FILE = "config/etc/netweb.cfg";

	/** MQTT-over-WSS broker URLs; the client bonds to all of them */
	public String[] brokers = {
		"wss://broker.emqx.io:8084/mqtt",
		"wss://test.mosquitto.org:8081",
	};

	/** Topic root; the protocol major version is appended (npp/v7/...) */
	public String topicRoot = "npp";

	/** MQTT keepalive in seconds */
	public int keepAliveSec = 30;

	/** Per-broker connect timeout (ms), gated on CONNACK */
	public int connectTimeout = 5000;

	/** STUN (and later TURN) URLs for RTCPeerConnection iceServers */
	public String[] stunUrls = {
		"stun:stun.l.google.com:19302",
		"stun:stun.cloudflare.com:3478",
	};

	/** Cap on ICE gathering before the offer/answer ships anyway (ms) */
	public int iceGatherTimeout = 3000;

	/** @return Settings from the config file, falling back to defaults per key */
	public static WebNetConfig load() {
		CustomProperties prop = CustomProperties.loadFromFileOrEmpty(CONFIG_FILE);
		WebNetConfig config = new WebNetConfig();
		config.brokers = splitList(prop.getProperty("netweb.mqtt.brokers", ""), config.brokers);
		config.topicRoot = prop.getProperty("netweb.mqtt.topicRoot", config.topicRoot);
		config.keepAliveSec = prop.getProperty("netweb.mqtt.keepAlive", config.keepAliveSec);
		config.connectTimeout = prop.getProperty("netweb.mqtt.connectTimeout", config.connectTimeout);
		config.stunUrls = splitList(prop.getProperty("netweb.stun", ""), config.stunUrls);
		config.iceGatherTimeout = prop.getProperty("netweb.iceGatherTimeout", config.iceGatherTimeout);
		return config;
	}

	private static String[] splitList(String value, String[] fallback) {
		String trimmed = (value == null) ? "" : value.trim();
		if(trimmed.length() == 0) return fallback;
		return trimmed.split(" +");
	}
}
